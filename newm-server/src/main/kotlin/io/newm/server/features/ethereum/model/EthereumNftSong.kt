package io.newm.server.features.ethereum.model

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class EthereumNftSong(
    @Contextual
    val id: UUID,
    val contractAddress: String,
    val tokenType: String,
    val tokenId: String,
    val amount: Long,
    val allocations: Map<@Contextual UUID, Long>,
    val title: String,
    val imageUrl: String,
    val audioUrl: String,
    val duration: Long,
    val artists: List<String>,
    val genres: List<String>,
    val moods: List<String>
)
