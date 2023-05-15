package io.newm.server.features.song.model

import io.newm.shared.serialization.LocalDateSerializer
import io.newm.shared.serialization.LocalDateTimeSerializer
import io.newm.shared.serialization.UUIDSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.time.LocalDate
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
    val moods: List<String>? = null,
    val coverArtUrl: String? = null,
    val description: String? = null,
    val album: String? = null,
    val track: Int? = null,
    val language: String? = null,
    val copyright: String? = null,
    val parentalAdvisory: String? = null,
    val isrc: String? = null,
    val iswc: String? = null,
    val ipi: List<String>? = null,
    @Serializable(with = LocalDateSerializer::class)
    val releaseDate: LocalDate? = null,
    @Serializable(with = LocalDateSerializer::class)
    val publicationDate: LocalDate? = null,
    val lyricsUrl: String? = null,
    val tokenAgreementUrl: String? = null,
    val originalAudioUrl: String? = null,
    val clipUrl: String? = null,
    val streamUrl: String? = null,
    val duration: Int? = null,
    val nftPolicyId: String? = null,
    val nftName: String? = null,
    val mintingStatus: MintingStatus? = null,
    val marketplaceStatus: MarketplaceStatus? = null,
    @Serializable(with = UUIDSerializer::class)
    val paymentKeyId: UUID? = null,
    @Transient
    val arweaveCoverArtUrl: String? = null,
    @Transient
    val arweaveLyricsUrl: String? = null,
    @Transient
    val arweaveTokenAgreementUrl: String? = null,
    @Transient
    val arweaveClipUrl: String? = null,
)
