package io.newm.server.features.cardano.model

import io.newm.shared.serialization.UUIDSerializer
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class SubmitTransactionRequest(
    @Serializable(with = UUIDSerializer::class)
    val songId: UUID,
    val cborHex: String
)
