package io.newm.server.features.distribution.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EvearaErrorResponse(
    @SerialName("message")
    val message: String,
    @SerialName("success")
    val success: Boolean,
    @SerialName("errors")
    val errors: List<Error>? = null,
    @SerialName("status_code")
    val statusCode: Int? = null
)
