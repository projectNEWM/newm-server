package io.newm.ardrive.turbo.payment

import com.google.common.truth.Truth.assertThat
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.newm.ardrive.turbo.TurboConfig
import io.newm.ardrive.turbo.auth.ArweaveSigner
import io.newm.ardrive.turbo.payment.model.Currency
import io.newm.ardrive.turbo.upload.util.TestJwkFactory
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test

class PaymentCurrencyTest {
    @Test
    fun `getSupportedCurrencies returns limits`() =
        runBlocking {
            val engine = MockEngine { request ->
                assertThat(request.url.encodedPath).isEqualTo("/v1/currencies")
                respond(
                    content = """{"supportedCurrencies":["usd"],"limits":{"usd":{"minimumPaymentAmount":1,"maximumPaymentAmount":100,"suggestedPaymentAmounts":[5],"zeroDecimalCurrency":false}}}""",
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

            val response = service.getSupportedCurrencies()

            assertThat(response.supportedCurrencies).contains(Currency.USD)
            assertThat(response.limits[Currency.USD]?.minimumPaymentAmount).isEqualTo(1.0)
        }

    @Test
    fun `getSupportedCountries returns list`() =
        runBlocking {
            val engine = MockEngine { request ->
                assertThat(request.url.encodedPath).isEqualTo("/v1/countries")
                respond(
                    content = """["United States"]""",
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

            val countries = service.getSupportedCountries()

            assertThat(countries).contains("United States")
        }

    @Test
    fun `getConversionRates returns fiat map`() =
        runBlocking {
            val engine = MockEngine { request ->
                assertThat(request.url.encodedPath).isEqualTo("/v1/rates")
                respond(
                    content = """{"winc":"100","adjustments":[],"fees":[],"fiat":{"usd":1.5}}""",
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

            val rates = service.getConversionRates()

            assertThat(rates.fiat[Currency.USD]).isEqualTo(1.5)
        }

    @Test
    fun `getARRate returns rate`() =
        runBlocking {
            val engine = MockEngine { request ->
                assertThat(request.url.encodedPath).isEqualTo("/v1/rates/usd")
                respond(
                    content = """{"currency":"usd","rate":2.0}""",
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

            val rate = service.getARRate(Currency.USD)

            assertThat(rate.rate).isEqualTo(2.0)
        }
}
