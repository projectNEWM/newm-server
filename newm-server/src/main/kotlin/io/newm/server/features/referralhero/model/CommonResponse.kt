package io.newm.server.features.referralhero.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CommonResponse(
    val status: String,
    val message: String?,
    val code: String?,
    val data: Data?
) {
    val isStatusOk: Boolean
        get() = status == "ok"

    @Serializable
    data class Data(
        val response: String,
        val code: String,
        @SerialName("referral_status")
        val referralStatus: String?,
        @SerialName("referred_by")
        val referredBy: Subscriber?
    )

    @Serializable
    data class Subscriber(
        val email: String?
    )
}
