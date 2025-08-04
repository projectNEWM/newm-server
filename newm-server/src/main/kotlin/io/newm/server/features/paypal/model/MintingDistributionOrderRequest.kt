package io.newm.server.features.paypal.model

import io.newm.server.typealiases.SongId
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
data class MintingDistributionOrderRequest(
    @Contextual
    val songId: SongId
)
