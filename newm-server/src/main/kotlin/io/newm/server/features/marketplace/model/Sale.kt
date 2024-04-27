package io.newm.server.features.marketplace.model

import io.newm.shared.serialization.LocalDateTimeSerializer
import io.newm.shared.serialization.UUIDSerializer
import kotlinx.serialization.Serializable
import java.time.LocalDateTime
import java.util.UUID

@Serializable
data class Sale(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    @Serializable(with = LocalDateTimeSerializer::class)
    val createdAt: LocalDateTime,
    val status: SaleStatus,
    val bundlePolicyId: String,
    val bundleAssetName: String,
    val bundleAmount: Long,
    val costPolicyId: String,
    val costAssetName: String,
    val costAmount: Long,
    val maxBundleSize: Long,
    val totalBundleQuantity: Long,
    val availableBundleQuantity: Long,
    val song: Song?
) {
    @Serializable
    data class Song(
        @Serializable(with = UUIDSerializer::class)
        val id: UUID,
        @Serializable(with = UUIDSerializer::class)
        val artistId: UUID,
        val artistName: String?,
        val title: String?,
        val genres: List<String>?,
        val moods: List<String>?,
        val coverArtUrl: String?,
        val clipUrl: String?,
        val tokenAgreementUrl: String?,
        val collaborators: List<SongCollaborator>?,
    )

    @Serializable
    data class SongCollaborator(
        @Serializable(with = UUIDSerializer::class)
        val id: UUID,
        val name: String?,
        val role: String?,
    )
}
