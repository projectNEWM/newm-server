package io.newm.ardrive.turbo.payment

import io.newm.ardrive.turbo.model.TokenType
import io.newm.ardrive.turbo.payment.model.Currency
import io.newm.ardrive.turbo.payment.model.CryptoFundResponse
import io.newm.ardrive.turbo.payment.model.FiatPriceQuote
import io.newm.ardrive.turbo.payment.model.FiatToArRate
import io.newm.ardrive.turbo.payment.model.PaymentBalance
import io.newm.ardrive.turbo.payment.model.PaymentServiceInfo
import io.newm.ardrive.turbo.payment.model.PriceQuote
import io.newm.ardrive.turbo.payment.model.RatesResponse
import io.newm.ardrive.turbo.payment.model.SubmitFundTransactionResponse
import io.newm.ardrive.turbo.payment.model.SupportedCurrencies
import io.newm.ardrive.turbo.payment.model.TokenPriceForBytes
import io.newm.ardrive.turbo.payment.model.TokenPriceQuote
import io.newm.ardrive.turbo.payment.model.TopUpMethod
import io.newm.ardrive.turbo.payment.model.TopUpQuote
import io.newm.ardrive.turbo.payment.model.UiMode
import io.newm.ardrive.turbo.upload.model.UploadPrice

/**
 * Payment service endpoints for Turbo balance, pricing, and top-ups.
 */
interface PaymentService {
    suspend fun getBalance(
        userAddress: String? = null,
        token: TokenType = TokenType.ARWEAVE
    ): PaymentBalance

    suspend fun getPriceForBytes(byteCount: Long): PriceQuote

    suspend fun getPriceForPayment(
        token: TokenType,
        amount: String
    ): TokenPriceQuote

    suspend fun getPriceForPayment(
        currency: Currency,
        amount: Double,
        destinationAddress: String? = null,
        promoCodes: List<String> = emptyList(),
    ): FiatPriceQuote

    suspend fun getTokenPriceForBytes(
        token: TokenType,
        byteCount: Long
    ): TokenPriceForBytes

    suspend fun getUploadPrice(
        token: TokenType,
        byteCount: Long,
    ): UploadPrice

    suspend fun getSupportedCurrencies(): SupportedCurrencies

    suspend fun getSupportedCountries(): List<String>

    suspend fun getServiceInfo(): PaymentServiceInfo

    suspend fun getConversionRates(): RatesResponse

    suspend fun getARRate(currency: Currency): FiatToArRate

    suspend fun getTopUpQuote(
        method: TopUpMethod,
        owner: String,
        currency: Currency,
        amount: Double,
        token: TokenType = TokenType.ARWEAVE,
        uiMode: UiMode? = null,
        promoCodes: List<String> = emptyList(),
        successUrl: String? = null,
        cancelUrl: String? = null,
        returnUrl: String? = null,
    ): TopUpQuote

    suspend fun submitPendingPayment(
        token: TokenType,
        transactionId: String,
    ): SubmitFundTransactionResponse

    suspend fun submitFundTransaction(
        token: TokenType,
        transactionId: String,
    ): CryptoFundResponse

    suspend fun topUpWithTokens(
        token: TokenType,
        tokenAmount: String,
        feeMultiplier: Double = 1.0,
        turboCreditDestinationAddress: String? = null,
    ): CryptoFundResponse
}
