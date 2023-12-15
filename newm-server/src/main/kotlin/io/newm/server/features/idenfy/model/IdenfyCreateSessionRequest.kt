package io.newm.server.features.idenfy.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class IdenfyCreateSessionRequest(
    @SerialName("clientId")
    val clientId: String,
    @SerialName("successUrl")
    val successUrl: String,
    @SerialName("errorUrl")
    val errorUrl: String,
)
