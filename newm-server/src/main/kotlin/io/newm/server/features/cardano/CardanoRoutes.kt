package io.newm.server.features.cardano

import com.firehose.model.CliKeyPair
import com.google.protobuf.kotlin.toByteString
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.newm.chain.util.hexToByteArray
import io.newm.server.auth.jwt.AUTH_JWT
import io.newm.server.auth.jwt.AUTH_JWT_ADMIN
import io.newm.server.features.cardano.model.Key
import io.newm.server.features.cardano.model.SubmitTransactionRequest
import io.newm.server.features.cardano.model.SubmitTransactionResponse
import io.newm.server.features.cardano.repo.CardanoRepository
import io.newm.server.features.song.model.MintingStatus
import io.newm.server.features.song.repo.SongRepository
import io.newm.server.ktx.limit
import io.newm.server.ktx.offset
import io.newm.shared.ktx.get
import io.newm.shared.ktx.post
import org.koin.core.parameter.parametersOf
import org.koin.ktor.ext.inject
import org.slf4j.Logger

fun Routing.createCardanoRoutes() {
    val log: Logger by inject { parametersOf("CardanoRoutes") }
    val songRepository: SongRepository by inject()
    val cardanoRepository: CardanoRepository by inject()

    authenticate(AUTH_JWT_ADMIN) {
        post("/v1/cardano/key") {
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

        get("/v1/cardano/key") {
            try {
                val keyName = parameters["name"] ?: throw IllegalArgumentException("name is required!")
                val key = requireNotNull(cardanoRepository.getKeyByName(keyName)) { "Key with name '$keyName' not found!" }
                respond(key.toCliKeyPair(keyName))
            } catch (e: Exception) {
                log.error("Failed to get key!", e)
                throw e
            }
        }

        post("/v1/cardano/encryption") {
            try {
                cardanoRepository.saveEncryptionParams(receive())
                respond(HttpStatusCode.Created)
            } catch (e: Exception) {
                log.error("Failed to save encryption params!", e)
                throw e
            }
        }
    }

    authenticate(AUTH_JWT) {
        post("/v1/cardano/submitTransaction") {
            try {
                val request = receive<SubmitTransactionRequest>()
                val response = cardanoRepository.submitTransaction(request.cborHex.hexToByteArray().toByteString())
                songRepository.updateSongMintingStatus(request.songId, MintingStatus.MintingPaymentSubmitted)
                respond(HttpStatusCode.Accepted, SubmitTransactionResponse(response.txId, response.result))
            } catch (e: Exception) {
                log.error("Failed to submit transaction: ${e.message}")
                throw e
            }
        }

        post("/v1/cardano/songs") {
            try {
                val request = receive<List<String>>()
                val response = cardanoRepository.getWalletSongs(request, offset, limit)
                respond(response)
            } catch (e: Exception) {
                log.error("Failed to get wallet songs: ${e.message}")
                throw e
            }
        }

        get("/v1/cardano/nfts") {
            try {
                val xpubKey = parameters["xpub"] ?: throw IllegalArgumentException("xpub is required!")
                val response = cardanoRepository.getWalletMusicNFTs(xpubKey)
                respond(response)
            } catch (e: Exception) {
                log.error("Failed to get wallet NFTs: ${e.message}")
                throw e
            }
        }
    }
}
