package io.newm.shared.ktx

import com.google.common.truth.Truth
import org.junit.jupiter.api.Test
import java.time.Duration

private val validDurations =
    listOf(
        "PT200S" to Duration.ofSeconds(200),
        "PT5M30S" to Duration.ofMinutes(5).plusSeconds(30),
        "PT1H5M15S" to Duration.ofHours(1).plusMinutes(5).plusSeconds(15),
    )

private val invalidDurations =
    listOf(
        "",
        " ",
        "JUNK",
    )

class StringToDurationTests {
    @Test
    fun testIsValidDurations() {
        for (duration in validDurations) {
            Truth.assertWithMessage(duration.first).that(duration.first.toDurationOrNull()).isEqualTo(duration.second)
        }
    }

    @Test
    fun testInvalidDurations() {
        for (duration in invalidDurations) {
            Truth.assertWithMessage(duration).that(duration.toDurationOrNull()).isNull()
        }
    }
}
