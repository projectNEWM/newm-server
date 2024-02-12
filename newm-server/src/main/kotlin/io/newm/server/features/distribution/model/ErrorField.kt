package io.newm.server.features.distribution.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ErrorField(
    @SerialName("fields")
    val fields: String? = null,
    @SerialName("message")
    val message: String? = null,
)
