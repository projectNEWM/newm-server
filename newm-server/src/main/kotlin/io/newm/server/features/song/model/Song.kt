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
    val compositionCopyrightOwner: String? = null,
    val compositionCopyrightYear: Int? = null,
    val phonographicCopyrightOwner: String? = null,
    val phonographicCopyrightYear: Int? = null,
    val parentalAdvisory: String? = null,
    val barcodeType: SongBarcodeType? = null,
    val barcodeNumber: String? = null,
    val isrc: String? = null,
    val iswc: String? = null,
    val ipis: List<String>? = null,
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
    val audioEncodingStatus: AudioEncodingStatus? = null,
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
    @Transient
    val distributionTrackId: Long? = null,
    @Transient
    val distributionReleaseId: Long? = null,
    @Transient
    val mintCostLovelace: Long? = null,
    @Transient
    val forceDistributed: Boolean? = null,
)
