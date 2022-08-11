package io.newm.server.features.cloudinary.model

import kotlinx.serialization.Serializable

@Serializable
data class CloudinarySignResponse(
    val signature: String,
    val timestamp: Long,
    val cloudName: String,
    val apiKey: String
)
