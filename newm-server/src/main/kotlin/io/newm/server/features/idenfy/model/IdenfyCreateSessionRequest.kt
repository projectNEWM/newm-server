package io.newm.server.features.idenfy.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class IdenfyCreateSessionRequest(
    @SerialName("clientId")
    val clientId: String
)
