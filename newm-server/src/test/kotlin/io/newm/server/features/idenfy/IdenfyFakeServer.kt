package io.newm.server.features.idenfy

import io.ktor.server.application.*
import io.ktor.server.auth.AuthenticationConfig
import io.ktor.server.auth.UserIdPrincipal
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.basic
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.newm.server.features.idenfy.model.IdenfyCreateSessionRequest
import io.newm.server.features.idenfy.model.IdenfyCreateSessionResponse
import io.newm.shared.ktx.getConfigString
import io.newm.shared.ktx.post
import io.newm.shared.koin.inject

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
