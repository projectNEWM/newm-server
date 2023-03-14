package io.newm.server.features.idenfy

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.newm.shared.koin.inject
import io.newm.shared.ext.getConfigString
import io.newm.server.features.idenfy.model.IdenfyCreateSessionRequest
import io.newm.server.features.idenfy.model.IdenfyCreateSessionResponse

private const val AUTH_IDENFY = "auth-idenfy"

fun AuthenticationConfig.configIdenfyFakeServerAuth() {
    val environment: ApplicationEnvironment by inject()
    val key = environment.getConfigString("idenfy.apiKey")
    val secret = environment.getConfigString("idenfy.apiSecret")
    basic(AUTH_IDENFY) {
        validate { credentials ->
            if (credentials.name == key && credentials.password == secret) {
                UserIdPrincipal(credentials.name)
            } else {
                null
            }
        }
    }
}

fun Routing.createIdenfyFakeServerRoutes() {
    authenticate(AUTH_IDENFY) {
        post("/idenfy-fake-server/api/v2/token") {
            with(call) {
                // for testing we echo back the request client ID
                val req = receive<IdenfyCreateSessionRequest>()
                respond(
                    IdenfyCreateSessionResponse(
                        authToken = req.clientId,
                        expiryTime = req.clientId.hashCode()
                    )
                )
            }
        }
    }
}
