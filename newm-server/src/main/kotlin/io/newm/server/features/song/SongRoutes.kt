package io.newm.server.features.song

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.newm.server.auth.jwt.AUTH_JWT
import io.newm.server.ext.limit
import io.newm.server.ext.myUserId
import io.newm.server.ext.offset
import io.newm.server.ext.songId
import io.newm.server.features.model.CountResponse
import io.newm.server.features.song.model.*
import io.newm.server.features.song.repo.SongRepository
import io.newm.shared.koin.inject

private const val SONGS_PATH = "v1/songs"

@Suppress("unused")
fun Routing.createSongRoutes() {
    val songRepository: SongRepository by inject()

    authenticate(AUTH_JWT) {
        route(SONGS_PATH) {
            post {
                with(call) {
                    respond(SongIdBody(songRepository.add(receive(), myUserId)))
                }
            }
            get {
                with(call) {
                    respond(songRepository.getAll(songFilters, offset, limit))
                }
            }
            get("count") {
                with(call) {
                    respond(CountResponse(songRepository.getAllCount(songFilters)))
                }
            }
            route("genres") {
                get {
                    with(call) {
                        respond(songRepository.getGenres(songFilters, offset, limit))
                    }
                }
                get("count") {
                    with(call) {
                        respond(CountResponse(songRepository.getGenreCount(songFilters)))
                    }
                }
            }
            route("{songId}") {
                get {
                    with(call) {
                        respond(songRepository.get(songId))
                    }
                }
                patch {
                    with(call) {
                        songRepository.update(songId, receive(), myUserId)
                        respond(HttpStatusCode.NoContent)
                    }
                }
                delete {
                    with(call) {
                        songRepository.delete(songId, myUserId)
                        respond(HttpStatusCode.NoContent)
                    }
                }
                post("upload") {
                    with(call) {
                        val resp = UploadAudioPostResponse(
                            songRepository.generateAudioUploadPost(
                                songId = songId,
                                requesterId = myUserId,
                                fileName = receive<UploadAudioRequest>().fileName
                            )
                        )
                        respond(resp)
                    }
                }
                post("audio") {
                    with(call) {
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
                }
                route("mint/payment") {
                    get {
                        with(call) {
                            respond(
                                MintPaymentResponse(
                                    cborHex = songRepository.getMintingPaymentAmount(
                                        songId = songId,
                                        requesterId = myUserId
                                    )
                                )
                            )
                        }
                    }
                    post {
                        with(call) {
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
                }
                put("agreement") {
                    with(call) {
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
}
