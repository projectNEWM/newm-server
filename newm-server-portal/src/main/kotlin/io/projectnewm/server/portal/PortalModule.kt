package io.projectnewm.server.portal

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.projectnewm.server.portal.repo.PortalRepository

@Suppress("unused")
fun Application.portalModule() {
    val repository = PortalRepository()
    routing {
        route("/portal/songs") {
            get {
                call.respond(repository.getSongs())
            }
        }
    }
}
