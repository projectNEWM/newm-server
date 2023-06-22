package io.newm.server.features.distribution.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GetLanguagesResponse(
    @SerialName("success")
    val success: Boolean,
    @SerialName("data")
    val languages: List<Language>
)
