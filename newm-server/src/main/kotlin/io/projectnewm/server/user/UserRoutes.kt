package io.projectnewm.server.user

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.route
import io.projectnewm.server.ext.identifyUser
import io.projectnewm.server.ext.restrictToMe
import io.projectnewm.server.koin.inject

fun Routing.createUserRoutes() {
    val repository: UserRepository by inject()

    authenticate("auth-jwt") {
        route("v1/users/{userId}") {
            get {
                identifyUser { userId, isMe ->
                    call.respond(repository.get(userId, isMe))
                }
            }
            patch {
                restrictToMe { myUserId ->
                    repository.update(myUserId, call.receive())
                    call.respond(HttpStatusCode.NoContent)
                }
            }
            delete {
                restrictToMe { myUserId ->
                    repository.delete(myUserId)
                    call.respond(HttpStatusCode.NoContent)
                }
            }
        }
    }
}
