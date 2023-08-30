package io.newm.server.features.collaboration

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.route
import io.newm.server.auth.jwt.AUTH_JWT
import io.newm.server.features.collaboration.model.CollaborationIdBody
import io.newm.server.features.collaboration.model.CollaborationReply
import io.newm.server.features.collaboration.model.collaborationFilters
import io.newm.server.features.collaboration.model.collaboratorFilters
import io.newm.server.features.collaboration.repo.CollaborationRepository
import io.newm.server.features.model.CountResponse
import io.newm.server.features.song.repo.SongRepository
import io.newm.server.ktx.collaborationId
import io.newm.server.ktx.limit
import io.newm.server.ktx.myUserId
import io.newm.server.ktx.offset
import io.newm.shared.koin.inject
import io.newm.shared.ktx.delete
import io.newm.shared.ktx.get
import io.newm.shared.ktx.patch
import io.newm.shared.ktx.post
import io.newm.shared.ktx.put

@Suppress("unused")
fun Routing.createCollaborationRoutes() {
    val collabRepository: CollaborationRepository by inject()
    val songRepository: SongRepository by inject()

    authenticate(AUTH_JWT) {
        route("v1/collaborations") {
            post {
                respond(CollaborationIdBody(collabRepository.add(receive(), myUserId)))
            }
            get {
                respond(collabRepository.getAll(myUserId, collaborationFilters, offset, limit))
            }
            get("count") {
                respond(CountResponse(collabRepository.getAllCount(myUserId, collaborationFilters)))
            }
            route("{collaborationId}") {
                patch {
                    collabRepository.update(receive(), collaborationId, myUserId)
                    respond(HttpStatusCode.NoContent)
                }
                get {
                    respond(collabRepository.get(collaborationId, myUserId))
                }
                delete {
                    collabRepository.delete(collaborationId, myUserId)
                    respond(HttpStatusCode.NoContent)
                }
                put("reply") {
                    val collab = collabRepository.reply(collaborationId, myUserId, receive<CollaborationReply>().accepted)
                    songRepository.processCollaborations(collab.songId!!)
                    respond(HttpStatusCode.NoContent)
                }
            }
            route("collaborators") {
                get {
                    respond(collabRepository.getCollaborators(myUserId, collaboratorFilters, offset, limit))
                }
                get("count") {
                    respond(CountResponse(collabRepository.getCollaboratorCount(myUserId, collaboratorFilters)))
                }
            }
        }
    }
}
