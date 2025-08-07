package io.newm.server.features.paypal.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PayPalCreateOrderResponse(
    @SerialName("id")
    val id: String,
    @SerialName("status")
    val status: String,
    @SerialName("links")
    val links: List<Link>
) {
    @Serializable
    data class Link(
        @SerialName("href")
        val href: String,
        @SerialName("rel")
        val rel: String
    )
}
