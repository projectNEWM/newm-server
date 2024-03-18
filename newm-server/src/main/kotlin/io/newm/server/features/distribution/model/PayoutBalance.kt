package io.newm.server.features.distribution.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PayoutBalance(
    @SerialName("currency")
    val currency: String,
    @SerialName("participant_id")
    val participantId: String,
    @SerialName("amount_to_pay")
    val amountToPay: String,
    @SerialName("name")
    val name: String
)
