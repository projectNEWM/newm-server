package io.newm.server.features.dripdropz.model

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
data class AvailableToken(
    @SerialName("token_policy")
    val tokenPolicy: String,
    @SerialName("token_asset_id")
    val tokenAssetId: String,
    @Contextual
    @SerialName("available_quantity")
    val availableQuantity: BigDecimal,
)
