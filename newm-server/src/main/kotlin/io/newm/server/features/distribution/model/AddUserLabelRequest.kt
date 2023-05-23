package io.newm.server.features.distribution.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AddUserLabelRequest(
    @SerialName("uuid")
    val uuid: String,
    @SerialName("name")
    val name: String
)
