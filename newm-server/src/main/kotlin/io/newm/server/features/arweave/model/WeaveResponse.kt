package io.newm.server.features.arweave.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WeaveResponse(
    @SerialName("statusCode")
    val statusCode: Int,
    @SerialName("body")
    val body: String,
)
