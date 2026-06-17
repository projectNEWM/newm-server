package io.newm.server.security

import java.util.Base64

object KeyParser {
    fun parse(key: String): ByteArray {
        // Remove the header and footer lines from the private key string
        val base64Key =
            key
                .replace(headerFooterRegexPattern, "")
                .replace(whitespaceRegexPattern, "")

        return Base64.getDecoder().decode(base64Key)
    }

    private val headerFooterRegexPattern = Regex("-----.*?-----")
    private val whitespaceRegexPattern = Regex("\\s")
}
