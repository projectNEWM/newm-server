package io.newm.server.features.arweave.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WeaveRequest(
    @SerialName("body")
    val body: String,
)
