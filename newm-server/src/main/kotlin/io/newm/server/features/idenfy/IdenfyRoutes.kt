package io.newm.server.features.idenfy

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.newm.server.auth.jwt.AUTH_JWT
import io.newm.server.ext.myUserId
import io.newm.server.ext.receiveAndVerify
import io.newm.server.features.idenfy.repo.IdenfyRepository
import org.koin.ktor.ext.inject
import java.security.Key

private const val IDENFY_PATH = "/v1/idenfy"

fun Routing.createIdenfyRoutes() {
    val key: Key by inject(IDENFY_KEY_QUALIFIER)
    val repository: IdenfyRepository by inject()

    route(IDENFY_PATH) {
        authenticate(AUTH_JWT) {
            get("session") {
                with(call) {
                    respond(repository.createSession(myUserId))
                }
            }
        }

        post("callback") {
            with(call) {
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
}
