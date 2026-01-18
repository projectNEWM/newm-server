package io.newm.ardrive.turbo.auth

import com.google.common.truth.Truth.assertThat
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPrivateCrtKey
import java.util.Base64
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test

class ArweaveSignerTest {
    @Test
    fun `generates signature headers`() {
        val jwk = createTestJwk()
        val signer = ArweaveSigner(jwk)

        val headers = runBlocking { signer.generateSignatureHeaders() }

        assertThat(headers.nonce).isNotEmpty()
        assertThat(headers.signature).isNotEmpty()
        assertThat(headers.publicKey).isNotEmpty()
    }

    private fun createTestJwk(): String {
        val keyPair = KeyPairGenerator.getInstance("RSA").apply { initialize(1024) }.generateKeyPair()
        val privateKey = keyPair.private as RSAPrivateCrtKey
        val jwk = TestJwk(
            kty = "RSA",
            e = privateKey.publicExponent.toBase64Url(),
            n = privateKey.modulus.toBase64Url(),
            d = privateKey.privateExponent.toBase64Url(),
            p = privateKey.primeP.toBase64Url(),
            q = privateKey.primeQ.toBase64Url(),
            dp = privateKey.primeExponentP.toBase64Url(),
            dq = privateKey.primeExponentQ.toBase64Url(),
            qi = privateKey.crtCoefficient.toBase64Url(),
        )
        return Json.encodeToString(jwk)
    }
}

@kotlinx.serialization.Serializable
private data class TestJwk(
    val kty: String,
    val e: String,
    val n: String,
    val d: String,
    val p: String,
    val q: String,
    val dp: String,
    val dq: String,
    val qi: String,
)

private fun BigInteger.toBase64Url(): String = Base64.getUrlEncoder().withoutPadding().encodeToString(toByteArray())
