package io.newm.server.features.song

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import io.newm.server.auth.jwt.AUTH_JWT
import io.newm.shared.koin.inject
import io.newm.server.features.song.model.SongIdBody
import io.newm.server.features.song.model.StreamTokenAgreementRequest
import io.newm.server.features.song.model.UploadAudioRequest
import io.newm.server.features.song.model.UploadAudioResponse
import io.newm.server.features.song.model.songFilters
import io.newm.server.features.song.repo.SongRepository
import io.newm.server.ext.limit
import io.newm.server.ext.myUserId
import io.newm.server.ext.offset
import io.newm.server.ext.songId

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
                        repository.update(songId, receive(), myUserId)
                        respond(HttpStatusCode.NoContent)
                    }
                }
                delete {
                    with(call) {
                        repository.delete(songId, myUserId)
                        respond(HttpStatusCode.NoContent)
                    }
                }
                post("audio") {
                    with(call) {
                        respond(
                            UploadAudioResponse(
                                repository.generateAudioUploadUrl(
                                    songId = songId,
                                    requesterId = myUserId,
                                    fileName = receive<UploadAudioRequest>().fileName
                                )
                            )
                        )
                    }
                }
                put("agreement") {
                    with(call) {
                        repository.processStreamTokenAgreement(
                            songId = songId,
                            requesterId = myUserId,
                            accepted = receive<StreamTokenAgreementRequest>().accepted
                        )
                        respond(HttpStatusCode.NoContent)
                    }
                }
            }
        }
    }
}
