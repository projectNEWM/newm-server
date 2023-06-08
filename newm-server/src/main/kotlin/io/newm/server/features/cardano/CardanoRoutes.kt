package io.newm.server.features.cardano

import com.firehose.model.CliKeyPair
import com.google.protobuf.kotlin.toByteString
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.newm.chain.util.hexToByteArray
import io.newm.server.auth.jwt.AUTH_JWT_ADMIN
import io.newm.server.features.cardano.model.Key
import io.newm.server.features.cardano.model.SignedTransaction
import io.newm.server.features.cardano.model.SubmitTransactionResponse
import io.newm.server.features.cardano.repo.CardanoRepository
import io.newm.shared.ktx.post
import org.koin.core.parameter.parametersOf
import org.koin.ktor.ext.inject
import org.slf4j.Logger

fun Routing.createCardanoRoutes() {
    val log: Logger by inject { parametersOf("CardanoRoutes") }
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

        post("/v1/cardano/encryption") {
            try {
                cardanoRepository.saveEncryptionParams(receive())
                respond(HttpStatusCode.Created)
            } catch (e: Exception) {
                log.error("Failed to save encryption params!", e)
                throw e
            }
        }

        post("/v1/cardano/submitTransaction") {
            val signedTransaction = receive<SignedTransaction>().cborHex.hexToByteArray().toByteString()
            try {
                val response = cardanoRepository.submitTransaction(signedTransaction)
                respond(HttpStatusCode.Accepted, SubmitTransactionResponse(response.txId, response.result))
            } catch (e: Exception) {
                log.error("Failed to submit transaction: ${e.message}")
                throw e
            }
        }
    }
}
