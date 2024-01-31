package io.newm.server.ktx

import com.auth0.jwk.JwkProvider
import com.auth0.jwt.interfaces.RSAKeyProvider
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey

fun JwkProvider.toRSAKeyProvider(): RSAKeyProvider =
    object : RSAKeyProvider {
        override fun getPublicKeyById(keyId: String): RSAPublicKey = get(keyId).publicKey as RSAPublicKey

        override fun getPrivateKey(): RSAPrivateKey? = null

        override fun getPrivateKeyId(): String? = null
    }
