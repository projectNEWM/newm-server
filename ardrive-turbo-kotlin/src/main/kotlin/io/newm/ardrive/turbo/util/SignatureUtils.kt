package io.newm.ardrive.turbo.util

import java.security.SecureRandom
import java.util.Base64

private val secureRandom = SecureRandom()

fun generateNonce(): String {
    val bytes = ByteArray(16)
    secureRandom.nextBytes(bytes)
    return bytes.joinToString(separator = "") { byte ->
        byte
            .toInt()
            .and(0xff)
            .toString(16)
            .padStart(2, '0')
    }
}

fun toBase64Url(input: ByteArray): String = Base64.getUrlEncoder().withoutPadding().encodeToString(input)
