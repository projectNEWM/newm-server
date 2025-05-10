package io.newm.shared.ktx

import at.favre.lib.crypto.bcrypt.BCrypt
import io.github.oshai.kotlinlogging.KotlinLogging
import java.net.URI
import java.net.URL
import java.net.URLConnection
import java.net.URLStreamHandler
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeParseException
import java.util.UUID

private val log by lazy { KotlinLogging.logger {} }

/**
 * Based on android.util.Patterns.EMAIL_ADDRESS
 */
private val EMAIL_REGEX =
    Regex(
        pattern = """[a-zA-Z0-9+._%\-]{1,256}@[a-zA-Z0-9][a-zA-Z0-9\-]{0,64}(\.[a-zA-Z0-9][a-zA-Z0-9\-]{0,25})+"""
    )

/**
 * Password regex, it must contain the following:
 * 8 characters, 1 uppercase letter, 1 lowercase letter and 1 number.
 */
private val PASSWORD_REGEX =
    Regex(
        pattern = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).{8,}\$"
    )

private val HEX_REGEX =
    Regex(
        pattern = "([a-f0-9]{2})+",
        option = RegexOption.IGNORE_CASE,
    )

private val FORMAT_REGEX = Regex("""\{([^}]+)}""")

private val INVALID_NAME_CHARS_REGEX = Regex("[\\\\/]")

private val CLOUDINARY_RESIZE_REGEX = Regex("""/c_limit[^/]+/""")

// Allows validating URL format ignoring the protocol value (e.g., "s3:", "ar:")
private object DummyURLHandler : URLStreamHandler() {
    override fun openConnection(url: URL): URLConnection =
        object : URLConnection(url) {
            override fun connect() {
            }
        }
}

/**
 * Returns the string if it is not blank, otherwise null.
 */
fun String.orNull(): String? = takeIf { isNotBlank() }?.trim()

fun String.toUUID(): UUID =
    try {
        UUID.fromString(this)
    } catch (e: Throwable) {
        log.error { "Error converting string to UUID: \"$this\"" }
        throw e
    }

fun String.isValidName(): Boolean = !contains(INVALID_NAME_CHARS_REGEX)

fun String.isValidEmail(): Boolean = EMAIL_REGEX.matches(this)

fun String.isValidPassword(): Boolean = PASSWORD_REGEX.matches(this)

fun String.isValidHex(): Boolean = HEX_REGEX.matches(this)

fun String.isValidUrl(): Boolean =
    try {
        URL.of(URI.create(this), DummyURLHandler).toURI()
        true
    } catch (_: Exception) {
        false
    }

fun String.toUrl(): URL = EMAIL_REGEX.javaClass.getResource(this) ?: URI.create(this).toURL()

fun String.removeCloudinaryResize(): String = replace(CLOUDINARY_RESIZE_REGEX, "/")

fun String.toHash(): String = BCrypt.withDefaults().hashToString(12, toCharArray())

fun String.verify(hash: String): Boolean = BCrypt.verifyer().verify(toCharArray(), hash).verified

fun String.splitAndTrim(): List<String> = split(',').map { it.trim() }

fun String.sanitizeName(): String = replace(INVALID_NAME_CHARS_REGEX, "")

fun String.toLocalDateTime(): LocalDateTime = LocalDateTime.parse(this)

fun String.toDuration(): Duration = Duration.parse(this)

fun String.toDurationOrNull(): Duration? =
    try {
        toDuration()
    } catch (_: DateTimeParseException) {
        null
    }

/**
 * Formats a String with curly brace delimited arguments.
 */
fun String.format(args: Map<String, Any?>): String =
    FORMAT_REGEX.replace(this) { result ->
        args[result.groupValues[1]]?.toString() ?: result.value
    }
