package io.projectnewm.server.features.cloudinary

import com.cloudinary.Cloudinary
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.post
import io.projectnewm.server.auth.jwt.AUTH_JWT
import io.projectnewm.server.di.inject
import io.projectnewm.server.features.cloudinary.model.CloudinarySignResponse
import java.time.Instant

fun Routing.createCloudinaryRoutes() {
    val cloudinary by inject<Cloudinary>()

    authenticate(AUTH_JWT) {
        post("/v1/cloudinary/sign") {
            with(call) {
                val timestamp = Instant.now().epochSecond
                val params = mutableMapOf<String, Any>().apply {
                    putAll(receive<Map<String, String>>())
                    put("timestamp", timestamp)
                }
                respond(
                    CloudinarySignResponse(
                        signature = cloudinary.apiSignRequest(params, cloudinary.config.apiSecret),
                        timestamp = timestamp,
                        cloudName = cloudinary.config.cloudName,
                        apiKey = cloudinary.config.apiKey
                    )
                )
            }
        }
    }
}
