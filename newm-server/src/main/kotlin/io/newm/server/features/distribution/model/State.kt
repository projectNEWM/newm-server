package io.newm.server.features.distribution.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class State(
    @SerialName("state_code")
    val stateCode: String,
    @SerialName("state_name")
    val stateName: String,
)
