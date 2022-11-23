package io.newm.server.features.user

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
import io.newm.server.auth.jwt.AUTH_JWT
import io.newm.server.di.inject
import io.newm.server.ext.identifyUser
import io.newm.server.ext.limit
import io.newm.server.ext.offset
import io.newm.server.ext.restrictToMe
import io.newm.server.features.user.model.userFilters
import io.newm.server.features.user.repo.UserRepository

private const val USERS_PATH = "v1/users"

fun Routing.createUserRoutes() {
    val repository: UserRepository by inject()

    authenticate(AUTH_JWT) {
        route("$USERS_PATH/{userId}") {
            get {
                identifyUser { userId, isMe ->
                    call.respond(repository.get(userId, isMe))
                }
            }
            patch {
                restrictToMe { myUserId ->
                    with(call) {
                        repository.update(myUserId, receive())
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
        get {
            with(call) {
                respond(repository.getAll(userFilters, offset, limit))
            }
        }
        put {
            with(call) {
                repository.add(receive())
                respond(HttpStatusCode.NoContent)
            }
        }
        put("password") {
            with(call) {
                repository.recover(receive())
                respond(HttpStatusCode.NoContent)
            }
        }
    }
}
