package io.newm.server.features.nftsong.model

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class NftWalletAllocation(
    @Contextual
    val id: UUID,
    val amount: Long
)
