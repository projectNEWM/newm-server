package io.newm.ardrive.turbo.payment.model

import io.newm.ardrive.turbo.model.TokenType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SubmitFundTransactionResponse(
    val id: String,
    val quantity: String,
    val owner: String,
    val winc: String,
    val token: TokenType,
    val status: FundTransactionStatus,
    val recipient: String? = null,
    val block: Int? = null,
)

@Serializable
enum class FundTransactionStatus {
    @SerialName("pending")
    PENDING,

    @SerialName("confirmed")
    CONFIRMED,

    @SerialName("failed")
    FAILED,

    @SerialName("created")
    CREATED,
}
