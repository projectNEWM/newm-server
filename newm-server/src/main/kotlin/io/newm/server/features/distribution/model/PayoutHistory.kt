package io.newm.server.features.distribution.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PayoutHistory(
    @SerialName("payout_amount")
    val payoutAmount: String,
    @SerialName("currency")
    val currency: String,
    @SerialName("participant_id")
    val participantId: String,
    @SerialName("paid_date")
    val paidDate: String,
    @SerialName("payout_status")
    val payoutStatus: PayoutStatus,
    @SerialName("name")
    val name: String
)
