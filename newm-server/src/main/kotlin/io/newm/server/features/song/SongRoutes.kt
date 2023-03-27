package io.newm.server.features.song

import com.google.iot.cbor.CborInteger
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
import io.newm.chain.grpc.outputUtxo
import io.newm.chain.util.toHexString
import io.newm.server.auth.jwt.AUTH_JWT
import io.newm.server.ext.limit
import io.newm.server.ext.myUserId
import io.newm.server.ext.offset
import io.newm.server.ext.songId
import io.newm.server.features.cardano.model.Key
import io.newm.server.features.cardano.repo.CardanoRepository
import io.newm.server.features.model.CountResponse
import io.newm.server.features.song.model.MintPaymentRequest
import io.newm.server.features.song.model.MintPaymentResponse
import io.newm.server.features.song.model.MintingStatus
import io.newm.server.features.song.model.SongIdBody
import io.newm.server.features.song.model.StreamTokenAgreementRequest
import io.newm.server.features.song.model.UploadAudioRequest
import io.newm.server.features.song.model.UploadAudioResponse
import io.newm.server.features.song.model.songFilters
import io.newm.server.features.song.repo.SongRepository
import io.newm.shared.koin.inject

private const val SONGS_PATH = "v1/songs"

@Suppress("unused")
fun Routing.createSongRoutes() {
    val songRepository: SongRepository by inject()
    val cardanoRepository: CardanoRepository by inject()

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
                get("mintPayment") {
                    with(call) {
                        // Return the cbor value to pass to api.getUtxos(value) for proper utxo selection.
                        // This is essentially the server telling the web how much to charge the user.
                        respond(
                            MintPaymentResponse(
                                // FIXME: Don't hardcode the price to mint
                                cborHex = CborInteger.create(6000000L).toCborByteArray().toHexString()
                            )
                        )
                    }
                }
                post("mintPayment") {
                    with(call) {
                        val song = songRepository.get(songId)
                        val mintPaymentRequest = receive<MintPaymentRequest>()
                        val paymentKey = Key.generateNew()
                        val paymentKeyId = cardanoRepository.add(paymentKey)
                        val sourceUtxos = mintPaymentRequest.utxos
                        val response = cardanoRepository.buildTransaction {
                            this.sourceUtxos.addAll(sourceUtxos)
                            this.outputUtxos.add(
                                outputUtxo {
                                    address = paymentKey.address
                                    lovelace = "6000000" // FIXME: Don't hardcode the price to mint
                                }
                            )
                            this.changeAddress = mintPaymentRequest.changeAddress
                        }
                        songRepository.update(
                            songId,
                            song.copy(mintingStatus = MintingStatus.MintingPaymentRequested, paymentKeyId = paymentKeyId),
                            myUserId
                        )
                        // Tell SQS to monitor for the payment

                        respond(MintPaymentResponse(cborHex = response.transactionCbor.toByteArray().toHexString()))
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
