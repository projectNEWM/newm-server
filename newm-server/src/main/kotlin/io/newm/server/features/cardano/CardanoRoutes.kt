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
import org.koin.ktor.ext.inject

fun Routing.createCardanoRoutes() {
    val cardanoRepository: CardanoRepository by inject()

    authenticate(AUTH_JWT_ADMIN) {
        post("/v1/cardano/key") {
            val cliKeyPair: CliKeyPair = receive()
            val key = Key.createFromCliKeys(cliKeyPair)
            if (cardanoRepository.getKeyByName(cliKeyPair.name) != null) {
                throw IllegalArgumentException("Key with name '${cliKeyPair.name}' already exists!")
            }
            cardanoRepository.saveKey(key, cliKeyPair.name)
            respond(HttpStatusCode.Created)
        }

        post("/v1/cardano/encryption") {
            cardanoRepository.saveEncryptionParams(receive())
            respond(HttpStatusCode.Created)
        }

        post("/v1/cardano/submitTransaction") {
            val signedTransaction = receive<SignedTransaction>().cborHex.hexToByteArray().toByteString()
            try {
                val response = cardanoRepository.submitTransaction(signedTransaction)
                respond(HttpStatusCode.Accepted, SubmitTransactionResponse(response.txId, response.result))
            } catch (e: Exception) {
                throw IllegalArgumentException("Failed to submit transaction: ${e.message}")
            }
        }
    }
}
