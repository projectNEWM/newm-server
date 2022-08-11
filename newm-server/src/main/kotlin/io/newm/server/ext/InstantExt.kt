package io.newm.server.ext

import java.time.Instant
import java.util.Date

fun Instant.toDate(): Date = Date.from(this)
