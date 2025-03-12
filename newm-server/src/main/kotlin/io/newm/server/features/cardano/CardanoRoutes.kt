package io.newm.server.features.cardano

import com.firehose.model.CliKeyPair
import com.google.protobuf.kotlin.toByteString
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.route
import io.newm.chain.util.extractStakeAddress
import io.newm.chain.util.hexToByteArray
import io.newm.server.auth.jwt.AUTH_JWT
import io.newm.server.auth.jwt.AUTH_JWT_ADMIN
import io.newm.server.features.cardano.model.Key
import io.newm.server.features.cardano.model.QueryPriceResponse
import io.newm.server.features.cardano.model.ScriptAddressWhitelistRequest
import io.newm.server.features.cardano.model.SubmitTransactionRequest
import io.newm.server.features.cardano.model.SubmitTransactionResponse
import io.newm.server.features.cardano.model.SubmitTxRequest
import io.newm.server.features.cardano.repo.CardanoRepository
import io.newm.server.features.song.model.MintingStatus
import io.newm.server.features.song.model.songFilters
import io.newm.server.features.song.repo.SongRepository
import io.newm.server.ktx.limit
import io.newm.server.ktx.myUserId
import io.newm.server.ktx.offset
import io.newm.server.recaptcha.repo.RecaptchaRepository
import io.newm.shared.ktx.error
import io.newm.shared.ktx.get
import io.newm.shared.ktx.post
import io.newm.shared.ktx.toUUID
import org.koin.core.parameter.parametersOf
import org.koin.ktor.ext.inject
import org.slf4j.Logger

fun Routing.createCardanoRoutes() {
    val log: Logger by inject { parametersOf("CardanoRoutes") }
    val songRepository: SongRepository by inject()
    val cardanoRepository: CardanoRepository by inject()
    val recaptchaRepository: RecaptchaRepository by inject()

    route("/v1/cardano") {
        authenticate(AUTH_JWT_ADMIN) {
            post("key") {
                try {
                    val cliKeyPair: CliKeyPair = receive()
                    val key = Key.createFromCliKeys(cliKeyPair)
                    if (cardanoRepository.getKeyByName(cliKeyPair.name) != null) {
                        throw IllegalArgumentException("Key with name '${cliKeyPair.name}' already exists!")
                    }
                    cardanoRepository.saveKey(key, cliKeyPair.name)
                    respond(HttpStatusCode.Created)
                } catch (e: Exception) {
                    log.error("Failed to save key!", e)
                    throw e
                }
            }

//        /**
//         * Only temporarily uncomment if there's some legal emergency where we need a key to perform a manual transaction.
//         */
//        get("/v1/cardano/key") {
//            try {
//                val keyName = request.requiredQueryParam("name")
//                val key = requireNotNull(cardanoRepository.getKeyByName(keyName)) { "Key with name '$keyName' not found!" }
//                respond(key.toCliKeyPair(keyName))
//            } catch (e: Exception) {
//                log.error("Failed to get key!", e)
//                throw e
//            }
//        }

            get("key/{keyId}") {
                try {
                    val keyId =
                        request.call.parameters["keyId"]?.toUUID() ?: throw IllegalArgumentException("Invalid key id!")
                    val key = requireNotNull(cardanoRepository.getKey(keyId)) { "Key with id '$keyId' not found!" }
                    respond(key.toCliKeyPair(keyId.toString()))
                } catch (e: Exception) {
                    log.error("Failed to get key!", e)
                    throw e
                }
            }

            post("encryption") {
                try {
                    cardanoRepository.saveEncryptionParams(receive())
                    respond(HttpStatusCode.Created)
                } catch (e: Exception) {
                    log.error("Failed to save encryption params!", e)
                    throw e
                }
            }

            post("scriptAddressWhitelist") {
                try {
                    val scriptAddressWhitelistRequest = receive<ScriptAddressWhitelistRequest>()
                    val scriptAddress = scriptAddressWhitelistRequest.scriptAddress
                    val stakeAddress =
                        if (scriptAddress.startsWith("stake") ||
                            scriptAddress.startsWith("stake_test")
                        ) {
                            scriptAddress
                        } else {
                            scriptAddress.extractStakeAddress(cardanoRepository.isMainnet())
                        }

                    if (scriptAddress != stakeAddress) {
                        cardanoRepository.saveScriptAddressToWhitelist(scriptAddress)
                    }
                    cardanoRepository.saveScriptAddressToWhitelist(stakeAddress)
                    respond(HttpStatusCode.Created)
                } catch (e: Exception) {
                    log.error("Failed to add script address to whitelist!", e)
                    throw e
                }
            }
        }

        authenticate(AUTH_JWT) {
            post("submitTransaction") {
                try {
                    val request = receive<SubmitTransactionRequest>()
                    val response = cardanoRepository.submitTransaction(request.cborHex.hexToByteArray().toByteString())
                    if (!response.txId.isNullOrBlank()) {
                        songRepository.updateSongMintingStatus(request.songId, MintingStatus.MintingPaymentSubmitted)
                        respond(HttpStatusCode.Accepted, SubmitTransactionResponse(response.txId, response.result))
                    } else {
                        songRepository.updateSongMintingStatus(
                            request.songId,
                            MintingStatus.MintingPaymentException,
                            "error submitting minting payment: ${response.result}"
                        )
                        respond(HttpStatusCode.BadRequest, SubmitTransactionResponse(response.txId, response.result))
                    }
                } catch (e: Exception) {
                    log.error("Failed to submit transaction: ${e.message}")
                    throw e
                }
            }

            post("songs") {
                try {
                    val request = receive<List<String>>()
                    val response = cardanoRepository.getWalletSongs(request, songFilters, offset, limit)
                    respond(response)
                } catch (e: Exception) {
                    log.error("Failed to get wallet songs: ${e.message}")
                    throw e
                }
            }

            get("nft/songs") {
                try {
                    respond(
                        cardanoRepository.getWalletNFTSongs(
                            userId = myUserId,
                            includeLegacy = parameters["legacy"]?.toBoolean() == true,
                            useDripDropz = parameters["dripDropz"]?.toBoolean() == true
                        )
                    )
                } catch (e: Exception) {
                    log.error(e) { "Failed to get NFT Songs" }
                    throw e
                }
            }

            get("images") {
                try {
                    respond(cardanoRepository.getWalletImages(myUserId))
                } catch (e: Exception) {
                    log.error(e) { "Failed to get images" }
                    throw e
                }
            }
        }

        get("prices/ada") {
            recaptchaRepository.verify("get_ada_price", request)
            respond(QueryPriceResponse(cardanoRepository.queryAdaUSDPrice()))
        }
        get("prices/newm") {
            recaptchaRepository.verify("get_newm_price", request)
            respond(QueryPriceResponse(cardanoRepository.queryNEWMUSDPrice()))
        }

        post("submitTx") {
            recaptchaRepository.verify("submit_tx", request)
            try {
                val request = receive<SubmitTxRequest>()
                val response = cardanoRepository.submitTransaction(request.cborHex.hexToByteArray().toByteString())
                if (!response.txId.isNullOrBlank()) {
                    respond(HttpStatusCode.Accepted, SubmitTransactionResponse(response.txId, response.result))
                } else {
                    respond(HttpStatusCode.BadRequest, SubmitTransactionResponse(response.txId, response.result))
                }
            } catch (e: Exception) {
                log.error { "Failed to submit transaction: ${e.message}" }
                throw e
            }
        }
    }
}
