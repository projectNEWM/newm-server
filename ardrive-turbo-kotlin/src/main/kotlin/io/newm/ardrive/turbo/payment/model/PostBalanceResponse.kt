package io.newm.ardrive.turbo.payment.model

import io.newm.ardrive.turbo.model.TokenType
import kotlinx.serialization.Serializable

@Serializable
data class PostBalanceResponse(
    val pendingTransaction: PendingPaymentTransaction? = null,
    val creditedTransaction: CreditedPaymentTransaction? = null,
    val failedTransaction: FailedPaymentTransaction? = null,
    val message: String? = null,
)

@Serializable
data class PendingPaymentTransaction(
    val transactionId: String,
    val tokenType: TokenType,
    val transactionQuantity: String,
    val winstonCreditAmount: String,
    val destinationAddress: String,
    val destinationAddressType: String,
    val transactionSenderAddress: String? = null,
)

@Serializable
data class CreditedPaymentTransaction(
    val transactionId: String,
    val tokenType: TokenType,
    val transactionQuantity: String,
    val winstonCreditAmount: String,
    val destinationAddress: String,
    val destinationAddressType: String,
    val blockHeight: Int,
    val transactionSenderAddress: String? = null,
)

@Serializable
data class FailedPaymentTransaction(
    val transactionId: String,
    val tokenType: TokenType,
    val transactionQuantity: String,
    val winstonCreditAmount: String,
    val destinationAddress: String,
    val destinationAddressType: String,
    val failedReason: String,
    val transactionSenderAddress: String? = null,
)
