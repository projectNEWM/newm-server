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
import io.newm.shared.ktx.error
import io.newm.shared.ktx.get
import io.newm.shared.ktx.post
import org.koin.core.parameter.parametersOf
import org.koin.ktor.ext.inject
import org.slf4j.Logger
import java.security.Key

private const val IDENFY_PATH = "/v1/idenfy"

fun Routing.createIdenfyRoutes() {
    val key: Key by inject(IDENFY_KEY_QUALIFIER)
    val repository: IdenfyRepository by inject()
    val logger: Logger by inject { parametersOf(javaClass.simpleName) }

    route(IDENFY_PATH) {
        authenticate(AUTH_JWT) {
            get("session") {
                try {
                    respond(repository.createSession(myUserId))
                } catch (throwable: Throwable) {
                    logger.error(throwable) { "Idenfy session creation failure" }
                    throw throwable
                }
            }
        }

        post("callback") {
            try {
                repository.processSessionResult(
                    receiveAndVerify(
                        signatureHeader = "Idenfy-Signature",
                        key = key
                    )
                )
            } catch (throwable: Throwable) {
                logger.error(throwable) { "Idenfy callback failure" }
                throw throwable
            }
            respond(HttpStatusCode.OK)
        }
    }
}
