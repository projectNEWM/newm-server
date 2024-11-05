package io.newm.shared.ktx

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.concurrent.getOrSet

private val minutesSecondsFormatter = ThreadLocal<SimpleDateFormat>()
private val groupingFormatter = ThreadLocal<NumberFormat>()

fun Long?.orZero(): Long = this ?: 0L

fun Long.epochSecondsToLocalDateTime(): LocalDateTime = LocalDateTime.ofEpochSecond(this, 0, ZoneOffset.UTC)

fun Long.millisToMinutesSecondsString(): String {
    val formatter = minutesSecondsFormatter.getOrSet {
        SimpleDateFormat("mm:ss").apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }
    return formatter.format(Date(this))
}

fun Long.toGroupingString(): String {
    val formatter = groupingFormatter.getOrSet {
        NumberFormat.getInstance(Locale.US).apply {
            isGroupingUsed = true
        }
    }
    return formatter.format(this)
}
