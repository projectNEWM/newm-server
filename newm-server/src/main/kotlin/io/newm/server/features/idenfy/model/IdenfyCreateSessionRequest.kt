package io.newm.server.features.idenfy.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class IdenfyCreateSessionRequest(
    @SerialName("clientId")
    val clientId: String,
    @SerialName("firstName")
    val firstName: String,
    @SerialName("lastName")
    val lastName: String
)
