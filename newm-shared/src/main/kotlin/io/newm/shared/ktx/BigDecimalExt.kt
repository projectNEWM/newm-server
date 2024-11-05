package io.newm.shared.ktx

import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

fun BigDecimal.toGroupingString(): String {
    val formatter = NumberFormat.getInstance(Locale.US).apply {
        isGroupingUsed = true
        maximumFractionDigits = scale()
    }
    return formatter.format(this)
}

fun BigDecimal?.orZero(): BigDecimal = this ?: BigDecimal.ZERO
