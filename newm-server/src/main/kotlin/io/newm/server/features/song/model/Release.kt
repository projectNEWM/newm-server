package io.newm.server.features.song.model

import io.newm.server.typealiases.UserId
import io.newm.shared.serialization.LocalDateSerializer
import io.newm.shared.serialization.LocalDateTimeSerializer
import io.newm.shared.serialization.UUIDSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@Serializable
data class Release(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID? = null,
    val archived: Boolean? = null,
    @Serializable(with = UUIDSerializer::class)
    val ownerId: UserId? = null,
    @Serializable(with = LocalDateTimeSerializer::class)
    val createdAt: LocalDateTime? = null,
    val title: String? = null,
    val releaseType: ReleaseType? = null,
    val coverArtUrl: String? = null,
    val barcodeType: ReleaseBarcodeType? = null,
    val barcodeNumber: String? = null,
    @Serializable(with = LocalDateSerializer::class)
    val releaseDate: LocalDate? = null,
    @Serializable(with = LocalDateSerializer::class)
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
