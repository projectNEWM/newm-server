package io.newm.server.features.nftsong.model

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class NftSong(
    @Contextual
    val id: UUID,
    val title: String,
    val imageUrl: String,
    val audioUrl: String,
    val duration: Long,
    val artists: List<String>,
    val genres: List<String>,
    val moods: List<String>,
    val amount: Long,
    val allocations: List<NftWalletAllocation>,
    val chainMetadata: NftChainMetadata
)
