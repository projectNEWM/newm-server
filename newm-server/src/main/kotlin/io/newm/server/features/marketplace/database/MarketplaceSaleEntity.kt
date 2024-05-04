package io.newm.server.features.marketplace.database

import io.newm.server.features.collaboration.database.CollaborationEntity
import io.newm.server.features.marketplace.model.Sale
import io.newm.server.features.marketplace.model.SaleFilters
import io.newm.server.features.marketplace.model.SaleStatus
import io.newm.server.features.marketplace.model.Token
import io.newm.server.features.song.database.ReleaseEntity
import io.newm.server.features.song.database.SongEntity
import io.newm.server.features.song.database.SongTable
import io.newm.server.features.user.database.UserEntity
import io.newm.server.features.user.database.UserTable
import io.newm.server.ktx.arweaveToWebUrl
import io.newm.shared.exposed.overlaps
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.AndOp
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SizedIterable
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greater
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.innerJoin
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.selectAll
import java.time.LocalDateTime
import java.util.UUID

class MarketplaceSaleEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    var createdAt: LocalDateTime by MarketplaceSaleTable.createdAt
    var status: SaleStatus by MarketplaceSaleTable.status
    var songId: EntityID<UUID> by MarketplaceSaleTable.songId
    var ownerAddress: String by MarketplaceSaleTable.ownerAddress
    var pointerPolicyId: String by MarketplaceSaleTable.pointerPolicyId
    var pointerAssetName: String by MarketplaceSaleTable.pointerAssetName
    var bundlePolicyId: String by MarketplaceSaleTable.bundlePolicyId
    var bundleAssetName: String by MarketplaceSaleTable.bundleAssetName
    var bundleAmount: Long by MarketplaceSaleTable.bundleAmount
    var costPolicyId: String by MarketplaceSaleTable.costPolicyId
    var costAssetName: String by MarketplaceSaleTable.costAssetName
    var costAmount: Long by MarketplaceSaleTable.costAmount
    var maxBundleSize: Long by MarketplaceSaleTable.maxBundleSize
    var totalBundleQuantity: Long by MarketplaceSaleTable.totalBundleQuantity
    var availableBundleQuantity: Long by MarketplaceSaleTable.availableBundleQuantity

    fun toModel(costAmountUsd: String): Sale {
        val song = SongEntity[songId]
        val artist = UserEntity[song.ownerId]
        val release = ReleaseEntity[song.releaseId!!]
        return Sale(
            id = id.value,
            createdAt = createdAt,
            status = status,
            bundlePolicyId = bundlePolicyId,
            bundleAssetName = bundleAssetName,
            costPolicyId = costPolicyId,
            costAssetName = costAssetName,
            costAmount = costAmount,
            costAmountUsd = costAmountUsd,
            maxBundleSize = maxBundleSize,
            totalBundleQuantity = totalBundleQuantity,
            bundleAmount = bundleAmount,
            availableBundleQuantity = availableBundleQuantity,
            song =
                Sale.Song(
                    id = songId.value,
                    artistId = artist.id.value,
                    artistName = artist.stageOrFullName,
                    artistPictureUrl = artist.pictureUrl,
                    title = song.title,
                    description = song.description,
                    parentalAdvisory = song.parentalAdvisory,
                    genres = song.genres.toList(),
                    moods = song.moods?.toList(),
                    coverArtUrl = release.arweaveCoverArtUrl?.arweaveToWebUrl(),
                    clipUrl = song.arweaveClipUrl?.arweaveToWebUrl(),
                    tokenAgreementUrl = song.arweaveTokenAgreementUrl?.arweaveToWebUrl(),
                    collaborators =
                        CollaborationEntity.findBySongId(songId.value).map {
                            Sale.SongCollaborator(
                                id = it.id.value,
                                name = UserEntity.getByEmail(it.email)?.stageOrFullName,
                                role = it.role
                            )
                        }
                )
        )
    }

    companion object : UUIDEntityClass<MarketplaceSaleEntity>(MarketplaceSaleTable) {
        fun getByPointer(pointer: Token): MarketplaceSaleEntity? = getByPointer(pointer.policyId, pointer.assetName)

        fun getByPointer(
            policyId: String,
            assetName: String
        ): MarketplaceSaleEntity? =
            MarketplaceSaleEntity.find {
                (MarketplaceSaleTable.pointerPolicyId eq policyId) and (MarketplaceSaleTable.pointerAssetName eq assetName)
            }.firstOrNull()

        fun all(filters: SaleFilters): SizedIterable<MarketplaceSaleEntity> {
            val ops = filters.toOps()
            return when {
                ops.isEmpty() -> all()
                filters.isIndependent -> find(AndOp(ops))
                else -> {
                    MarketplaceSaleEntity.wrapRows(
                        MarketplaceSaleTable.innerJoin(
                            otherTable = SongTable,
                            onColumn = { songId },
                            otherColumn = { id }
                        ).innerJoin(
                            otherTable = UserTable,
                            onColumn = { SongTable.ownerId },
                            otherColumn = { id }
                        ).selectAll().where(AndOp(ops))
                    )
                }
            }.orderBy(MarketplaceSaleTable.createdAt to (filters.sortOrder ?: SortOrder.ASC))
        }

        private fun SaleFilters.toOps(): List<Op<Boolean>> {
            val ops = mutableListOf<Op<Boolean>>()
            olderThan?.let {
                ops += MarketplaceSaleTable.createdAt less it
            }
            newerThan?.let {
                ops += MarketplaceSaleTable.createdAt greater it
            }
            ids?.let {
                ops += MarketplaceSaleTable.id inList it
            }
            songIds?.let {
                ops += MarketplaceSaleTable.songId inList it
            }
            artistIds?.let {
                ops += UserTable.id inList it
            }
            statuses?.let {
                ops += MarketplaceSaleTable.status inList it
            }
            genres?.let {
                ops += SongTable.genres overlaps it.toTypedArray()
            }
            moods?.let {
                ops += SongTable.moods overlaps it.toTypedArray()
            }
            phrase?.let {
                val pattern = "%${it.lowercase()}%"
                ops += (
                    (SongTable.title.lowerCase() like pattern)
                        or (UserTable.nickname.lowerCase() like pattern)
                        or (
                            (UserTable.nickname eq null) and (
                                (UserTable.firstName.lowerCase() like pattern)
                                    or (UserTable.lastName.lowerCase() like pattern)
                            )
                        )
                )
            }
            return ops
        }

        private val SaleFilters.isIndependent: Boolean
            get() = artistIds == null && genres == null && moods == null && phrase == null
    }
}
