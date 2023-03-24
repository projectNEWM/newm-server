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
import io.newm.server.ext.limit
import io.newm.server.ext.myUserId
import io.newm.server.ext.offset
import io.newm.server.ext.playlistId
import io.newm.server.ext.songId
import io.newm.server.features.model.CountResponse
import io.newm.server.features.playlist.model.PlaylistIdBody
import io.newm.server.features.playlist.model.playlistFilters
import io.newm.server.features.playlist.repo.PlaylistRepository
import io.newm.server.features.song.model.SongIdBody
import io.newm.shared.koin.inject

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
                    respond(
                        repository.getAll(playlistFilters, offset, limit)
                    )
                }
            }
            get("count") {
                with(call) {
                    respond(CountResponse(repository.getAllCount(playlistFilters)))
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
