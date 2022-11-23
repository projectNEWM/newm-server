package io.newm.server.features.song

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
import io.newm.server.auth.jwt.AUTH_JWT
import io.newm.server.di.inject
import io.newm.server.ext.*
import io.newm.server.features.song.model.SongIdBody
import io.newm.server.features.song.model.UploadRequest
import io.newm.server.features.song.model.UploadResponse
import io.newm.server.features.song.model.songFilters
import io.newm.server.features.song.repo.SongRepository

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
                    respond(repository.getAll(songFilters, offset, limit))
                }
            }
            get("genres") {
                with(call) {
                    respond(repository.getGenres(songFilters, offset, limit))
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
                post("upload") {
                    with(call) {
                        val req = receive<UploadRequest>()
                        respond(
                            UploadResponse(repository.generateUploadUrl(songId, myUserId, req.fileName))
                        )
                    }
                }
            }
        }
    }
}
