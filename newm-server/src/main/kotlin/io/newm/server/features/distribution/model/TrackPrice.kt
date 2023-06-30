package io.newm.server.features.distribution.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TrackPrice(
    @SerialName("price")
    val price: Double,
    @SerialName("price_id")
    val priceId: Long,
)
