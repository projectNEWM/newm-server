package io.newm.server.features.distribution.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EvearaSimpleResponse(
    @SerialName("message")
    val message: String,
    @SerialName("success")
    val success: Boolean
)
