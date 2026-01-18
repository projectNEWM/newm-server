package io.newm.ardrive.turbo

import com.google.common.truth.Truth.assertThat
import io.ktor.client.plugins.logging.LogLevel
import kotlin.time.Duration.Companion.seconds
import org.junit.jupiter.api.Test

class TurboConfigTest {
    @Test
    fun `defaults are set`() {
        val config = TurboConfig()

        assertThat(config.uploadBaseUrl).isEqualTo("https://turbo.ardrive.io")
        assertThat(config.paymentBaseUrl).isEqualTo("https://payment.ardrive.io")
        assertThat(config.requestTimeout).isEqualTo(30.seconds)
        assertThat(config.connectTimeout).isEqualTo(10.seconds)
        assertThat(config.socketTimeout).isEqualTo(30.seconds)
        assertThat(config.retryPolicy).isEqualTo(RetryPolicy())
        assertThat(config.logLevel).isEqualTo(LogLevel.INFO)
        assertThat(config.logSeverity).isEqualTo(TurboLogSeverity.WARN)
        assertThat(config.httpClient).isNull()
    }
}
