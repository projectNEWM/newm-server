package io.newm.server.features.cardano.model

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class CardanoNftSong(
    @Contextual
    val id: UUID,
    val fingerprint: String,
    val policyId: String,
    val assetName: String,
    val isStreamToken: Boolean,
    val amount: Long,
    val title: String,
    val imageUrl: String,
    val audioUrl: String,
    val duration: Long,
    val artists: List<String>,
    val genres: List<String>,
    val moods: List<String>
)
