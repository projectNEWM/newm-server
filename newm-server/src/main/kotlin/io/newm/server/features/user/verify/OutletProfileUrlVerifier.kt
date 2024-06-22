package io.newm.server.features.user.verify

/**
 * Verifies that the outlet profile URL is valid and that the NEWM stageOrFullName matches the outlet profile.
 */
interface OutletProfileUrlVerifier {
    @Throws(OutletProfileUrlVerificationException::class)
    suspend fun verify(
        outletProfileUrl: String,
        stageOrFullName: String
    )
}

class OutletProfileUrlVerificationException(
    message: String
) : Exception(message)
