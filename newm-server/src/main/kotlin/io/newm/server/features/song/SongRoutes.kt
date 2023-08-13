package io.newm.server.features.song

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.route
import io.newm.server.auth.jwt.AUTH_JWT
import io.newm.server.features.model.CountResponse
import io.newm.server.features.song.model.AudioStreamResponse
import io.newm.server.features.song.model.AudioUploadRequest
import io.newm.server.features.song.model.AudioUploadResponse
import io.newm.server.features.song.model.MintPaymentRequest
import io.newm.server.features.song.model.MintPaymentResponse
import io.newm.server.features.song.model.SongIdBody
import io.newm.server.features.song.model.StreamTokenAgreementRequest
import io.newm.server.features.song.model.songFilters
import io.newm.server.features.song.repo.SongRepository
import io.newm.server.ktx.limit
import io.newm.server.ktx.myUserId
import io.newm.server.ktx.offset
import io.newm.server.ktx.songId
import io.newm.shared.koin.inject
import io.newm.shared.ktx.delete
import io.newm.shared.ktx.get
import io.newm.shared.ktx.patch
import io.newm.shared.ktx.post
import io.newm.shared.ktx.put

private const val SONGS_PATH = "v1/songs"

@Suppress("unused")
fun Routing.createSongRoutes() {
    val songRepository: SongRepository by inject()

    authenticate(AUTH_JWT) {
        route(SONGS_PATH) {
            post {
                respond(SongIdBody(songRepository.add(receive(), myUserId)))
            }
            get {
                respond(songRepository.getAll(songFilters, offset, limit))
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
            route("{songId}") {
                get {
                    respond(songRepository.get(songId))
                }
                patch {
                    songRepository.update(songId, receive(), myUserId)
                    respond(HttpStatusCode.NoContent)
                }
                delete {
                    songRepository.delete(songId, myUserId)
                    respond(HttpStatusCode.NoContent)
                }
                // TODO: CU-86a0e050w - remove next POST after frontend migrates to new API
                post("upload") {
                    respond(
                        AudioUploadResponse(
                            songRepository.generateAudioUpload(
                                songId = songId,
                                requesterId = myUserId,
                                fileName = receive<AudioUploadRequest>().fileName
                            )
                        )
                    )
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
                    val streamData = songRepository.generateAudioStreamData(
                        songId = songId,
                    )
                    streamData.cookies.forEach { response.cookies.append(it) }
                    respond(
                        AudioStreamResponse(streamData)
                    )
                }
                route("mint/payment") {
                    get {
                        respond(
                            MintPaymentResponse(
                                cborHex = songRepository.getMintingPaymentAmountCborHex(
                                    songId = songId,
                                    requesterId = myUserId
                                )
                            )
                        )
                    }
                    post {
                        val request = receive<MintPaymentRequest>()
                        respond(
                            MintPaymentResponse(
                                cborHex = songRepository.generateMintingPaymentTransaction(
                                    songId = songId,
                                    requesterId = myUserId,
                                    sourceUtxos = request.utxos,
                                    changeAddress = request.changeAddress
                                )
                            )
                        )
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
            }
        }
    }
}
