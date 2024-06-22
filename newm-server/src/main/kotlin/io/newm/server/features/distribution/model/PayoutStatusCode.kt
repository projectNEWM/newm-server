package io.newm.server.features.distribution.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class PayoutStatusCode(
    val code: Int
) {
    @SerialName("1111")
    SUCCESS(1111),

    @SerialName("1112")
    FAILED(1112),

    @SerialName("1113")
    PENDING(1113),

    @SerialName("1114")
    UNCLAIMED(1114),

    @SerialName("1115")
    RETURNED(1115),

    @SerialName("1116")
    ONHOLD(1116),

    @SerialName("1117")
    BLOCKED(1117),

    @SerialName("1118")
    REFUNDED(1118),

    @SerialName("1119")
    REVERSED(1119),

    @SerialName("1120")
    DENIED(1120),

    @SerialName("1121")
    PROCESSING(1121),

    @SerialName("1122")
    CANCELED(1122);

    companion object {
        fun getDescription(code: Int): String =
            when (code) {
                1111 -> "Funds have been credited to the recipient’s account"
                1112 -> "This payout request has failed, so funds were not deducted from the sender’s account"
                1113 -> "Your payout request was received and will be processed"
                1114 -> "The recipient for this payout does not have a PayPal account. A link to sign up for a PayPal account was sent to the recipient. However, if the recipient does not claim this payout within 30 days, the funds are returned to your account"
                1115 -> "The recipient has not claimed this payout, so the funds have been returned to your account"
                1116 -> "This payout request is being reviewed and is on hold"
                1117 -> "This payout request has been blocked"
                1118 -> "The recipient refunded your payment"
                1119 -> "This payout request was reversed"
                1120 -> "Your payout requests were denied, so they were not processed"
                1121 -> "Your payout requests were received and are now being processed"
                1122 -> "The payouts file that was uploaded through the PayPal portal was cancelled by the sender"
                else -> "Unknown status code"
            }
    }
}
