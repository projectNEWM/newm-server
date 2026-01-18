package io.newm.ardrive.turbo.util

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.request.HttpRequest
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import io.newm.ardrive.turbo.RetryPolicy
import io.newm.ardrive.turbo.TurboConfig
import io.newm.ardrive.turbo.TurboLogSeverity
import kotlin.math.pow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.serialization.json.Json

object HttpClientFactory {
    fun create(config: TurboConfig): HttpClient =
        config.httpClient?.config {
            installTurboResponseValidation()
        } ?: buildClient(config)

    private fun buildClient(config: TurboConfig): HttpClient =
        HttpClient(CIO) {
            expectSuccess = false

            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        explicitNulls = false
                    }
                )
            }

            install(HttpTimeout) {
                requestTimeoutMillis = config.requestTimeout.inWholeMilliseconds
                connectTimeoutMillis = config.connectTimeout.inWholeMilliseconds
                socketTimeoutMillis = config.socketTimeout.inWholeMilliseconds
            }

            install(Logging) {
                logger = buildLogger(config.logSeverity)
                level = config.logLevel
                filter { config.logSeverity != TurboLogSeverity.NONE }
            }

            install(HttpRequestRetry) {
                maxRetries = config.retryPolicy.maxAttempts
                retryOnExceptionIf { _, cause ->
                    cause is java.io.IOException
                }
                retryIf { _: HttpRequest, response: HttpResponse ->
                    response.status.value in RETRY_STATUS_CODES
                }
                delayMillis { retry ->
                    computeDelayMillis(config.retryPolicy, retry)
                }
            }

            installTurboResponseValidation()
        }

    private fun io.ktor.client.HttpClientConfig<*>.installTurboResponseValidation() {
        HttpResponseValidator {
            validateResponse { response: HttpResponse ->
                val status = response.status
                if (status.isSuccess()) {
                    // Chunk upload endpoints return text/plain "OK" - that's fine
                    // Only JSON endpoints (init, finalize, status) need JSON validation,
                    // and those will fail at deserialization if the format is wrong.
                    return@validateResponse
                }

                throw TurboHttpException(
                    url = response.call.request.url
                        .toString(),
                    status = status,
                    contentType = response.contentType(),
                    responseBodySnippet = safeBodySnippet(response),
                )
            }
        }
    }

    private suspend fun safeBodySnippet(response: HttpResponse): String {
        val text = runCatching { response.bodyAsText() }.getOrNull().orEmpty()
        val cleaned = text.replace("\n", " ").replace("\r", " ").trim()
        return if (cleaned.length <= MAX_ERROR_BODY_CHARS) cleaned else cleaned.take(MAX_ERROR_BODY_CHARS) + "…"
    }

    private val RETRY_STATUS_CODES: Set<Int> =
        setOf(
            HttpStatusCode.RequestTimeout.value,
            HttpStatusCode.TooManyRequests.value,
            HttpStatusCode.BadGateway.value,
            HttpStatusCode.ServiceUnavailable.value,
            HttpStatusCode.GatewayTimeout.value,
        ) + (500..599)

    private fun computeDelayMillis(
        retryPolicy: RetryPolicy,
        retryCount: Int,
    ): Long =
        backoffDuration(
            retryPolicy.initialDelay,
            retryPolicy.maxDelay,
            retryPolicy.multiplier,
            retryCount,
        ).inWholeMilliseconds

    internal fun backoffDuration(
        initialDelay: Duration,
        maxDelay: Duration,
        multiplier: Double,
        retryCount: Int,
    ): Duration {
        if (retryCount <= 0) {
            return Duration.ZERO
        }
        val attempt = retryCount.coerceAtLeast(1) - 1
        val scaledMillis = initialDelay.inWholeMilliseconds * multiplier.pow(attempt)
        return scaledMillis.milliseconds.coerceAtMost(maxDelay)
    }

    private fun Double.pow(exponent: Int): Double = this.pow(exponent.toDouble())

    private fun buildLogger(severity: TurboLogSeverity): Logger {
        val log = KotlinLogging.logger("TurboHttpClient")
        return object : Logger {
            override fun log(message: String) {
                when (severity) {
                    TurboLogSeverity.WARN -> log.warn { message }
                    TurboLogSeverity.INFO -> log.info { message }
                    TurboLogSeverity.DEBUG -> log.debug { message }
                    TurboLogSeverity.NONE -> Unit
                }
            }
        }
    }

    private const val MAX_ERROR_BODY_CHARS = 4096
}
