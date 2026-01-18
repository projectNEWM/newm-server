package io.newm.ardrive.turbo.payment

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.newm.ardrive.turbo.TurboConfig
import io.newm.ardrive.turbo.auth.ArweaveSigner
import io.newm.ardrive.turbo.model.TokenType
import io.newm.ardrive.turbo.model.toApiValue
import io.newm.ardrive.turbo.payment.model.CryptoFundResponse
import io.newm.ardrive.turbo.payment.model.Currency
import io.newm.ardrive.turbo.payment.model.FiatPriceQuote
import io.newm.ardrive.turbo.payment.model.FiatToArRate
import io.newm.ardrive.turbo.payment.model.FundTransactionStatus
import io.newm.ardrive.turbo.payment.model.PaymentBalance
import io.newm.ardrive.turbo.payment.model.PaymentServiceInfo
import io.newm.ardrive.turbo.payment.model.PostBalanceResponse
import io.newm.ardrive.turbo.payment.model.PriceQuote
import io.newm.ardrive.turbo.payment.model.RatesResponse
import io.newm.ardrive.turbo.payment.model.RawTokenPriceQuote
import io.newm.ardrive.turbo.payment.model.SubmitFundTransactionResponse
import io.newm.ardrive.turbo.payment.model.SupportedCurrencies
import io.newm.ardrive.turbo.payment.model.TokenPriceForBytes
import io.newm.ardrive.turbo.payment.model.TokenPriceQuote
import io.newm.ardrive.turbo.payment.model.TopUpMethod
import io.newm.ardrive.turbo.payment.model.TopUpQuote
import io.newm.ardrive.turbo.payment.model.TopUpRawResponse
import io.newm.ardrive.turbo.payment.model.UiMode
import io.newm.ardrive.turbo.payment.model.toApiValue
import io.newm.ardrive.turbo.upload.model.UploadPrice
import io.newm.ardrive.turbo.util.HttpClientFactory
import io.newm.ardrive.turbo.util.TurboHttpException
import java.math.BigDecimal

import java.math.MathContext
import java.math.RoundingMode
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class PaymentServiceImpl(
    private val config: TurboConfig,
    private val signer: ArweaveSigner?,
    private val tokenTools: TokenTools?,
    private val httpClient: HttpClient = HttpClientFactory.create(config),
) : PaymentService {
    override suspend fun getBalance(
        userAddress: String?,
        token: TokenType
    ): PaymentBalance {
        val address = userAddress ?: signer?.getAddress()
            ?: error("userAddress is required when signer is not provided")
        val encodedAddress = URLEncoder.encode(address, StandardCharsets.UTF_8)
        return try {
            httpClient
                .get("${config.paymentBaseUrl}/v1/account/balance/${token.toApiValue()}?address=$encodedAddress")
                .body()
        } catch (exception: TurboHttpException) {
            if (exception.status == HttpStatusCode.NotFound) {
                PaymentBalance(
                    winc = "0",
                    controlledWinc = "0",
                    effectiveBalance = "0",
                    givenApprovals = emptyList(),
                    receivedApprovals = emptyList(),
                )
            } else {
                throw exception
            }
        }
    }

    override suspend fun getPriceForBytes(byteCount: Long): PriceQuote = httpClient.get("${config.paymentBaseUrl}/v1/price/bytes/$byteCount").body()

    override suspend fun getPriceForPayment(
        token: TokenType,
        amount: String
    ): TokenPriceQuote {
        val response = httpClient
            .get("${config.paymentBaseUrl}/v1/price/${token.toApiValue()}/$amount")
            .body<RawTokenPriceQuote>()
        return TokenPriceQuote(
            winc = response.winc,
            fees = response.fees,
            actualTokenAmount = amount,
            equivalentWincTokenAmount = response.actualPaymentAmount.content,
        )
    }

    override suspend fun getPriceForPayment(
        currency: Currency,
        amount: Double,
        destinationAddress: String?,
        promoCodes: List<String>,
    ): FiatPriceQuote {
        val queryParams = mutableListOf<String>()
        if (!destinationAddress.isNullOrBlank()) {
            val encoded = URLEncoder.encode(destinationAddress, StandardCharsets.UTF_8)
            queryParams += "destinationAddress=$encoded"
        }
        if (promoCodes.isNotEmpty()) {
            val encoded = URLEncoder.encode(promoCodes.joinToString(","), StandardCharsets.UTF_8)
            queryParams += "promoCode=$encoded"
        }
        val queryString = if (queryParams.isEmpty()) "" else "?${queryParams.joinToString("&")}"
        return httpClient
            .get("${config.paymentBaseUrl}/v1/price/${currency.toApiValue()}/$amount$queryString")
            .body()
    }

    override suspend fun getTokenPriceForBytes(
        token: TokenType,
        byteCount: Long
    ): TokenPriceForBytes {
        val bytesQuote = getPriceForBytes(ONE_GIB_BYTES)
        val tokenUnitAmount = TokenPriceCalculator.tokenUnitAmount(token)
        val tokenQuote = getPriceForPayment(token, tokenUnitAmount)
        val tokenPrice = TokenPriceCalculator.calculate(
            byteCount = byteCount,
            wincPerGiB = bytesQuote.winc,
            wincPerToken = tokenQuote.winc,
        )
        return TokenPriceForBytes(
            tokenPrice = tokenPrice,
            byteCount = byteCount,
            token = token,
        )
    }

    override suspend fun getUploadPrice(
        token: TokenType,
        byteCount: Long,
    ): UploadPrice {
        val quote = getTokenPriceForBytes(token, byteCount)
        return UploadPrice(
            tokenPrice = quote.tokenPrice,
            byteCount = quote.byteCount,
            token = quote.token,
        )
    }

    override suspend fun getSupportedCurrencies(): SupportedCurrencies = httpClient.get("${config.paymentBaseUrl}/v1/currencies").body()

    override suspend fun getSupportedCountries(): List<String> = httpClient.get("${config.paymentBaseUrl}/v1/countries").body()

    override suspend fun getServiceInfo(): PaymentServiceInfo = httpClient.get("${config.paymentBaseUrl}/v1/info").body()

    override suspend fun getConversionRates(): RatesResponse = httpClient.get("${config.paymentBaseUrl}/v1/rates").body()

    override suspend fun getARRate(currency: Currency): FiatToArRate = httpClient.get("${config.paymentBaseUrl}/v1/rates/${currency.toApiValue()}").body()

    override suspend fun getTopUpQuote(
        method: TopUpMethod,
        owner: String,
        currency: Currency,
        amount: Double,
        token: TokenType,
        uiMode: UiMode?,
        promoCodes: List<String>,
        successUrl: String?,
        cancelUrl: String?,
        returnUrl: String?,
    ): TopUpQuote {
        val queryParams = mutableMapOf(
            "token" to token.toApiValue(),
        )
        if (uiMode != null) queryParams["uiMode"] = uiMode.toApiValue()
        if (promoCodes.isNotEmpty()) queryParams["promoCode"] = promoCodes.joinToString(",")
        if (!successUrl.isNullOrBlank()) queryParams["successUrl"] = successUrl
        if (!cancelUrl.isNullOrBlank()) queryParams["cancelUrl"] = cancelUrl
        if (!returnUrl.isNullOrBlank()) queryParams["returnUrl"] = returnUrl

        val queryString = queryParams.entries.joinToString("&") { (key, value) ->
            "$key=${URLEncoder.encode(value, StandardCharsets.UTF_8)}"
        }
        val response = httpClient
            .get(
                "${config.paymentBaseUrl}/v1/top-up/${method.toApiValue()}/" +
                    "${URLEncoder.encode(owner, StandardCharsets.UTF_8)}/" +
                    "${currency.toApiValue()}/" +
                    "${URLEncoder.encode(amount.toString(), StandardCharsets.UTF_8)}?" +
                    queryString,
            ).body<TopUpRawResponse>()
        return TopUpQuote(
            winc = response.topUpQuote.winstonCreditAmount,
            adjustments = response.adjustments,
            fees = response.fees,
            id = response.paymentSession.id,
            url = response.paymentSession.url,
            clientSecret = response.paymentSession.clientSecret,
            paymentAmount = response.topUpQuote.paymentAmount,
            actualPaymentAmount = response.topUpQuote.paymentAmount,
            quotedPaymentAmount = response.topUpQuote.quotedPaymentAmount,
        )
    }

    override suspend fun submitPendingPayment(
        token: TokenType,
        transactionId: String,
    ): SubmitFundTransactionResponse {
        val response = httpClient.post("${config.paymentBaseUrl}/v1/account/balance/${token.toApiValue()}") {
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            setBody(mapOf("tx_id" to transactionId))
        }
        return response.body<PostBalanceResponse>().toSubmitFundTransactionResponse()
    }

    override suspend fun submitFundTransaction(
        token: TokenType,
        transactionId: String,
    ): CryptoFundResponse {
        val result = submitPendingPayment(token, transactionId)
        return CryptoFundResponse(
            id = result.id,
            quantity = result.quantity,
            owner = result.owner,
            winc = result.winc,
            token = result.token,
            status = result.status,
            recipient = result.recipient,
            block = result.block,
            target = "",
            reward = null,
        )
    }

    override suspend fun topUpWithTokens(
        token: TokenType,
        tokenAmount: String,
        feeMultiplier: Double,
        turboCreditDestinationAddress: String?,
    ): CryptoFundResponse {
        if (tokenTools == null) {
            throw IllegalStateException("TokenTools is required for token top-ups")
        }
        if (signer == null) {
            throw IllegalStateException("Signer is required for token top-ups")
        }
        val target = getTargetWalletForFund(token)
        val response = tokenTools.createAndSubmitTx(
            TokenCreateTxParams(
                target = target,
                tokenAmount = tokenAmount,
                feeMultiplier = feeMultiplier,
                signer = signer,
                turboCreditDestinationAddress = turboCreditDestinationAddress,
            ),
        )
        tokenTools.pollTxAvailability(response.id)
        val result = httpClient
            .post("${config.paymentBaseUrl}/v1/account/balance/${token.toApiValue()}") {
                header(HttpHeaders.ContentType, ContentType.Application.Json)
                setBody(mapOf("tx_id" to response.id))
            }.body<PostBalanceResponse>()
            .toSubmitFundTransactionResponse()
        return CryptoFundResponse(
            id = result.id,
            quantity = result.quantity,
            owner = result.owner,
            winc = result.winc,
            token = result.token,
            status = result.status,
            recipient = result.recipient,
            block = result.block,
            target = response.target,
            reward = response.reward,
        )
    }

    private suspend fun getTargetWalletForFund(token: TokenType): String {
        val info = getServiceInfo()
        val address = info.addresses[token]
        if (address.isNullOrBlank()) {
            throw IllegalStateException("No wallet address found for token type: $token")
        }
        return address
    }

    private fun PostBalanceResponse.toSubmitFundTransactionResponse(): SubmitFundTransactionResponse {
        creditedTransaction?.let {
            return SubmitFundTransactionResponse(
                id = it.transactionId,
                quantity = it.transactionQuantity,
                owner = it.transactionSenderAddress ?: it.destinationAddress,
                winc = it.winstonCreditAmount,
                token = it.tokenType,
                status = FundTransactionStatus.CONFIRMED,
                block = it.blockHeight,
                recipient = it.destinationAddress,
            )
        }
        pendingTransaction?.let {
            return SubmitFundTransactionResponse(
                id = it.transactionId,
                quantity = it.transactionQuantity,
                owner = it.transactionSenderAddress ?: it.destinationAddress,
                winc = it.winstonCreditAmount,
                token = it.tokenType,
                status = FundTransactionStatus.PENDING,
                recipient = it.destinationAddress,
            )
        }
        failedTransaction?.let {
            return SubmitFundTransactionResponse(
                id = it.transactionId,
                quantity = it.transactionQuantity,
                owner = it.transactionSenderAddress ?: it.destinationAddress,
                winc = it.winstonCreditAmount,
                token = it.tokenType,
                status = FundTransactionStatus.FAILED,
                recipient = it.destinationAddress,
            )
        }
        return SubmitFundTransactionResponse(
            id = "",
            quantity = "0",
            owner = "",
            winc = "0",
            token = TokenType.ARWEAVE,
            status = FundTransactionStatus.FAILED,
        )
    }

    private companion object {
        const val ONE_GIB_BYTES = 1024L * 1024L * 1024L
        const val TOKEN_PRICE_SCALE = 18
    }

    private object TokenPriceCalculator {
        fun calculate(
            byteCount: Long,
            wincPerGiB: String,
            wincPerToken: String,
        ): String {
            val wincPerGiBValue = wincPerGiB.toBigDecimalOrNull()
                ?: error("Invalid wincPerGiB value: $wincPerGiB")
            val wincPerTokenValue = wincPerToken.toBigDecimalOrNull()
                ?: error("Invalid wincPerToken value: $wincPerToken")
            val byteCountValue = byteCount.toBigDecimal()
            val bytesPerGiB = ONE_GIB_BYTES.toBigDecimal()

            val tokenPriceForGiB = wincPerGiBValue
                .divide(wincPerTokenValue, MathContext.DECIMAL128)
            val tokenPriceForBytes = tokenPriceForGiB
                .multiply(byteCountValue)
                .divide(bytesPerGiB, TOKEN_PRICE_SCALE, RoundingMode.HALF_UP)

            return tokenPriceForBytes.stripTrailingZeros().toPlainString()
        }

        fun tokenUnitAmount(token: TokenType): String {
            val exponent = tokenExponent(token)
            return BigDecimal.TEN.pow(exponent).toPlainString()
        }

        private fun tokenExponent(token: TokenType): Int =
            when (token) {
                TokenType.ARWEAVE -> 12
                TokenType.ARIO -> 6
                TokenType.BASE_ARIO -> 6
                TokenType.SOLANA -> 9
                TokenType.ETHEREUM -> 18
                TokenType.KYVE -> 6
                TokenType.MATIC -> 18
                TokenType.POL -> 18
                TokenType.BASE_ETH -> 18
                TokenType.USDC -> 6
                TokenType.BASE_USDC -> 6
                TokenType.POLYGON_USDC -> 6
                TokenType.ED25519 -> 6
            }
    }
}
