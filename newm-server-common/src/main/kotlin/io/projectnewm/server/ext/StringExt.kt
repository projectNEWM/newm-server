package io.projectnewm.server.ext

import at.favre.lib.crypto.bcrypt.BCrypt
import java.net.URL
import java.util.UUID

// https://owasp.org/www-community/OWASP_Validation_Regex_Repository
private val EMAIL_REGEX = Regex(
    "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$"
)

// https://owasp.org/www-community/OWASP_Validation_Regex_Repository
private val URL_REGEX = Regex(
    "^((((https?|ftps?|gopher|telnet|nntp)://)|(mailto:|news:))(%[0-9A-Fa-f]{2}|[-()_.!~*';/?:@&=+$,A-Za-z0-9])+)([).!';/?:,][[:blank:]])?$"
)

fun String.toUUID(): UUID = UUID.fromString(this)

fun String.isValidEmail() = EMAIL_REGEX.matches(this)

fun String.isValidUrl() = URL_REGEX.matches(this)

fun String.toUrl(): URL = URL_REGEX.javaClass.getResource(this) ?: URL(this)

fun String.toHash(): String = BCrypt.withDefaults().hashToString(12, toCharArray())

fun String.verify(hash: String): Boolean = BCrypt.verifyer().verify(toCharArray(), hash).verified
