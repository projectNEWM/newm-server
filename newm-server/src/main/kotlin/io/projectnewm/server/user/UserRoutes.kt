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
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.projectnewm.server.ext.identifyUser
import io.projectnewm.server.ext.requiredQueryParam
import io.projectnewm.server.ext.restrictToMe
import io.projectnewm.server.koin.inject

private const val USERS_PATH = "v1/users"
private const val AUTH_CODE = "authCode"

fun Routing.createUserRoutes() {
    val repository: UserRepository by inject()

    authenticate("auth-jwt") {
        route("$USERS_PATH/{userId}") {
            get {
                identifyUser { userId, isMe ->
                    call.respond(repository.get(userId, isMe))
                }
            }
            patch {
                restrictToMe { myUserId ->
                    with(call) {
                        repository.update(myUserId, receive(), request.queryParameters[AUTH_CODE])
                        respond(HttpStatusCode.NoContent)
                    }
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

    route(USERS_PATH) {
        put {
            with(call) {
                repository.add(receive(), request.requiredQueryParam(AUTH_CODE))
                respond(HttpStatusCode.NoContent)
            }
        }
    }
}
