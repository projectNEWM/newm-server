package io.newm.ardrive.turbo.auth.model

data class SignatureHeaders(
    val publicKey: String,
    val nonce: String,
    val signature: String,
) {
    fun asHeaderMap(): Map<String, String> =
        mapOf(
            "x-public-key" to publicKey,
            "x-nonce" to nonce,
            "x-signature" to signature,
        )
}
