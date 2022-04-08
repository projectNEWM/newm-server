package io.projectnewm.server.features.song

import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.projectnewm.server.auth.jwt.AUTH_JWT
import io.projectnewm.server.di.inject
import io.projectnewm.server.features.song.repo.SongRepository

@Suppress("unused")
fun Routing.createSongRoutes() {
    val repository: SongRepository by inject()

    authenticate(AUTH_JWT) {
        route("v1/songs") {
            get {
                call.respond(repository.getSongs())
            }
        }
    }
}
