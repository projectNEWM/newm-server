package io.newm.ardrive.turbo

import io.ktor.client.HttpClient
import io.ktor.client.plugins.logging.LogLevel
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Configuration for Turbo upload and payment clients.
 */
data class TurboConfig(
    val uploadBaseUrl: String = "https://turbo.ardrive.io",
    val paymentBaseUrl: String = "https://payment.ardrive.io",
    val requestTimeout: Duration = 30.seconds,
    val connectTimeout: Duration = 10.seconds,
    val socketTimeout: Duration = 30.seconds,
    val retryPolicy: RetryPolicy = RetryPolicy(),
    val logLevel: LogLevel = LogLevel.INFO,
    val logSeverity: TurboLogSeverity = TurboLogSeverity.WARN,
    val httpClient: HttpClient? = null,
)
