package io.newm.server.features.song.model

import io.newm.server.typealiases.UserId
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Serializable
data class Release(
    @Contextual
    val id: UUID? = null,
    val archived: Boolean? = null,
    @Contextual
    val ownerId: UserId? = null,
    @Contextual
    val createdAt: LocalDateTime? = null,
    val title: String? = null,
    val releaseType: ReleaseType? = null,
    val coverArtUrl: String? = null,
    val barcodeType: ReleaseBarcodeType? = null,
    val barcodeNumber: String? = null,
    @Contextual
    val releaseDate: LocalDate? = null,
    @Contextual
    val publicationDate: LocalDate? = null,
    @Transient
    val arweaveCoverArtUrl: String? = null,
    @Transient
    val distributionReleaseId: Long? = null,
    val hasSubmittedForDistribution: Boolean? = null,
    @Transient
    val forceDistributed: Boolean? = null,
    val errorMessage: String? = null,
    val preSavePage: String? = null,
    val mintCostLovelace: Long? = null,
) {
    @Transient
    val releaseProductCodeType: String =
        if (barcodeNumber == null || barcodeType == ReleaseBarcodeType.Ean) {
            "ean"
        } else if (barcodeType == ReleaseBarcodeType.Upc) {
            "upc"
        } else {
            "jan"
        }
}
