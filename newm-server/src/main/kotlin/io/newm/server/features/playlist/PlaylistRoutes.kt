package io.newm.server.features.playlist

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.route
import io.newm.server.auth.jwt.AUTH_JWT
import io.newm.server.ktx.limit
import io.newm.server.ktx.myUserId
import io.newm.server.ktx.offset
import io.newm.server.ktx.playlistId
import io.newm.server.ktx.songId
import io.newm.server.features.model.CountResponse
import io.newm.server.features.playlist.model.PlaylistIdBody
import io.newm.server.features.playlist.model.playlistFilters
import io.newm.server.features.playlist.repo.PlaylistRepository
import io.newm.server.features.song.model.SongIdBody
import io.newm.shared.ktx.delete
import io.newm.shared.ktx.get
import io.newm.shared.ktx.patch
import io.newm.shared.ktx.post
import io.newm.shared.ktx.put
import io.newm.shared.koin.inject

private const val PLAYLISTS_PATH = "v1/playlists"

@Suppress("unused")
fun Routing.createPlaylistRoutes() {
    val repository: PlaylistRepository by inject()

    authenticate(AUTH_JWT) {
        route(PLAYLISTS_PATH) {
            post {
                respond(PlaylistIdBody(repository.add(receive(), myUserId)))
            }
            get {
                respond(
                    repository.getAll(playlistFilters, offset, limit)
                )
            }
            get("count") {
                respond(CountResponse(repository.getAllCount(playlistFilters)))
            }
            route("{playlistId}") {
                get {
                    respond(repository.get(playlistId))
                }
                patch {
                    repository.update(receive(), playlistId, myUserId)
                    respond(HttpStatusCode.NoContent)
                }
                delete {
                    repository.delete(playlistId, myUserId)
                    respond(HttpStatusCode.NoContent)
                }
                route("songs") {
                    put {
                        repository.addSong(playlistId, receive<SongIdBody>().songId, myUserId)
                        respond(HttpStatusCode.NoContent)
                    }
                    get {
                        respond(
                            repository.getSongs(playlistId, offset, limit)
                        )
                    }
                    delete("{songId}") {
                        repository.deleteSong(playlistId, songId, myUserId)
                        respond(HttpStatusCode.NoContent)
                    }
                }
            }
        }
    }
}
