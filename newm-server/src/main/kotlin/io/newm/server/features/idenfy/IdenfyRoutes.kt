package io.newm.server.features.idenfy

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.route
import io.newm.server.auth.jwt.AUTH_JWT
import io.newm.server.ktx.myUserId
import io.newm.server.ktx.receiveAndVerify
import io.newm.server.features.idenfy.repo.IdenfyRepository
import io.newm.shared.ktx.get
import io.newm.shared.ktx.post
import org.koin.ktor.ext.inject
import java.security.Key

private const val IDENFY_PATH = "/v1/idenfy"

fun Routing.createIdenfyRoutes() {
    val key: Key by inject(IDENFY_KEY_QUALIFIER)
    val repository: IdenfyRepository by inject()

    route(IDENFY_PATH) {
        authenticate(AUTH_JWT) {
            get("session") {
                respond(repository.createSession(myUserId))
            }
        }

        post("callback") {
            repository.processSessionResult(
                receiveAndVerify(
                    signatureHeader = "Idenfy-Signature",
                    key = key
                )
            )
            respond(HttpStatusCode.OK)
        }
    }
}
