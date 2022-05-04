package io.projectnewm.server.features.song

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.projectnewm.server.auth.jwt.AUTH_JWT
import io.projectnewm.server.di.inject
import io.projectnewm.server.ext.myUserId
import io.projectnewm.server.ext.songId
import io.projectnewm.server.ext.toUUID
import io.projectnewm.server.features.song.model.SongIdBody
import io.projectnewm.server.features.song.repo.SongRepository

private const val SONGS_PATH = "v1/songs"

@Suppress("unused")
fun Routing.createSongRoutes() {
    val repository: SongRepository by inject()

    authenticate(AUTH_JWT) {
        route(SONGS_PATH) {
            post {
                with(call) {
                    respond(SongIdBody(repository.add(receive(), myUserId)))
                }
            }
            get {
                with(call) {
                    val ownerId = request.queryParameters["ownerId"]?.toUUID() ?: myUserId
                    respond(repository.getAllByOwnerId(ownerId))
                }
            }
            route("{songId}") {
                get {
                    with(call) {
                        respond(repository.get(songId))
                    }
                }
                patch {
                    with(call) {
                        repository.update(receive(), songId, myUserId)
                        respond(HttpStatusCode.NoContent)
                    }
                }
                delete {
                    with(call) {
                        repository.delete(songId, myUserId)
                        respond(HttpStatusCode.NoContent)
                    }
                }
            }
        }
    }
}
