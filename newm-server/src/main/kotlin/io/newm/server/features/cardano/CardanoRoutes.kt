package io.newm.server.features.cardano

import com.firehose.model.CliKeyPair
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.newm.server.auth.jwt.AUTH_JWT_ADMIN
import io.newm.server.features.cardano.model.Key
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
    }
}
