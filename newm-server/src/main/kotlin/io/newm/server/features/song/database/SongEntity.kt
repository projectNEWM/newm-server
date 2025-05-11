package io.newm.server.features.song.database

import io.newm.server.features.song.model.AudioEncodingStatus
import io.newm.server.features.song.model.MarketplaceStatus
import io.newm.server.features.song.model.MintingStatus
import io.newm.server.features.song.model.Release
import io.newm.server.features.song.model.Song
import io.newm.server.features.song.model.SongFilters
import io.newm.server.features.user.database.UserTable
import io.newm.server.typealiases.ReleaseId
import io.newm.server.typealiases.SongId
import io.newm.server.typealiases.UserId
import io.newm.shared.exposed.notOverlaps
import io.newm.shared.exposed.overlaps
import io.newm.shared.ktx.exists
import java.time.LocalDateTime
import java.util.UUID
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.AndOp
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SizedCollection
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
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.selectAll

class SongEntity(
    id: EntityID<SongId>
) : UUIDEntity(id) {
    var archived: Boolean by SongTable.archived
    val createdAt: LocalDateTime by SongTable.createdAt
    var ownerId: EntityID<UserId> by SongTable.ownerId
    var title: String by SongTable.title
    var genres: List<String> by SongTable.genres
    var moods: List<String>? by SongTable.moods
    var description: String? by SongTable.description
    var releaseId: EntityID<ReleaseId>? by SongTable.releaseId
    var track: Int? by SongTable.track
    var language: String? by SongTable.language
    var coverRemixSample: Boolean by SongTable.coverRemixSample
    var compositionCopyrightOwner: String? by SongTable.compositionCopyrightOwner
    var compositionCopyrightYear: Int? by SongTable.compositionCopyrightYear
    var phonographicCopyrightOwner: String? by SongTable.phonographicCopyrightOwner
    var phonographicCopyrightYear: Int? by SongTable.phonographicCopyrightYear
    var parentalAdvisory: String? by SongTable.parentalAdvisory
    var isrc: String? by SongTable.isrc
    var iswc: String? by SongTable.iswc
    var ipis: List<String>? by SongTable.ipis
    var lyricsUrl: String? by SongTable.lyricsUrl
    var tokenAgreementUrl: String? by SongTable.tokenAgreementUrl
    var originalAudioUrl: String? by SongTable.originalAudioUrl
    var clipUrl: String? by SongTable.clipUrl
    var streamUrl: String? by SongTable.streamUrl
    var duration: Int? by SongTable.duration
    var nftPolicyId: String? by SongTable.nftPolicyId
    var nftName: String? by SongTable.nftName
    var audioEncodingStatus: AudioEncodingStatus by SongTable.audioEncodingStatus
    var mintingStatus: MintingStatus by SongTable.mintingStatus
    var mintingTxId: String? by SongTable.mintingTxId
    var marketplaceStatus: MarketplaceStatus by SongTable.marketplaceStatus
    var paymentKeyId: EntityID<UUID>? by SongTable.paymentKeyId
    var arweaveLyricsUrl: String? by SongTable.arweaveLyricsUrl
    var arweaveTokenAgreementUrl: String? by SongTable.arweaveTokenAgreementUrl
    var arweaveClipUrl: String? by SongTable.arweaveClipUrl
    var distributionTrackId: Long? by SongTable.distributionTrackId
    var instrumental: Boolean by SongTable.instrumental

    fun toModel(release: Release): Song =
        Song(
            id = id.value,
            archived = archived,
            ownerId = ownerId.value,
            createdAt = createdAt,
            title = title,
            genres = genres,
            moods = moods,
            coverArtUrl = release.coverArtUrl,
            description = description,
            releaseId = releaseId?.value,
            track = track,
            language = language,
            coverRemixSample = coverRemixSample,
            compositionCopyrightOwner = compositionCopyrightOwner,
            compositionCopyrightYear = compositionCopyrightYear,
            phonographicCopyrightOwner = phonographicCopyrightOwner,
            phonographicCopyrightYear = phonographicCopyrightYear,
            parentalAdvisory = parentalAdvisory,
            barcodeType = release.barcodeType,
            barcodeNumber = release.barcodeNumber,
            isrc = isrc,
            iswc = iswc,
            ipis = ipis,
            releaseDate = release.releaseDate,
            publicationDate = release.publicationDate,
            lyricsUrl = lyricsUrl,
            tokenAgreementUrl = tokenAgreementUrl,
            originalAudioUrl = originalAudioUrl,
            clipUrl = clipUrl,
            streamUrl = streamUrl,
            duration = duration,
            nftPolicyId = nftPolicyId,
            nftName = nftName,
            audioEncodingStatus = audioEncodingStatus,
            mintingStatus = mintingStatus,
            mintingTxId = mintingTxId,
            marketplaceStatus = marketplaceStatus,
            paymentKeyId = paymentKeyId?.value,
            arweaveLyricsUrl = arweaveLyricsUrl,
            arweaveTokenAgreementUrl = arweaveTokenAgreementUrl,
            arweaveClipUrl = arweaveClipUrl,
            distributionTrackId = distributionTrackId,
            mintCostLovelace = release.mintCostLovelace,
            mintPaymentType = release.mintPaymentType,
            mintCost = release.mintCost,
            forceDistributed = release.forceDistributed,
            errorMessage = release.errorMessage,
            instrumental = instrumental,
        )

    companion object : UUIDEntityClass<SongEntity>(SongTable) {
        fun all(filters: SongFilters): SizedIterable<SongEntity> {
            val ops = filters.toOps()
            return when {
                ops.isEmpty() -> all()
                filters.phrase == null -> find(AndOp(ops))
                else ->
                    SongEntity.wrapRows(
                        SongTable
                            .innerJoin(
                                otherTable = UserTable,
                                onColumn = { ownerId },
                                otherColumn = { id }
                            ).selectAll()
                            .where(AndOp(ops))
                    )
            }.orderBy(SongTable.createdAt to (filters.sortOrder ?: SortOrder.ASC))
        }

        fun genres(filters: SongFilters): SizedIterable<String> {
            val ops = filters.toOps()
            val table = if (filters.phrase == null) {
                SongTable
            } else {
                SongTable.innerJoin(
                    otherTable = UserTable,
                    onColumn = { ownerId },
                    otherColumn = { id }
                )
            }
            val fields = table.select(SongTable.genres)
            val query = if (ops.isEmpty()) fields else fields.where(AndOp(ops))
            val sortOrder = filters.sortOrder ?: SortOrder.DESC
            return SizedCollection(
                query
                    .flatMap { it[SongTable.genres] }
                    .groupingBy { it }
                    .eachCount()
                    .entries
                    .sortedWith { (_, v1), (_, v2) ->
                        if (sortOrder == SortOrder.ASC) {
                            v1.compareTo(v2)
                        } else {
                            v2.compareTo(v1)
                        }
                    }.map { it.key }
            )
        }

        fun exists(
            ownerId: UserId,
            title: String
        ): Boolean =
            exists {
                (SongTable.archived eq false) and
                    (SongTable.ownerId eq ownerId) and
                    (SongTable.title.lowerCase() eq title.lowercase())
            }

        private fun SongFilters.toOps(): List<Op<Boolean>> {
            val ops = mutableListOf<Op<Boolean>>()
            ops += SongTable.archived eq (archived ?: false)
            olderThan?.let {
                ops += SongTable.createdAt less it
            }
            newerThan?.let {
                ops += SongTable.createdAt greater it
            }
            ids?.includes?.let {
                ops += SongTable.id inList it
            }
            ids?.excludes?.let {
                ops += SongTable.id notInList it
            }
            ownerIds?.includes?.let {
                ops += SongTable.ownerId inList it
            }
            ownerIds?.excludes?.let {
                ops += SongTable.ownerId notInList it
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
            mintingStatuses?.includes?.let {
                ops += SongTable.mintingStatus inList it
            }
            mintingStatuses?.excludes?.let {
                ops += SongTable.mintingStatus notInList it
            }
            nftNames?.includes?.let {
                ops += SongTable.nftName inList it
            }
            nftNames?.excludes?.let {
                ops += SongTable.nftName notInList it
            }
            phrase?.let {
                val pattern = "%${it.lowercase()}%"
                ops += (
                    (SongTable.title.lowerCase() like pattern)
                        or (SongTable.description.lowerCase() like pattern)
                        or (SongTable.nftName.lowerCase() like pattern)
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
    }
}
