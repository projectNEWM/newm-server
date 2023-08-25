package io.newm.shared.ktx

import java.math.BigDecimal

fun BigDecimal?.orZero(): BigDecimal = this ?: BigDecimal.ZERO
