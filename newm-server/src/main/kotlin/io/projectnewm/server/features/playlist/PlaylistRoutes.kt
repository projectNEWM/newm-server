package io.projectnewm.server.features.playlist

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.projectnewm.server.auth.jwt.AUTH_JWT
import io.projectnewm.server.di.inject
import io.projectnewm.server.ext.myUserId
import io.projectnewm.server.ext.playlistId
import io.projectnewm.server.ext.songId
import io.projectnewm.server.ext.toUUID
import io.projectnewm.server.features.playlist.model.SongIdRequest
import io.projectnewm.server.features.playlist.repo.PlaylistRepository

private const val PLAYLISTS_PATH = "v1/playlists"

@Suppress("unused")
fun Routing.createPlaylistRoutes() {
    val repository: PlaylistRepository by inject()

    authenticate(AUTH_JWT) {
        route(PLAYLISTS_PATH) {
            put {
                with(call) {
                    repository.add(receive(), myUserId)
                    respond(HttpStatusCode.NoContent)
                }
            }
            get {
                with(call) {
                    val ownerId = request.queryParameters["ownerId"]?.toUUID() ?: myUserId
                    respond(repository.getAllByOwnerId(ownerId))
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
                            repository.addSong(playlistId, receive<SongIdRequest>().songId, myUserId)
                            respond(HttpStatusCode.NoContent)
                        }
                    }
                    get {
                        call.respond(repository.getSongs(call.playlistId))
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
