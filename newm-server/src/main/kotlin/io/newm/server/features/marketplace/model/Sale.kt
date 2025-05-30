package io.newm.server.features.marketplace.model

import io.newm.server.features.song.model.SongSmartLink
import io.newm.server.typealiases.SaleId
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.time.LocalDateTime
import java.util.UUID

@Serializable
data class Sale(
    @Contextual
    val id: SaleId,
    @Contextual
    val createdAt: LocalDateTime,
    val status: SaleStatus,
    val pointerPolicyId: String,
    val pointerAssetName: String,
    val bundlePolicyId: String,
    val bundleAssetName: String,
    val bundleAmount: Long,
    val costPolicyId: String,
    val costAssetName: String,
    val costAmount: Long,
    val costAmountUsd: String,
    val costAmountNewm: String,
    val maxBundleSize: Long,
    val totalBundleQuantity: Long,
    val availableBundleQuantity: Long,
    val song: Song?
) {
    @Serializable
    data class Song(
        @Contextual
        val id: UUID,
        @Contextual
        val artistId: UUID,
        val artistName: String?,
        val artistPictureUrl: String?,
        val title: String?,
        val description: String?,
        val parentalAdvisory: String?,
        val genres: List<String>?,
        val moods: List<String>?,
        val coverArtUrl: String?,
        val clipUrl: String?,
        val tokenAgreementUrl: String?,
        val assetUrl: String?,
        val collaborators: List<SongCollaborator>?,
        val smartLinks: List<SongSmartLink>?
    )

    @Serializable
    data class SongCollaborator(
        @Contextual
        val id: UUID,
        val name: String?,
        val pictureUrl: String?,
        @Deprecated("Use 'roles' instead")
        val role: String?, // TODO: STUD-460 - remove "role" field after frontend migrates to use "roles"
        val roles: List<String>?
    )
}
