package io.newm.server.features.walletconnection.model

import io.newm.shared.serialization.UUIDSerializer
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class AnswerChallengeRequest(
    @Serializable(with = UUIDSerializer::class)
    val challengeId: UUID,
    // data_signature or cbor<transaction_witness_set> (depending on GenerateChallengeRequest.method)
    val payload: String
)
