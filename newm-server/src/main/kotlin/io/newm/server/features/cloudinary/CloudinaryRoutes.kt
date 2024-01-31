package io.newm.server.features.cloudinary

import com.cloudinary.Cloudinary
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.newm.server.auth.jwt.AUTH_JWT
import io.newm.shared.koin.inject
import io.newm.shared.ktx.value
import io.newm.server.features.cloudinary.model.CloudinarySignResponse
import io.newm.shared.ktx.post
import java.time.Instant
import kotlinx.serialization.json.JsonPrimitive

fun Routing.createCloudinaryRoutes() {
    val cloudinary by inject<Cloudinary>()

    authenticate(AUTH_JWT) {
        post("/v1/cloudinary/sign") {
            val timestamp = Instant.now().epochSecond
            val params =
                mutableMapOf<String, Any>().apply {
                    receive<Map<String, JsonPrimitive>>().mapValuesTo(this) { it.value.value }
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
