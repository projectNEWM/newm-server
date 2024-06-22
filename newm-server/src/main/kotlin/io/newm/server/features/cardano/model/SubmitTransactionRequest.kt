package io.newm.server.features.cardano.model

import io.newm.server.typealiases.SongId
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
data class SubmitTransactionRequest(
    @Contextual
    val songId: SongId,
    val cborHex: String
)
