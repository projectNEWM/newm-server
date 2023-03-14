package io.newm.shared.ext

import java.security.SecureRandom
import kotlin.math.pow

fun SecureRandom.nextDigitCode(size: Int): String =
    nextLong(10.0.pow(size).toLong()).toString().padStart(size, '0')
