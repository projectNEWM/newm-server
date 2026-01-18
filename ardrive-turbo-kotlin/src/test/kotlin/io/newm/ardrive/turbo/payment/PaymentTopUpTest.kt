package io.newm.ardrive.turbo.payment

import com.google.common.truth.Truth.assertThat
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.newm.ardrive.turbo.TurboConfig
import io.newm.ardrive.turbo.auth.ArweaveSigner
import io.newm.ardrive.turbo.model.TokenType
import io.newm.ardrive.turbo.payment.model.FundTransactionStatus
import io.newm.ardrive.turbo.payment.model.TopUpMethod
import io.newm.ardrive.turbo.payment.model.UiMode
import io.newm.ardrive.turbo.upload.util.TestJwkFactory
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test

class PaymentTopUpTest {
    @Test
    fun `getTopUpQuote builds normalized response`() =
        runBlocking {
            val engine = MockEngine { request ->
                assertThat(request.method).isEqualTo(HttpMethod.Get)
                assertThat(request.url.encodedPath).isEqualTo("/v1/top-up/checkout-session/owner/usd/10.0")
                assertThat(request.url.parameters["token"]).isEqualTo("arweave")
                assertThat(request.url.parameters["uiMode"]).isEqualTo("hosted")
                assertThat(request.url.parameters["promoCode"]).isNull()
                respond(
                    content =
                        """
                        {
                          "topUpQuote": {
                            "topUpQuoteId": "quote",
                            "destinationAddressType": "arweave",
                            "paymentAmount": 10.0,
                            "quotedPaymentAmount": 10.0,
                            "winstonCreditAmount": "100",
                            "destinationAddress": "dest",
                            "currencyType": "usd",
                            "quoteExpirationDate": "soon",
                            "paymentProvider": "stripe",
                            "tokenType": "arweave",
                            "adjustments": []
                          },
                          "paymentSession": {
                            "url": "https://pay",
                            "id": "session",
                            "client_secret": "secret"
                          },
                          "status": "created",
                          "txId": "tx",
                          "adjustments": [],
                          "fees": []
                        }
                        """.trimIndent(),
                    status = HttpStatusCode.OK,
                    headers = io.ktor.http.headersOf(HttpHeaders.ContentType, "application/json"),
                )
            }
            val client = HttpClient(engine) {
                install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            }
            val service = PaymentServiceImpl(
                config = TurboConfig(paymentBaseUrl = "https://payment.ardrive.io"),
                signer = ArweaveSigner(TestJwkFactory.create()),
                tokenTools = null,
                httpClient = client,
            )

            val quote = service.getTopUpQuote(
                method = TopUpMethod.CHECKOUT_SESSION,
                owner = "owner",
                currency = io.newm.ardrive.turbo.payment.model.Currency.USD,
                amount = 10.0,
                uiMode = UiMode.HOSTED,
                token = TokenType.ARWEAVE,
            )

            assertThat(quote.winc).isEqualTo("100")
            assertThat(quote.id).isEqualTo("session")
            assertThat(quote.url).isEqualTo("https://pay")
            assertThat(quote.clientSecret).isEqualTo("secret")
        }

    @Test
    fun `submitPendingPayment maps credited response`() =
        runBlocking {
            val engine = MockEngine { request ->
                assertThat(request.method).isEqualTo(HttpMethod.Post)
                assertThat(request.url.encodedPath).isEqualTo("/v1/account/balance/arweave")
                respond(
                    content =
                        """
                        {
                          "creditedTransaction": {
                            "transactionId": "tx",
                            "tokenType": "arweave",
                            "transactionQuantity": "1",
                            "winstonCreditAmount": "2",
                            "destinationAddress": "owner",
                            "destinationAddressType": "arweave",
                            "blockHeight": 10,
                            "transactionSenderAddress": "owner"
                          }
                        }
                        """.trimIndent(),
                    status = HttpStatusCode.OK,
                    headers = io.ktor.http.headersOf(HttpHeaders.ContentType, "application/json"),
                )
            }
            val client = HttpClient(engine) {
                install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            }
            val service = PaymentServiceImpl(
                config = TurboConfig(paymentBaseUrl = "https://payment.ardrive.io"),
                signer = ArweaveSigner(TestJwkFactory.create()),
                tokenTools = null,
                httpClient = client,
            )

            val response = service.submitPendingPayment(TokenType.ARWEAVE, "tx")

            assertThat(response.status).isEqualTo(FundTransactionStatus.CONFIRMED)
            assertThat(response.id).isEqualTo("tx")
        }
}
