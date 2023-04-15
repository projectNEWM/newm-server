package io.newm.server.features.song

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.route
import io.newm.server.auth.jwt.AUTH_JWT
import io.newm.server.ktx.limit
import io.newm.server.ktx.myUserId
import io.newm.server.ktx.offset
import io.newm.server.ktx.songId
import io.newm.server.features.model.CountResponse
import io.newm.server.features.song.model.MintPaymentRequest
import io.newm.server.features.song.model.MintPaymentResponse
import io.newm.server.features.song.model.SongIdBody
import io.newm.server.features.song.model.StreamTokenAgreementRequest
import io.newm.server.features.song.model.UploadAudioPostResponse
import io.newm.server.features.song.model.UploadAudioRequest
import io.newm.server.features.song.model.UploadAudioResponse
import io.newm.server.features.song.model.songFilters
import io.newm.server.features.song.repo.SongRepository
import io.newm.shared.ktx.delete
import io.newm.shared.ktx.get
import io.newm.shared.ktx.patch
import io.newm.shared.ktx.post
import io.newm.shared.ktx.put
import io.newm.shared.koin.inject

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
                post("audio") {
                    respond(
                        UploadAudioResponse(
                            songRepository.generateAudioUploadUrl(
                                songId = songId,
                                requesterId = myUserId,
                                fileName = receive<UploadAudioRequest>().fileName
                            )
                        )
                    )
                }
                post("upload") {
                    respond(
                        UploadAudioPostResponse(
                            songRepository.generateAudioUploadPost(
                                songId = songId,
                                requesterId = myUserId,
                                fileName = receive<UploadAudioRequest>().fileName
                            )
                        )
                    )
                }
                route("mint/payment") {
                    get {
                        respond(
                            MintPaymentResponse(
                                cborHex = songRepository.getMintingPaymentAmount(
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
