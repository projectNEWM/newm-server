package io.newm.server.security

import io.ktor.util.decodeBase64Bytes

object KeyParser {
    fun parse(key: String): ByteArray {
        // Remove the header and footer lines from the private key string
        val base64Key = key
            .replace("\\-\\-\\-\\-\\-.*".toRegex(), "")
            .replace("\\s".toRegex(), "")

        return base64Key.decodeBase64Bytes()
    }
}
