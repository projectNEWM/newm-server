package io.newm.server.features.earnings.model

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.math.BigInteger

/**
 * Request to add royalties to a song. Either newmAmount or usdAmount must be provided, but not both.
 */
@Serializable
data class AddSongRoyaltyRequest(
    /**
     * Amount of NEWM to be spread out among the song's stream token holders. BigInteger assuming 6 decimals.
     */
    @Contextual
    val newmAmount: BigInteger? = null,
    /**
     * Amount of USD to be converted to equivalent NEWM amount and spread out among the song's stream token holders.
     * BigInteger assuming 6 decimals.
     */
    @Contextual
    val usdAmount: BigInteger? = null,
)
