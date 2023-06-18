package io.newm.server.security

import java.security.KeyFactory
import java.security.PrivateKey
import java.security.spec.PKCS8EncodedKeySpec

object PrivateKeyReader {
    fun readFromString(privateKey: String): PrivateKey {
        val keyBytes = KeyParser.parse(privateKey)
        val keySpec = PKCS8EncodedKeySpec(keyBytes)
        return KeyFactory.getInstance("RSA").generatePrivate(keySpec)
    }
}
