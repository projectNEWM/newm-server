package io.newm.server.features.dripdropz.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CheckResponse(
    @SerialName("data")
    val data: Data
) {
    @Serializable
    data class Data(
        @SerialName("available_tokens")
        val availableTokens: List<AvailableToken>
    )
}
