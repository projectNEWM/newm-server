package io.newm.server.features.cardano.model

import io.newm.server.typealiases.SongId
import io.newm.shared.serialization.UUIDSerializer
import kotlinx.serialization.Serializable

@Serializable
data class SubmitTransactionRequest(
    @Serializable(with = UUIDSerializer::class)
    val songId: SongId,
    val cborHex: String
)
