package io.newm.server.features.distribution.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AlbumStatus(
    @SerialName("status_code")
    val statusCode: Int,
    @SerialName("status_name")
    val statusName: String
)
