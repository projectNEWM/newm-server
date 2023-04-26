package io.newm.shared.ktx

import at.favre.lib.crypto.bcrypt.BCrypt
import java.net.URL
import java.time.LocalDateTime
import java.util.UUID

/**
 * Based on android.util.Patterns.EMAIL_ADDRESS
 */
private val EMAIL_REGEX = Regex(
    pattern = """[a-zA-Z0-9+._%\-]{1,256}@[a-zA-Z0-9][a-zA-Z0-9\-]{0,64}(\.[a-zA-Z0-9][a-zA-Z0-9\-]{0,25})+"""
)

/**
 * Password regex, it must contain the following:
 * 8 characters, 1 uppercase letter, 1 lowercase letter and 1 number.
 */
private val PASSWORD_REGEX = Regex(
    pattern = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).{8,}\$"
)

private val HEX_REGEX = Regex(
    pattern = "([a-f0-9]{2})+",
    option = RegexOption.IGNORE_CASE,
)

fun String.toUUID(): UUID = UUID.fromString(this)

fun String.isValidEmail(): Boolean = EMAIL_REGEX.matches(this)

fun String.isValidPassword(): Boolean = PASSWORD_REGEX.matches(this)

fun String.isValidHex(): Boolean = HEX_REGEX.matches(this)

fun String.isValidUrl(): Boolean = try {
    URL(this).toURI()
    true
} catch (_: Exception) {
    false
}

fun String.toUrl(): URL = EMAIL_REGEX.javaClass.getResource(this) ?: URL(this)

fun String.toHash(): String = BCrypt.withDefaults().hashToString(12, toCharArray())

fun String.verify(hash: String): Boolean = BCrypt.verifyer().verify(toCharArray(), hash).verified

fun String.splitAndTrim(): List<String> = split(',').map { it.trim() }

fun String.toLocalDateTime(): LocalDateTime = LocalDateTime.parse(this)
