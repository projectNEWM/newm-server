package io.newm.server.features.marketplace.database

import io.newm.chain.util.assetFingerprintOf
import io.newm.chain.util.assetUrlOf
import io.newm.chain.util.extractStakeKeyHex
import io.newm.server.features.collaboration.database.CollaborationEntity
import io.newm.server.features.marketplace.model.CostAmountConversions
import io.newm.server.features.marketplace.model.Sale
import io.newm.server.features.marketplace.model.SaleFilters
import io.newm.server.features.marketplace.model.SaleStatus
import io.newm.server.features.marketplace.model.Token
import io.newm.server.features.minting.repo.MintingRepository
import io.newm.server.features.nftcdn.repo.NftCdnRepository
import io.newm.server.features.song.database.ReleaseEntity
import io.newm.server.features.song.database.SongEntity
import io.newm.server.features.song.database.SongTable
import io.newm.server.features.song.model.SongSmartLink
import io.newm.server.features.user.database.UserEntity
import io.newm.server.features.user.database.UserTable
import io.newm.server.ktx.arweaveToWebUrl
import io.newm.server.typealiases.SaleId
import io.newm.shared.exposed.notOverlaps
import io.newm.shared.exposed.overlaps
import io.newm.shared.koin.inject
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
import org.jetbrains.exposed.sql.SqlExpressionBuilder.notInList
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.innerJoin
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.mapLazy
import org.jetbrains.exposed.sql.or
import java.time.LocalDateTime
import java.util.UUID

private val nftCdnRepository: NftCdnRepository by inject()
private val mintingRepository: MintingRepository by inject()

class MarketplaceSaleEntity(
    id: EntityID<SaleId>
) : UUIDEntity(id) {
    var createdAt: LocalDateTime by MarketplaceSaleTable.createdAt
    var status: SaleStatus by MarketplaceSaleTable.status
    var songId: EntityID<UUID> by MarketplaceSaleTable.songId
    var ownerAddress: String by MarketplaceSaleTable.ownerAddress
    var ownerAddressStakeKey: String? by MarketplaceSaleTable.ownerAddressStakeKey
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

    fun toModel(
        isMainnet: Boolean,
        isNftCdnEnabled: Boolean,
        costAmountConversions: CostAmountConversions,
        smartLinks: List<SongSmartLink>
    ): Sale {
        val song = SongEntity[songId]
        val artist = UserEntity[song.ownerId]
        val release = ReleaseEntity[song.releaseId!!]
        val coverArtUrl: String
        val clipUrl: String
        val tokenAgreementUrl: String
        if (isNftCdnEnabled) {
            val fingerprint = assetFingerprintOf(song.nftPolicyId!!, song.nftName!!)
            coverArtUrl = nftCdnRepository.generateImageUrl(fingerprint)
            clipUrl = nftCdnRepository.generateFileUrl(
                fingerprint = fingerprint,
                index = mintingRepository.getAudioClipFileIndex(song.nftPolicyId!!)
            )
            tokenAgreementUrl = nftCdnRepository.generateFileUrl(
                fingerprint = fingerprint,
                index = mintingRepository.getTokenAgreementFileIndex(song.nftPolicyId!!)
            )
        } else {
            coverArtUrl = release.arweaveCoverArtUrl!!.arweaveToWebUrl()
            clipUrl = song.arweaveClipUrl!!.arweaveToWebUrl()
            tokenAgreementUrl = song.arweaveTokenAgreementUrl!!.arweaveToWebUrl()
        }
        return Sale(
            id = id.value,
            createdAt = createdAt,
            status = status,
            pointerPolicyId = pointerPolicyId,
            pointerAssetName = pointerAssetName,
            bundlePolicyId = bundlePolicyId,
            bundleAssetName = bundleAssetName,
            costPolicyId = costPolicyId,
            costAssetName = costAssetName,
            costAmount = costAmount,
            costAmountUsd = costAmountConversions.usd,
            costAmountNewm = costAmountConversions.newm,
            maxBundleSize = maxBundleSize,
            totalBundleQuantity = totalBundleQuantity,
            bundleAmount = bundleAmount,
            availableBundleQuantity = availableBundleQuantity,
            song = Sale.Song(
                id = songId.value,
                artistId = artist.id.value,
                artistName = artist.stageOrFullName,
                artistPictureUrl = artist.pictureUrl,
                title = song.title,
                description = song.description,
                parentalAdvisory = song.parentalAdvisory,
                genres = song.genres.toList(),
                moods = song.moods?.toList(),
                coverArtUrl = coverArtUrl,
                clipUrl = clipUrl,
                tokenAgreementUrl = tokenAgreementUrl,
                assetUrl = assetUrlOf(isMainnet, song.nftPolicyId!!, song.nftName!!),
                collaborators = CollaborationEntity
                    .findBySongId(songId.value)
                    .mapNotNull { collab ->
                        UserEntity.getByEmail(collab.email)?.run {
                            Sale.SongCollaborator(
                                id = id.value,
                                name = stageOrFullName,
                                pictureUrl = pictureUrl,
                                roles = collab.roles,
                                role = collab.roles.firstOrNull()
                            )
                        }
                    }.sortedBy { it.name },
                smartLinks = smartLinks
            )
        )
    }

    companion object : UUIDEntityClass<MarketplaceSaleEntity>(MarketplaceSaleTable) {
        fun getByPointer(pointer: Token): MarketplaceSaleEntity? = getByPointer(pointer.policyId, pointer.assetName)

        fun getByPointer(
            policyId: String,
            assetName: String
        ): MarketplaceSaleEntity? =
            MarketplaceSaleEntity
                .find {
                    (MarketplaceSaleTable.pointerPolicyId eq policyId) and (MarketplaceSaleTable.pointerAssetName eq assetName)
                }.firstOrNull()

        fun all(filters: SaleFilters): SizedIterable<MarketplaceSaleEntity> {
            val ops = filters.toOps()
            return when {
                ops.isEmpty() -> all()
                filters.isIndependent -> find(AndOp(ops))
                else -> {
                    MarketplaceSaleTable
                        .innerJoin(
                            otherTable = SongTable,
                            onColumn = { songId },
                            otherColumn = { id }
                        ).innerJoin(
                            otherTable = UserTable,
                            onColumn = { SongTable.ownerId },
                            otherColumn = { id }
                        ).select(MarketplaceSaleTable.columns)
                        .where(AndOp(ops))
                        .mapLazy(MarketplaceSaleEntity::wrapRow)
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
            ids?.includes?.let {
                ops += MarketplaceSaleTable.id inList it
            }
            ids?.excludes?.let {
                ops += MarketplaceSaleTable.id notInList it
            }
            songIds?.includes?.let {
                ops += MarketplaceSaleTable.songId inList it
            }
            songIds?.excludes?.let {
                ops += MarketplaceSaleTable.songId notInList it
            }
            artistIds?.includes?.let {
                ops += UserTable.id inList it
            }
            artistIds?.excludes?.let {
                ops += UserTable.id notInList it
            }
            addresses?.includes?.mapNotNull(String::extractStakeKeyHex)?.let {
                ops += MarketplaceSaleTable.ownerAddressStakeKey inList it
            }
            addresses?.excludes?.mapNotNull(String::extractStakeKeyHex)?.let {
                ops += MarketplaceSaleTable.ownerAddressStakeKey notInList it
            }
            statuses?.includes?.let {
                ops += MarketplaceSaleTable.status inList it
            }
            statuses?.excludes?.let {
                ops += MarketplaceSaleTable.status notInList it
            }
            genres?.includes?.let {
                ops += SongTable.genres overlaps it
            }
            genres?.excludes?.let {
                ops += SongTable.genres notOverlaps it
            }
            moods?.includes?.let {
                ops += SongTable.moods overlaps it
            }
            moods?.excludes?.let {
                ops += SongTable.moods notOverlaps it
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
