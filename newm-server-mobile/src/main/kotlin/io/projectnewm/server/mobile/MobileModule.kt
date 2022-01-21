package io.projectnewm.server.mobile

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

@Suppress("unused")
fun Application.mobileModule() {
    routing {
        get("/mobile") {
            call.respond(mapOf("Module" to "Mobile"))
        }
    }
}
