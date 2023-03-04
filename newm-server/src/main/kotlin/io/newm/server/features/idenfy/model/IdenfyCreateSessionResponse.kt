package io.newm.server.features.idenfy.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class IdenfyCreateSessionResponse(
    @SerialName("authToken")
    val authToken: String,
    @SerialName("expiryTime")
    val expiryTime: Int
)
