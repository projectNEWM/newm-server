package io.newm.shared.ktx

import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone

private val minutesSecondsFormatter =
    SimpleDateFormat("mm:ss").apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

fun Long?.orZero(): Long = this ?: 0L

fun Long.millisToMinutesSecondsString(): String = minutesSecondsFormatter.format(Date(this))
