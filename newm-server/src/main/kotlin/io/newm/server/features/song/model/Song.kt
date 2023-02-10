package io.newm.server.features.song.model

import io.newm.server.serialization.LocalDateTimeSerializer
import io.newm.server.serialization.UUIDSerializer
import kotlinx.serialization.Serializable
import java.time.LocalDateTime
import java.util.UUID

@Serializable
data class Song(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID? = null,
    @Serializable(with = UUIDSerializer::class)
    val ownerId: UUID? = null,
    @Serializable(with = LocalDateTimeSerializer::class)
    val createdAt: LocalDateTime? = null,
    val title: String? = null,
    val genres: List<String>? = null,
    val genre: String? = null, // TODO: remove genre (CU-8669gyp2a)
    val coverArtUrl: String? = null,
    val description: String? = null,
    val credits: String? = null,
    val duration: Int? = null,
    val streamUrl: String? = null,
    val nftPolicyId: String? = null,
    val nftName: String? = null,
    val mintingStatus: MintingStatus? = null,
    val marketplaceStatus: MarketplaceStatus? = null
)
