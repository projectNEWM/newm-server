package io.newm.server.features.user

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.route
import io.newm.server.auth.jwt.AUTH_JWT
import io.newm.server.auth.jwt.repo.JwtRepository
import io.newm.server.features.model.CountResponse
import io.newm.server.features.user.model.UserIdBody
import io.newm.server.features.user.model.userFilters
import io.newm.server.features.user.repo.UserRepository
import io.newm.server.ktx.clientPlatform
import io.newm.server.ktx.identifyUser
import io.newm.server.ktx.jwtId
import io.newm.server.ktx.limit
import io.newm.server.ktx.offset
import io.newm.server.ktx.restrictToMe
import io.newm.server.recaptcha.repo.RecaptchaRepository
import io.newm.shared.koin.inject
import io.newm.shared.ktx.delete
import io.newm.shared.ktx.get
import io.newm.shared.ktx.patch
import io.newm.shared.ktx.post
import io.newm.shared.ktx.put

private const val USERS_PATH = "v1/users"

fun Routing.createUserRoutes() {
    val recaptchaRepository: RecaptchaRepository by inject()
    val userRepository: UserRepository by inject()
    val jwtRepository: JwtRepository by inject()

    route(USERS_PATH) {
        authenticate(AUTH_JWT) {
            route("{userId}") {
                get {
                    identifyUser { userId, isMe ->
                        respond(userRepository.get(userId, isMe))
                    }
                }
                patch {
                    restrictToMe { myUserId ->
                        userRepository.update(myUserId, receive())
                        respond(HttpStatusCode.NoContent)
                    }
                }
                delete {
                    restrictToMe { myUserId ->
                        jwtId?.let { jwtRepository.blackList(it) }
                        userRepository.delete(myUserId)
                        respond(HttpStatusCode.NoContent)
                    }
                }
            }

            get {
                respond(userRepository.getAll(userFilters, offset, limit))
            }
            get("count") {
                respond(CountResponse(userRepository.getAllCount(userFilters)))
            }
        }
        post {
            recaptchaRepository.verify("signup", request)
            respond(UserIdBody(userRepository.add(receive(), clientPlatform)))
        }
        put("password") {
            recaptchaRepository.verify("password_reset", request)
            userRepository.recover(receive())
            respond(HttpStatusCode.NoContent)
        }
    }
}
