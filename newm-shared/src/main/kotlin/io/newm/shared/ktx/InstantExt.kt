package io.newm.shared.ktx

import java.time.Instant
import java.util.Date

fun Instant.toDate(): Date = Date.from(this)
