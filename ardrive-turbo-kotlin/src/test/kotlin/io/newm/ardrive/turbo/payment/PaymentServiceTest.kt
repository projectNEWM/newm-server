package io.newm.ardrive.turbo.payment

import com.google.common.truth.Truth.assertThat
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import io.newm.ardrive.turbo.TurboConfig
import io.newm.ardrive.turbo.auth.ArweaveSigner
import io.newm.ardrive.turbo.model.TokenType
import io.newm.ardrive.turbo.payment.model.Currency
import io.newm.ardrive.turbo.upload.util.TestJwkFactory
import io.newm.ardrive.turbo.util.HttpClientFactory
import io.newm.ardrive.turbo.util.TurboHttpException
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class PaymentServiceTest {
    @Test
    fun `getBalance uses provided address`() =
        runBlocking {
            val engine = MockEngine { request ->
                assertThat(request.method).isEqualTo(HttpMethod.Get)
                assertThat(request.url.encodedPath).isEqualTo("/v1/account/balance/arweave")
                assertThat(request.url.parameters["address"]).isEqualTo("addr")
                respond(
                    content = """{"controlledWinc":"1","winc":"2","effectiveBalance":"3","receivedApprovals":[],"givenApprovals":[]}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            }
            val config = TurboConfig(
                paymentBaseUrl = "https://payment.ardrive.io",
                httpClient = HttpClient(engine) {
                    install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
                },
            )
            val client = HttpClientFactory.create(config)
            val service = PaymentServiceImpl(
                config = config,
                signer = ArweaveSigner(TestJwkFactory.create()),
                tokenTools = null,
                httpClient = client,
            )

            val balance = service.getBalance("addr", TokenType.ARWEAVE)

            assertThat(balance.winc).isEqualTo("2")
        }

    @Test
    fun `getBalance uses signer address when omitted`() =
        runBlocking {
            val engine = MockEngine { request ->
                assertThat(request.method).isEqualTo(HttpMethod.Get)
                assertThat(request.url.encodedPath).isEqualTo("/v1/account/balance/arweave")
                respond(
                    content = """{"controlledWinc":"1","winc":"2","effectiveBalance":"3","receivedApprovals":[],"givenApprovals":[]}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            }
            val config = TurboConfig(
                paymentBaseUrl = "https://payment.ardrive.io",
                httpClient = HttpClient(engine) {
                    install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
                },
            )
            val client = HttpClientFactory.create(config)
            val signer = ArweaveSigner(TestJwkFactory.create())
            val service = PaymentServiceImpl(
                config = config,
                signer = signer,
                tokenTools = null,
                httpClient = client,
            )

            val balance = service.getBalance(token = TokenType.ARWEAVE)

            assertThat(balance.winc).isEqualTo("2")
        }

    @Test
    fun `getBalance returns zero on 404`() =
        runBlocking {
            val engine = MockEngine { request ->
                assertThat(request.url.encodedPath).isEqualTo("/v1/account/balance/arweave")
                respond(
                    content = "User Not Found",
                    status = HttpStatusCode.NotFound,
                    headers = headersOf(HttpHeaders.ContentType, "text/plain"),
                )
            }
            val config = TurboConfig(
                paymentBaseUrl = "https://payment.ardrive.io",
                httpClient = HttpClient(engine) {
                    install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
                },
            )
            val client = HttpClientFactory.create(config)
            val service = PaymentServiceImpl(
                config = config,
                signer = ArweaveSigner(TestJwkFactory.create()),
                tokenTools = null,
                httpClient = client,
            )

            val balance = service.getBalance("addr", TokenType.ARWEAVE)

            assertThat(balance.winc).isEqualTo("0")
            assertThat(balance.receivedApprovals).isEmpty()
            assertThat(balance.givenApprovals).isEmpty()
        }

    @Test
    fun `getPriceForBytes returns price quote`() =
        runBlocking {
            val engine = MockEngine { request ->
                assertThat(request.url.encodedPath).isEqualTo("/v1/price/bytes/100")
                respond(
                    content = """{"winc":"10","adjustments":[],"fees":[]}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            }
            val config = TurboConfig(
                paymentBaseUrl = "https://payment.ardrive.io",
                httpClient = HttpClient(engine) {
                    install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
                },
            )
            val client = HttpClientFactory.create(config)
            val service = PaymentServiceImpl(
                config = config,
                signer = ArweaveSigner(TestJwkFactory.create()),
                tokenTools = null,
                httpClient = client,
            )

            val quote = service.getPriceForBytes(100)

            assertThat(quote.winc).isEqualTo("10")
        }

    @Test
    fun `getPriceForPayment token returns token quote`() =
        runBlocking {
            val engine = MockEngine { request ->
                assertThat(request.url.encodedPath).isEqualTo("/v1/price/arweave/123")
                respond(
                    content = """{"winc":"1","fees":[],"actualPaymentAmount":"5"}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            }
            val config = TurboConfig(
                paymentBaseUrl = "https://payment.ardrive.io",
                httpClient = HttpClient(engine) {
                    install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
                },
            )
            val client = HttpClientFactory.create(config)
            val service = PaymentServiceImpl(
                config = config,
                signer = ArweaveSigner(TestJwkFactory.create()),
                tokenTools = null,
                httpClient = client,
            )

            val quote = service.getPriceForPayment(TokenType.ARWEAVE, "123")

            assertThat(quote.equivalentWincTokenAmount).isEqualTo("5")
            assertThat(quote.actualTokenAmount).isEqualTo("123")
        }

    @Test
    fun `getPriceForPayment currency returns fiat quote`() =
        runBlocking {
            val engine = MockEngine { request ->
                assertThat(request.url.encodedPath).isEqualTo("/v1/price/usd/12.5")
                assertThat(request.url.parameters["destinationAddress"]).isEqualTo("addr")
                assertThat(request.url.parameters["promoCode"]).isEqualTo("PROMO")
                respond(
                    content = """{"winc":"1","adjustments":[],"fees":[],"actualPaymentAmount":12.5,"quotedPaymentAmount":12.5}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            }
            val config = TurboConfig(
                paymentBaseUrl = "https://payment.ardrive.io",
                httpClient = HttpClient(engine) {
                    install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
                },
            )
            val client = HttpClientFactory.create(config)
            val service = PaymentServiceImpl(
                config = config,
                signer = ArweaveSigner(TestJwkFactory.create()),
                tokenTools = null,
                httpClient = client,
            )

            val quote = service.getPriceForPayment(
                currency = Currency.USD,
                amount = 12.5,
                destinationAddress = "addr",
                promoCodes = listOf("PROMO"),
            )

            assertThat(quote.actualPaymentAmount).isEqualTo(12.5)
        }

    @Test
    fun `getTokenPriceForBytes derives price`() =
        runBlocking {
            val engine = MockEngine { request ->
                when (request.url.encodedPath) {
                    "/v1/price/bytes/1073741824" -> respond(
                        content = """{"winc":"1000","adjustments":[],"fees":[]}""",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )

                    "/v1/price/ario/1000000" -> respond(
                        content = """{"winc":"500","fees":[],"actualPaymentAmount":"1000000"}""",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )

                    "/v1/price/arweave/1000000000000" -> respond(
                        content = """{"winc":"500","fees":[],"actualPaymentAmount":"1000000000000"}""",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )

                    else -> respond(
                        content = "{}",
                        status = HttpStatusCode.NotFound,
                    )
                }
            }
            val config = TurboConfig(
                paymentBaseUrl = "https://payment.ardrive.io",
                httpClient = HttpClient(engine) {
                    install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
                },
            )
            val client = HttpClientFactory.create(config)
            val service = PaymentServiceImpl(
                config = config,
                signer = ArweaveSigner(TestJwkFactory.create()),
                tokenTools = null,
                httpClient = client,
            )

            val quote = service.getTokenPriceForBytes(TokenType.ARIO, 2048)
            val arQuote = service.getTokenPriceForBytes(TokenType.ARWEAVE, 2048)

            assertThat(arQuote.token).isEqualTo(TokenType.ARWEAVE)

            assertThat(quote.token).isEqualTo(TokenType.ARIO)
            assertThat(quote.byteCount).isEqualTo(2048)
            assertThat(quote.tokenPrice).isNotEmpty()
        }

    @Test
    fun `getUploadPrice derives token price for bytes`() =
        runBlocking {
            val engine = MockEngine { request ->
                when (request.url.encodedPath) {
                    "/v1/price/bytes/1073741824" -> respond(
                        content = """{"winc":"1000","adjustments":[],"fees":[]}""",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )

                    "/v1/price/arweave/1000000000000" -> respond(
                        content = """{"winc":"500","fees":[],"actualPaymentAmount":"1000000000000"}""",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )

                    else -> respond(
                        content = "{}",
                        status = HttpStatusCode.NotFound,
                    )
                }
            }
            val config = TurboConfig(
                paymentBaseUrl = "https://payment.ardrive.io",
                httpClient = HttpClient(engine) {
                    install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
                },
            )
            val client = HttpClientFactory.create(config)
            val service = PaymentServiceImpl(
                config = config,
                signer = ArweaveSigner(TestJwkFactory.create()),
                tokenTools = null,
                httpClient = client,
            )

            val price = service.getUploadPrice(TokenType.ARWEAVE, 2048)

            assertThat(price.token).isEqualTo(TokenType.ARWEAVE)
            assertThat(price.byteCount).isEqualTo(2048)
            assertThat(price.tokenPrice).isNotEmpty()
        }

    @Test
    fun `getBalance returns empty on text plain 404`() =
        runBlocking {
            val engine = MockEngine {
                respond(
                    content = "Not found",
                    status = HttpStatusCode.NotFound,
                    headers = headersOf(HttpHeaders.ContentType, "text/plain; charset=utf-8"),
                )
            }
            val config = TurboConfig(
                paymentBaseUrl = "https://payment.ardrive.io",
                httpClient = HttpClient(engine) {
                    install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
                },
            )
            val client = HttpClientFactory.create(config)
            val service = PaymentServiceImpl(
                config = config,
                signer = ArweaveSigner(TestJwkFactory.create()),
                tokenTools = null,
                httpClient = client,
            )

            val balance = service.getBalance("addr", TokenType.ARWEAVE)

            assertThat(balance.winc).isEqualTo("0")
            assertThat(balance.receivedApprovals).isEmpty()
            assertThat(balance.givenApprovals).isEmpty()
        }
}
