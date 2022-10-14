package io.newm.server.features.playlist

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
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.newm.server.auth.jwt.AUTH_JWT
import io.newm.server.di.inject
import io.newm.server.ext.*
import io.newm.server.features.playlist.model.PlaylistFilter
import io.newm.server.features.playlist.model.PlaylistIdBody
import io.newm.server.features.song.model.SongIdBody
import io.newm.server.features.playlist.repo.PlaylistRepository

private const val PLAYLISTS_PATH = "v1/playlists"

@Suppress("unused")
fun Routing.createPlaylistRoutes() {
    val repository: PlaylistRepository by inject()

    authenticate(AUTH_JWT) {
        route(PLAYLISTS_PATH) {
            post {
                with(call) {
                    respond(PlaylistIdBody(repository.add(receive(), myUserId)))
                }
            }
            get {
                with(call) {
                    val filter = ownerId?.let { PlaylistFilter.OwnerId(it) } ?: PlaylistFilter.All
                    respond(
                        repository.getAll(filter, offset, limit)
                    )
                }
            }
            route("{playlistId}") {
                get {
                    call.respond(repository.get(call.playlistId))
                }
                patch {
                    with(call) {
                        repository.update(receive(), playlistId, myUserId)
                        respond(HttpStatusCode.NoContent)
                    }
                }
                delete {
                    with(call) {
                        repository.delete(playlistId, myUserId)
                        respond(HttpStatusCode.NoContent)
                    }
                }
                route("songs") {
                    put {
                        with(call) {
                            repository.addSong(playlistId, receive<SongIdBody>().songId, myUserId)
                            respond(HttpStatusCode.NoContent)
                        }
                    }
                    get {
                        with(call) {
                            respond(
                                repository.getSongs(playlistId, offset, limit)
                            )
                        }
                    }
                    delete("{songId}") {
                        with(call) {
                            repository.deleteSong(playlistId, songId, myUserId)
                            respond(HttpStatusCode.NoContent)
                        }
                    }
                }
            }
        }
    }
}
