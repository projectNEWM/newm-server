package io.newm.ardrive.turbo.auth

import io.newm.ardrive.turbo.auth.model.SignatureHeaders
import io.newm.ardrive.turbo.util.generateNonce
import io.newm.ardrive.turbo.util.toBase64Url
import java.math.BigInteger
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.Signature
import java.security.spec.MGF1ParameterSpec
import java.security.spec.PSSParameterSpec
import java.security.spec.RSAPrivateCrtKeySpec
import java.security.spec.RSAPublicKeySpec
import java.util.Base64
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class ArweaveSigner(
    walletJson: String,
) : TurboSigner {
    private val privateKey: PrivateKey
    private val publicKeySpec: RSAPublicKeySpec

    val publicKeyModulus: ByteArray
    override val publicKey: ByteArray
    private val address: String

    init {
        val jwk = Json.decodeFromString<Jwk>(walletJson)
        val modulus = jwk.n.toBigIntegerUrl()
        val publicExponent = jwk.e.toBigIntegerUrl()
        val privateExponent = jwk.d.toBigIntegerUrl()
        val primeP = jwk.p.toBigIntegerUrl()
        val primeQ = jwk.q.toBigIntegerUrl()
        val primeExponentP = jwk.dp.toBigIntegerUrl()
        val primeExponentQ = jwk.dq.toBigIntegerUrl()
        val crtCoefficient = jwk.qi.toBigIntegerUrl()

        publicKeySpec = RSAPublicKeySpec(modulus, publicExponent)
        val privateKeySpec = RSAPrivateCrtKeySpec(
            modulus,
            publicExponent,
            privateExponent,
            primeP,
            primeQ,
            primeExponentP,
            primeExponentQ,
            crtCoefficient,
        )

        val keyFactory = KeyFactory.getInstance("RSA")
        privateKey = keyFactory.generatePrivate(privateKeySpec)
        val publicKey = keyFactory.generatePublic(publicKeySpec)
        publicKeyModulus = normalizeUnsigned(publicKeySpec.modulus.toByteArray(), 512)
        this.publicKey = publicKeyModulus
        address = computeAddress(publicKeyModulus)
    }

    override suspend fun sign(payload: ByteArray): ByteArray {
        val signer = Signature.getInstance("RSASSA-PSS")
        signer.setParameter(
            PSSParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA256, 32, 1),
        )
        signer.initSign(privateKey)
        signer.update(payload)
        return signer.sign()
    }

    fun getAddress(): String = address

    override suspend fun generateSignatureHeaders(): SignatureHeaders {
        val nonce = generateNonce()
        val signature = sign(nonce.toByteArray(Charsets.UTF_8))
        val publicKeyEncoded = toBase64Url(publicKeyModulus)
        val signatureEncoded = toBase64Url(signature)
        return SignatureHeaders(
            publicKey = publicKeyEncoded,
            nonce = nonce,
            signature = signatureEncoded,
        )
    }
}

@Serializable
private data class Jwk(
    val kty: String,
    val e: String,
    val n: String,
    val d: String,
    val p: String,
    val q: String,
    val dp: String,
    val dq: String,
    val qi: String,
    val ext: Boolean? = null,
)

private fun String.toBigIntegerUrl(): BigInteger = BigInteger(1, Base64.getUrlDecoder().decode(this))

private fun computeAddress(publicKey: ByteArray): String {
    val digest = java.security.MessageDigest
        .getInstance("SHA-256")
        .digest(publicKey)
    return toBase64Url(digest)
}

private fun normalizeUnsigned(
    value: ByteArray,
    length: Int
): ByteArray {
    if (value.size == length) {
        return value
    }
    if (value.size == length + 1 && value.first() == 0.toByte()) {
        return value.copyOfRange(1, value.size)
    }
    if (value.size < length) {
        val padded = ByteArray(length)
        value.copyInto(padded, destinationOffset = length - value.size)
        return padded
    }
    throw IllegalArgumentException("Value is larger than expected length $length")
}
