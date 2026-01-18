package io.newm.ardrive.turbo.util

import com.google.common.truth.Truth.assertThat
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.http.HttpStatusCode
import io.newm.ardrive.turbo.TurboConfig
import io.newm.ardrive.turbo.TurboLogSeverity
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import org.junit.jupiter.api.Test

class HttpClientFactoryTest {
    @Test
    fun `configures injected client when provided`() {
        val engine = MockEngine { respond("ok", HttpStatusCode.OK) }
        val client = HttpClient(engine)
        val config = TurboConfig(httpClient = client)

        val created = HttpClientFactory.create(config)

        assertThat(created).isNotSameInstanceAs(client)
    }

    @Test
    fun `backoff duration grows with retry count`() {
        val initial = 200.milliseconds
        val max = 2.seconds

        val first = HttpClientFactory.backoffDuration(initial, max, 2.0, 1)
        val second = HttpClientFactory.backoffDuration(initial, max, 2.0, 2)
        val third = HttpClientFactory.backoffDuration(initial, max, 2.0, 3)

        assertThat(first).isEqualTo(200.milliseconds)
        assertThat(second).isEqualTo(400.milliseconds)
        assertThat(third).isEqualTo(800.milliseconds)
    }

    @Test
    fun `backoff duration caps at max`() {
        val initial = 500.milliseconds
        val max = 1.seconds

        val duration = HttpClientFactory.backoffDuration(initial, max, 3.0, 4)

        assertThat(duration).isEqualTo(max)
    }

    @Test
    fun `backoff duration is zero for retry zero`() {
        val duration = HttpClientFactory.backoffDuration(200.milliseconds, 1.seconds, 2.0, 0)

        assertThat(duration).isEqualTo(0.seconds)
    }

    @Test
    fun `default config uses warn logging`() {
        val config = TurboConfig()

        assertThat(config.logLevel).isEqualTo(LogLevel.INFO)
        assertThat(config.logSeverity).isEqualTo(TurboLogSeverity.WARN)
    }
}
