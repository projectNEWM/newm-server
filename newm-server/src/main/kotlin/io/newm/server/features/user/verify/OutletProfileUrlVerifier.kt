package io.newm.server.features.user.verify

import java.lang.IllegalArgumentException

/**
 * Verifies that the outlet profile URL is valid and that the NEWM stageOrFullName matches the outlet profile.
 */
interface OutletProfileUrlVerifier {
    @Throws(IllegalArgumentException::class)
    suspend fun verify(outletProfileUrl: String, stageOrFullName: String)
}
