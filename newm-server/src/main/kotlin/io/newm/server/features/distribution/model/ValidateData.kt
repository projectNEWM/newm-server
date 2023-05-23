package io.newm.server.features.distribution.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ValidateData(
    @SerialName("album_status")
    val albumStatus: AlbumStatus,
    @SerialName("error_fields")
    val errorFields: List<ErrorField>? = null,
)
