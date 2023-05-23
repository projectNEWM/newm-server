package io.newm.server.features.distribution.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ReleaseData(
    @SerialName("albumStatus")
    val albumStatus: String,
    @SerialName("errorFields")
    val errorFields: List<ErrorField>? = null,
)
