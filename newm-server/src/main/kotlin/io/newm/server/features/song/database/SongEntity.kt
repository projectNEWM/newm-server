package io.newm.server.features.song.database

import io.newm.server.features.song.model.MarketplaceStatus
import io.newm.server.features.song.model.MintingStatus
import io.newm.server.features.song.model.Song
import io.newm.server.features.song.model.SongFilters
import io.newm.shared.exposed.overlaps
import io.newm.shared.exposed.unnest
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.AndOp
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SizedIterable
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greater
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import org.jetbrains.exposed.sql.count
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.mapLazy
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class SongEntity(id: EntityID<UUID>) : UUIDEntity(id) {
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
    var copyright: String? by SongTable.copyright
    var parentalAdvisory: String? by SongTable.parentalAdvisory
    var isrc: String? by SongTable.isrc
    var iswc: String? by SongTable.iswc
    var ipi: Array<String>? by SongTable.ipi
    var releaseDate: LocalDate? by SongTable.releaseDate
    var publicationDate: LocalDate? by SongTable.publicationDate
    var lyricsUrl: String? by SongTable.lyricsUrl
    var tokenAgreementUrl: String? by SongTable.tokenAgreementUrl
    var clipUrl: String? by SongTable.clipUrl
    var streamUrl: String? by SongTable.streamUrl
    var duration: Int? by SongTable.duration
    var nftPolicyId: String? by SongTable.nftPolicyId
    var nftName: String? by SongTable.nftName
    var mintingStatus: MintingStatus by SongTable.mintingStatus
    var marketplaceStatus: MarketplaceStatus by SongTable.marketplaceStatus
    var paymentKeyId: EntityID<UUID>? by SongTable.paymentKeyId
    var arweaveCoverArtUrl: String? by SongTable.arweaveCoverArtUrl
    var arweaveLyricsUrl: String? by SongTable.arweaveLyricsUrl
    var arweaveTokenAgreementUrl: String? by SongTable.arweaveTokenAgreementUrl
    var arweaveClipUrl: String? by SongTable.arweaveClipUrl

    fun toModel(): Song = Song(
        id = id.value,
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
        copyright = copyright,
        parentalAdvisory = parentalAdvisory,
        isrc = isrc,
        ipi = ipi?.toList(),
        releaseDate = releaseDate,
        publicationDate = publicationDate,
        lyricsUrl = lyricsUrl,
        tokenAgreementUrl = tokenAgreementUrl,
        clipUrl = clipUrl,
        streamUrl = streamUrl,
        duration = duration,
        nftPolicyId = nftPolicyId,
        nftName = nftName,
        mintingStatus = mintingStatus,
        marketplaceStatus = marketplaceStatus,
        paymentKeyId = paymentKeyId?.value,
        arweaveCoverArtUrl = arweaveCoverArtUrl,
        arweaveLyricsUrl = arweaveLyricsUrl,
        arweaveTokenAgreementUrl = arweaveTokenAgreementUrl,
        arweaveClipUrl = arweaveClipUrl
    )

    companion object : UUIDEntityClass<SongEntity>(SongTable) {
        fun all(filters: SongFilters): SizedIterable<SongEntity> {
            val ops = filters.toOps()
            return if (ops.isEmpty()) all() else find(AndOp(ops))
        }

        fun genres(filters: SongFilters): SizedIterable<String> {
            val ops = filters.toOps()
            val genre = SongTable.genres.unnest()
            val fields = SongTable.slice(SongTable.id.count(), genre)
            val query = if (ops.isEmpty()) fields.selectAll() else fields.select(AndOp(ops))
            return query.groupBy(genre)
                .orderBy(SongTable.id.count(), SortOrder.DESC)
                .mapLazy { it[genre] }
        }

        private fun SongFilters.toOps(): List<Op<Boolean>> {
            val ops = mutableListOf<Op<Boolean>>()
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
            phrase?.let {
                val pattern = "%${it.lowercase()}%"
                ops += (
                    (SongTable.title.lowerCase() like pattern)
                        or (SongTable.description.lowerCase() like pattern)
                        or (SongTable.album.lowerCase() like pattern)
                        or (SongTable.nftName.lowerCase() like pattern)
                    )
            }
            return ops
        }
    }
}
