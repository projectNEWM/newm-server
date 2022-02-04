package io.projectnewm.server.mobile

import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

@Suppress("unused")
fun Application.mobileModule() {
    routing {
        get("/mobile") {
            call.respond(mapOf("Module" to "Mobile"))
        }
    }
}
