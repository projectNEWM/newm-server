package io.newm.server.features.song

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.route
import io.newm.server.auth.jwt.AUTH_JWT
import io.newm.server.auth.jwt.AUTH_JWT_ADMIN
import io.newm.server.features.earnings.repo.EarningsRepository
import io.newm.server.features.model.CountResponse
import io.newm.server.features.song.model.AudioStreamResponse
import io.newm.server.features.song.model.MintPaymentRequest
import io.newm.server.features.song.model.MintPaymentResponse
import io.newm.server.features.song.model.Song
import io.newm.server.features.song.model.SongIdBody
import io.newm.server.features.song.model.StreamTokenAgreementRequest
import io.newm.server.features.song.model.songFilters
import io.newm.server.features.song.repo.SongRepository
import io.newm.server.features.user.model.User
import io.newm.server.features.user.repo.UserRepository
import io.newm.server.ktx.collaborators
import io.newm.server.ktx.limit
import io.newm.server.ktx.mintingStatus
import io.newm.server.ktx.myUserId
import io.newm.server.ktx.offset
import io.newm.server.ktx.requestPaymentType
import io.newm.server.ktx.songId
import io.newm.shared.koin.inject
import io.newm.shared.ktx.delete
import io.newm.shared.ktx.get
import io.newm.shared.ktx.patch
import io.newm.shared.ktx.post
import io.newm.shared.ktx.put
import io.newm.shared.ktx.toLocalDateTime
import org.jetbrains.exposed.sql.SortOrder

private const val SONGS_PATH = "v1/songs"

@Suppress("unused")
fun Routing.createSongRoutes() {
    val songRepository: SongRepository by inject()
    val userRepository: UserRepository by inject()
    val earningsRepository: EarningsRepository by inject()

    authenticate(AUTH_JWT_ADMIN) {
        route(SONGS_PATH) {
            route("{songId}") {
                post("refund") {
                    val song = songRepository.get(songId)
                    val walletAddress = userRepository.get(song.ownerId!!).walletAddress!!
                    respond(songRepository.refundMintingPayment(songId, walletAddress))
                }
                post("reprocess/{mintingStatus}") {
                    songRepository.updateSongMintingStatus(songId, mintingStatus)
                    respond(HttpStatusCode.Accepted)
                }
            }
        }
    }
    authenticate(AUTH_JWT) {
        route(SONGS_PATH) {
            post {
                respond(SongIdBody(songRepository.add(receive(), myUserId)))
            }
            get {
                val songs = songRepository.getAll(songFilters, 0, Int.MAX_VALUE)
                val startDate = parameters["startDate"]?.toLocalDateTime()
                val endDate = parameters["endDate"]?.toLocalDateTime()
                val songsAndEarnings = songs.map { song ->
                    val allEarningsAmount = earningsRepository
                        .getAllBySongId(song.id!!)
                        .filter { earning ->
                            (startDate == null || earning.createdAt >= startDate) &&
                                (endDate == null || earning.createdAt <= endDate)
                        }.sumOf { it.amount }
                    song.copy(earnings = allEarningsAmount)
                }
                val startIndex = offset.coerceAtLeast(0)
                val endIndex = (offset + limit).coerceAtMost(songsAndEarnings.size)
                if (songFilters.sortedBy == "earnings") {
                    val sortOrder = songFilters.sortOrder ?: SortOrder.DESC
                    respond(
                        if (sortOrder == SortOrder.ASC) {
                            songsAndEarnings.sortedBy { it.earnings }
                        } else {
                            songsAndEarnings.sortedByDescending { it.earnings }
                        }.slice(startIndex until endIndex)
                    )
                } else {
                    respond(songsAndEarnings.slice(startIndex until endIndex))
                }
            }
            get("count") {
                respond(CountResponse(songRepository.getAllCount(songFilters)))
            }
            route("genres") {
                get {
                    respond(songRepository.getGenres(songFilters, offset, limit))
                }
                get("count") {
                    respond(CountResponse(songRepository.getGenreCount(songFilters)))
                }
            }
            route("mint/estimate") {
                get {
                    respond(
                        songRepository.getMintingPaymentEstimate(
                            collaborators = collaborators
                        )
                    )
                }
            }
            route("{songId}") {
                get {
                    respond(songRepository.get(songId))
                }
                patch {
                    val song: Song = receive()
                    songRepository.update(songId, song, myUserId)
                    respond(HttpStatusCode.NoContent)
                }
                delete {
                    songRepository.delete(songId, myUserId)
                    respond(HttpStatusCode.NoContent)
                }
                post("audio") {
                    respond(
                        songRepository.uploadAudio(
                            songId = songId,
                            requesterId = myUserId,
                            data = request.receiveChannel()
                        )
                    )
                }
                get("stream") {
                    val streamData =
                        songRepository.generateAudioStreamData(
                            songId = songId,
                        )
                    streamData.cookies.forEach { response.cookies.append(it) }
                    respond(
                        AudioStreamResponse(streamData)
                    )
                }
                post("redistribute") {
                    songRepository.redistribute(songId)
                    respond(HttpStatusCode.NoContent)
                }
                route("mint/payment") {
                    get {
                        respond(
                            songRepository.getMintingPaymentAmount(
                                songId = songId,
                                requesterId = myUserId,
                                paymentType = requestPaymentType,
                            )
                        )
                    }
                    post {
                        val request = receive<MintPaymentRequest>()
                        val utxos = request.utxos
                        if (utxos.isEmpty()) {
                            respond(HttpStatusCode.PaymentRequired, "No UTXOs provided!")
                        } else {
                            val user = userRepository.get(myUserId)
                            if (user.walletAddress.isNullOrBlank()) {
                                // We need to update the user's wallet address since it wasn't set properly at this point.
                                userRepository.updateUserData(myUserId, User(walletAddress = request.changeAddress))
                            }
                            respond(
                                MintPaymentResponse(
                                    cborHex =
                                        songRepository.generateMintingPaymentTransaction(
                                            songId = songId,
                                            requesterId = myUserId,
                                            sourceUtxos = utxos,
                                            changeAddress = request.changeAddress
                                        )
                                )
                            )
                        }
                    }
                }
                put("agreement") {
                    songRepository.processStreamTokenAgreement(
                        songId = songId,
                        requesterId = myUserId,
                        accepted = receive<StreamTokenAgreementRequest>().accepted
                    )
                    respond(HttpStatusCode.NoContent)
                }
                get("smartlinks") {
                    respond(songRepository.getSmartLinks(songId))
                }
            }
        }
    }
}
