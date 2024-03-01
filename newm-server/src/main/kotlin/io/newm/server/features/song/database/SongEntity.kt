package io.newm.server.features.song.database

import io.newm.server.features.song.model.AudioEncodingStatus
import io.newm.server.features.song.model.MarketplaceStatus
import io.newm.server.features.song.model.MintingStatus
import io.newm.server.features.song.model.Song
import io.newm.server.features.song.model.SongBarcodeType
import io.newm.server.features.song.model.SongFilters
import io.newm.server.features.user.database.UserTable
import io.newm.shared.exposed.overlaps
import io.newm.shared.exposed.unnest
import io.newm.shared.ktx.exists
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
import org.jetbrains.exposed.sql.count
import org.jetbrains.exposed.sql.innerJoin
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.mapLazy
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.selectAll
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class SongEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    var archived: Boolean by SongTable.archived
    val createdAt: LocalDateTime by SongTable.createdAt
    var ownerId: EntityID<UUID> by SongTable.ownerId
    var title: String by SongTable.title
    var genres: Array<String> by SongTable.genres
    var moods: Array<String>? by SongTable.moods
    var coverArtUrl: String? by SongTable.coverArtUrl
    var description: String? by SongTable.description
    var album: String? by SongTable.album
    var track: Int? by SongTable.track
    var language: String? by SongTable.language
    var coverRemixSample: Boolean by SongTable.coverRemixSample
    var compositionCopyrightOwner: String? by SongTable.compositionCopyrightOwner
    var compositionCopyrightYear: Int? by SongTable.compositionCopyrightYear
    var phonographicCopyrightOwner: String? by SongTable.phonographicCopyrightOwner
    var phonographicCopyrightYear: Int? by SongTable.phonographicCopyrightYear
    var parentalAdvisory: String? by SongTable.parentalAdvisory
    var barcodeType: SongBarcodeType? by SongTable.barcodeType
    var barcodeNumber: String? by SongTable.barcodeNumber
    var isrc: String? by SongTable.isrc
    var iswc: String? by SongTable.iswc
    var ipis: Array<String>? by SongTable.ipis
    var releaseDate: LocalDate? by SongTable.releaseDate
    var publicationDate: LocalDate? by SongTable.publicationDate
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
    var arweaveCoverArtUrl: String? by SongTable.arweaveCoverArtUrl
    var arweaveLyricsUrl: String? by SongTable.arweaveLyricsUrl
    var arweaveTokenAgreementUrl: String? by SongTable.arweaveTokenAgreementUrl
    var arweaveClipUrl: String? by SongTable.arweaveClipUrl
    var distributionTrackId: Long? by SongTable.distributionTrackId
    var distributionReleaseId: Long? by SongTable.distributionReleaseId
    var mintCostLovelace: Long? by SongTable.mintCostLovelace
    var forceDistributed: Boolean? by SongTable.forceDistributed
    var errorMessage: String? by SongTable.errorMessage
    var instrumental: Boolean by SongTable.instrumental

    fun toModel(): Song =
        Song(
            id = id.value,
            archived = archived,
            ownerId = ownerId.value,
            createdAt = createdAt,
            title = title,
            genres = genres.toList(),
            moods = moods?.toList(),
            coverArtUrl = coverArtUrl,
            description = description,
            album = album,
            track = track,
            language = language,
            coverRemixSample = coverRemixSample,
            compositionCopyrightOwner = compositionCopyrightOwner,
            compositionCopyrightYear = compositionCopyrightYear,
            phonographicCopyrightOwner = phonographicCopyrightOwner,
            phonographicCopyrightYear = phonographicCopyrightYear,
            parentalAdvisory = parentalAdvisory,
            barcodeType = barcodeType,
            barcodeNumber = barcodeNumber,
            isrc = isrc,
            iswc = iswc,
            ipis = ipis?.toList(),
            releaseDate = releaseDate,
            publicationDate = publicationDate,
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
            arweaveCoverArtUrl = arweaveCoverArtUrl,
            arweaveLyricsUrl = arweaveLyricsUrl,
            arweaveTokenAgreementUrl = arweaveTokenAgreementUrl,
            arweaveClipUrl = arweaveClipUrl,
            distributionTrackId = distributionTrackId,
            distributionReleaseId = distributionReleaseId,
            mintCostLovelace = mintCostLovelace,
            forceDistributed = forceDistributed,
            errorMessage = errorMessage,
            instrumental = instrumental
        )

    companion object : UUIDEntityClass<SongEntity>(SongTable) {
        fun all(filters: SongFilters): SizedIterable<SongEntity> {
            val ops = filters.toOps()
            return when {
                ops.isEmpty() -> all()
                filters.phrase == null -> find(AndOp(ops))
                else ->
                    SongEntity.wrapRows(
                        SongTable.innerJoin(
                            otherTable = UserTable,
                            onColumn = { ownerId },
                            otherColumn = { id }
                        ).selectAll().where(AndOp(ops))
                    )
            }.orderBy(SongTable.createdAt to (filters.sortOrder ?: SortOrder.ASC))
        }

        fun genres(filters: SongFilters): SizedIterable<String> {
            val ops = filters.toOps()
            val genre = SongTable.genres.unnest()
            val table =
                if (filters.phrase == null) {
                    SongTable
                } else {
                    SongTable.innerJoin(
                        otherTable = UserTable,
                        onColumn = { ownerId },
                        otherColumn = { id }
                    )
                }
            val fields = table.select(SongTable.id.count(), genre)
            val query = if (ops.isEmpty()) fields else fields.where(AndOp(ops))
            return query.groupBy(genre)
                .orderBy(SongTable.id.count(), filters.sortOrder ?: SortOrder.DESC)
                .mapLazy { it[genre] }
        }

        fun exists(
            ownerId: UUID,
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
            ids?.let {
                ops += SongTable.id inList it
            }
            ownerIds?.let {
                ops += SongTable.ownerId inList it
            }
            genres?.let {
                ops += SongTable.genres overlaps it.toTypedArray()
            }
            moods?.let {
                ops += SongTable.moods overlaps it.toTypedArray()
            }
            mintingStatuses?.let {
                ops += SongTable.mintingStatus inList it
            }
            phrase?.let {
                val pattern = "%${it.lowercase()}%"
                ops += (
                    (SongTable.title.lowerCase() like pattern)
                        or (SongTable.description.lowerCase() like pattern)
                        or (SongTable.album.lowerCase() like pattern)
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
            nftNames?.let {
                ops += SongTable.nftName inList it
            }
            return ops
        }
    }
}
