package io.newm.ardrive.turbo

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

data class RetryPolicy(
    val maxAttempts: Int = 3,
    val initialDelay: Duration = 200.milliseconds,
    val maxDelay: Duration = 5.seconds,
    val multiplier: Double = 2.0,
)
