package io.projectnewm.server.portal

import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.projectnewm.server.koin.inject
import io.projectnewm.server.portal.song.SongRepository

// https://ktor.io/docs/locations.html
@Suppress("unused")
fun Application.portalModule() {
    val repository: SongRepository by inject()

    routing {
        authenticate("auth-jwt") {
            route("/portal/songs") {
                get {
                    call.respond(repository.getSongs())
                }
            }
        }
    }
}
