package io.projectnewm.server.portal

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

@Suppress("unused")
fun Application.portalModule() {
    routing {
        get("/portal") {
            call.respond(mapOf("Module" to "Portral"))
        }
    }
}
