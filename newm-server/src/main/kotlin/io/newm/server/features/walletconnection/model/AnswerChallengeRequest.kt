package io.newm.server.features.walletconnection.model

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class AnswerChallengeRequest(
    @Contextual
    val challengeId: UUID,
    // data_signature or cbor<signed_transaction> (depending on GenerateChallengeRequest.method)
    val payload: String,
    // key only needed if method = SignedData
    val key: String? = null
)
