package io.newm.server.features.song.model

import io.newm.server.typealiases.ReleaseId
import io.newm.server.typealiases.SongId
import io.newm.server.typealiases.UserId
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Serializable
data class Song(
    @Contextual
    val id: SongId? = null,
    val archived: Boolean? = null,
    @Contextual
    val ownerId: UserId? = null,
    @Contextual
    val createdAt: LocalDateTime? = null,
    val title: String? = null,
    val genres: List<String>? = null,
    val moods: List<String>? = null,
    // FIXME: Keep coverArtUrl for now since the UI/UX expects it. Eventually this will only be on the Release instead of the Song.
    val coverArtUrl: String? = null,
    val description: String? = null,
    @Contextual
    val releaseId: ReleaseId? = null,
    val track: Int? = null,
    val language: String? = null,
    val coverRemixSample: Boolean? = null,
    val compositionCopyrightOwner: String? = null,
    val compositionCopyrightYear: Int? = null,
    val phonographicCopyrightOwner: String? = null,
    val phonographicCopyrightYear: Int? = null,
    val parentalAdvisory: String? = null,
    // FIXME: Keep barcodeType for now since the UI/UX expects it. Eventually this will only be on the Release instead of the Song.
    val barcodeType: ReleaseBarcodeType? = null,
    // FIXME: Keep barcodeNumber for now since the UI/UX expects it. Eventually this will only be on the Release instead of the Song.
    val barcodeNumber: String? = null,
    val isrc: String? = null,
    val iswc: String? = null,
    val ipis: List<String>? = null,
    // FIXME: Keep releaseDate for now since the UI/UX expects it. Eventually this will only be on the Release instead of the Song.
    @Contextual
    val releaseDate: LocalDate? = null,
    // FIXME: Keep publicationDate for now since the UI/UX expects it. Eventually this will only be on the Release instead of the Song.
    @Contextual
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
    val mintingTxId: String? = null,
    val marketplaceStatus: MarketplaceStatus? = null,
    @Contextual
    val paymentKeyId: UUID? = null,
    @Transient
    val arweaveLyricsUrl: String? = null,
    @Transient
    val arweaveTokenAgreementUrl: String? = null,
    @Transient
    val arweaveClipUrl: String? = null,
    @Transient
    val distributionTrackId: Long? = null,
    @Transient
    val mintCostLovelace: Long? = null,
    @Transient
    val forceDistributed: Boolean? = null,
    // FIXME: Keep errorMessage for now since the UI/UX expects it. Eventually this will only be on the Release instead of the Song.
    val errorMessage: String? = null,
    val instrumental: Boolean? = null,
)
