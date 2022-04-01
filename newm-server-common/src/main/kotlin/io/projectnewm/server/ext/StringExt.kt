package io.projectnewm.server.ext

import at.favre.lib.crypto.bcrypt.BCrypt
import java.net.URL
import java.util.UUID

// Based on android.util.Patterns.EMAIL_ADDRESS
private val EMAIL_REGEX = Regex(
    """[a-zA-Z0-9+._%\-]{1,256}@[a-zA-Z0-9][a-zA-Z0-9\-]{0,64}(\.[a-zA-Z0-9][a-zA-Z0-9\-]{0,25})+"""
)

fun String.toUUID(): UUID = UUID.fromString(this)

fun String.isValidEmail(): Boolean = EMAIL_REGEX.matches(this)

fun String.isValidUrl(): Boolean = try {
    URL(this).toURI()
    true
} catch (_: Exception) {
    false
}

fun String.toUrl(): URL = EMAIL_REGEX.javaClass.getResource(this) ?: URL(this)

fun String.toHash(): String = BCrypt.withDefaults().hashToString(12, toCharArray())

fun String.verify(hash: String): Boolean = BCrypt.verifyer().verify(toCharArray(), hash).verified
