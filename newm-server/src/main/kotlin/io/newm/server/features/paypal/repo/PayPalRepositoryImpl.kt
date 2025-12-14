package io.newm.server.features.paypal.repo

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.request.accept
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.server.application.ApplicationEnvironment
import io.newm.server.config.repo.ConfigRepository
import io.newm.server.config.repo.ConfigRepository.Companion.CONFIG_KEY_PAYPAL_ITEMIZED_INVOICE_ENABLED
import io.newm.server.features.paypal.model.MintingDistributionOrderRequest
import io.newm.server.features.paypal.model.MintingDistributionOrderResponse
import io.newm.server.features.paypal.model.PayPalAmount
import io.newm.server.features.paypal.model.PayPalCaptureOrderResponse
import io.newm.server.features.paypal.model.PayPalCreateOrderRequest
import io.newm.server.features.paypal.model.PayPalCreateOrderResponse
import io.newm.server.features.paypal.model.PayPalItem
import io.newm.server.features.song.model.MintPaymentOption
import io.newm.server.features.song.model.MintingStatus
import io.newm.server.features.song.model.PaymentType
import io.newm.server.features.song.repo.SongRepository
import io.newm.server.ktx.checkedBody
import io.newm.server.typealiases.SongId
import io.newm.server.typealiases.UserId
import io.newm.shared.exception.HttpForbiddenException
import io.newm.shared.exception.HttpUnprocessableEntityException
import io.newm.shared.ktx.getConfigString
import io.newm.shared.ktx.toUUID
import java.math.BigDecimal
import java.math.RoundingMode

internal class PayPalRepositoryImpl(
    private val environment: ApplicationEnvironment,
    private val configRepository: ConfigRepository,
    private val httpClient: HttpClient,
    private val songRepository: SongRepository
) : PayPalRepository {
    private val logger = KotlinLogging.logger {}

    private val ordersApiUrl: String
        get() = "${environment.getConfigString("payPal.apiUrl")}/v2/checkout/orders"

    override suspend fun createMintingDistributionOrder(
        requesterId: UserId,
        request: MintingDistributionOrderRequest
    ): MintingDistributionOrderResponse {
        logger.debug { "createMintingDistributionOrder: requesterId=$requesterId, request=$request" }

        val song = songRepository.get(request.songId)
        if (song.ownerId != requesterId) throw HttpForbiddenException("Operation allowed only by song owner")

        val paymentOption = getPaymentOption(request.songId)
        val response = httpClient
            .post(ordersApiUrl) {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                setBody(
                    PayPalCreateOrderRequest(
                        intent = "CAPTURE",
                        purchaseUnits = listOf(
                            PayPalCreateOrderRequest.PurchaseUnit(
                                invoiceId = request.songId.toString(),
                                amount = PayPalAmount(
                                    totalPriceUsd = paymentOption.price.toBigDecimalUsd(),
                                ),
                                items = if (configRepository.getBoolean(CONFIG_KEY_PAYPAL_ITEMIZED_INVOICE_ENABLED)) {
                                    listOf(
                                        PayPalItem(
                                            name = "Distribution cost",
                                            unitPriceUsd = paymentOption.dspPrice.toBigDecimalUsd()
                                        ),
                                        PayPalItem(
                                            name = "Royalty split(s) fee",
                                            unitPriceUsd = paymentOption.collabPrice.toBigDecimalUsd()
                                        ),
                                        PayPalItem(
                                            name = "Service fee",
                                            unitPriceUsd = paymentOption.mintPrice.toBigDecimalUsd()
                                        )
                                    )
                                } else {
                                    null
                                }
                            )
                        ),
                        paymentSource = PayPalCreateOrderRequest.PaymentSource(
                            paypal = PayPalCreateOrderRequest.PayPal(
                                experienceContext = PayPalCreateOrderRequest.ExperienceContext(
                                    brandName = "NEWM",
                                    paymentMethodPreference = "IMMEDIATE_PAYMENT_REQUIRED",
                                    shippingPreference = "NO_SHIPPING",
                                    userAction = "PAY_NOW",
                                    returnUrl = environment.getConfigString("payPal.orders.mintingDistribution.returnUrl"),
                                    cancelUrl = environment.getConfigString("payPal.orders.mintingDistribution.cancelUrl"),
                                )
                            )
                        )
                    ),
                )
            }.checkedBody<PayPalCreateOrderResponse>()

        response.status.checkResponseStatus("PAYER_ACTION_REQUIRED")

        val checkoutUrl = response.links.firstOrNull { it.rel == "payer-action" }?.href
            ?: throw HttpUnprocessableEntityException("No payer-action found in PayPal response")

        songRepository.updateSongMintingStatus(request.songId, MintingStatus.MintingPaymentRequested)
        logger.info { "Successfully created orderId=${response.id} for songId=${request.songId}" }
        return MintingDistributionOrderResponse(
            orderId = response.id,
            checkoutUrl = checkoutUrl
        )
    }

    override suspend fun captureMintingDistributionOrder(orderId: String) {
        logger.debug { "captureMintingDistributionOrder: orderId=$orderId" }

        val response = httpClient
            .post("$ordersApiUrl/$orderId/capture") {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
            }.checkedBody<PayPalCaptureOrderResponse>()

        response.status.checkResponseStatus("COMPLETED")

        val capture = response.purchaseUnits
            .firstOrNull()
            ?.payments
            ?.captures
            ?.firstOrNull()
            ?: throw HttpUnprocessableEntityException("No capture found in PayPal response")

        with(capture) {
            if (status != "COMPLETED") {
                throw HttpUnprocessableEntityException(
                    "Unexpected capture status - expected: COMPLETED, received: $status"
                )
            }

            val songId = invoiceId.toUUID()
            val price = getPaymentOption(songId).price.toBigDecimalUsd()
            if (amount.currencyCode != "USD" || amount.value != price) {
                throw HttpUnprocessableEntityException(
                    "Unexpected capture amount - expected $price USD, received: ${amount.value} ${amount.currencyCode}"
                )
            }

            songRepository.updateSongMintingStatus(songId, MintingStatus.MintingPaymentSubmitted)
            logger.info { "Successfully captured orderId=$orderId for songId=$songId" }
        }
    }

    private fun String.checkResponseStatus(status: String) {
        if (this != status) {
            throw HttpUnprocessableEntityException(
                "Unexpected PayPal response status - expected: $status, received: $this"
            )
        }
    }

    // PayPal only accepts 2 decimal places for USD amounts
    private fun String.toBigDecimalUsd(): BigDecimal = this.toBigDecimal().setScale(2, RoundingMode.HALF_UP)

    private suspend fun getPaymentOption(songId: SongId): MintPaymentOption =
        songRepository
            .getMintingPaymentAmount(songId, PaymentType.PAYPAL)
            .mintPaymentOptions!!
            .first { it.paymentType == PaymentType.PAYPAL }
}
