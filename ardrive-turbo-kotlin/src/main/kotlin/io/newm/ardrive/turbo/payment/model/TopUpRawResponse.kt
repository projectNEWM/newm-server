package io.newm.ardrive.turbo.payment.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class TopUpRawResponse(
    val topUpQuote: TopUpQuoteRaw,
    val paymentSession: PaymentSessionRaw,
    val status: FundTransactionStatus,
    val txId: String? = null,
    val adjustments: List<Adjustment> = emptyList(),
    val fees: List<Adjustment> = emptyList(),
)

@Serializable
internal data class TopUpQuoteRaw(
    val topUpQuoteId: String,
    val destinationAddressType: String,
    val paymentAmount: Double,
    val quotedPaymentAmount: Double,
    val winstonCreditAmount: String,
    val destinationAddress: String,
    val currencyType: Currency,
    val quoteExpirationDate: String,
    val paymentProvider: String,
    val tokenType: io.newm.ardrive.turbo.model.TokenType = io.newm.ardrive.turbo.model.TokenType.ARWEAVE,
    val adjustments: List<Adjustment> = emptyList(),
)

@Serializable
internal data class PaymentSessionRaw(
    val url: String? = null,
    val id: String,
    @SerialName("client_secret")
    val clientSecret: String? = null,
)
