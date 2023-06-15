package io.newm.server.security

import io.ktor.util.decodeBase64Bytes

class KeyParser {
    companion object {
        fun parse(key: String): ByteArray {
            // Remove the header and footer lines from the private key string
            val base64Key = key
                .replace(Regex("\\-\\-\\-\\-\\-.*"), "")
                .replace(Regex("\\n"), "")
                .trim()

            return base64Key.decodeBase64Bytes()
        }
    }
}
