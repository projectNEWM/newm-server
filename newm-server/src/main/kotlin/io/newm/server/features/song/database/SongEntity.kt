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
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greater
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
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
    var credits: String? by SongTable.credits
    var duration: Int? by SongTable.duration
    var streamUrl: String? by SongTable.streamUrl
    var nftPolicyId: String? by SongTable.nftPolicyId
    var nftName: String? by SongTable.nftName
    var mintingStatus: MintingStatus by SongTable.mintingStatus
    var marketplaceStatus: MarketplaceStatus by SongTable.marketplaceStatus

    fun toModel(): Song = Song(
        id = id.value,
        ownerId = ownerId.value,
        createdAt = createdAt,
        title = title,
        genres = genres.toList(),
        moods = moods?.toList(),
        coverArtUrl = coverArtUrl,
        description = description,
        credits = credits,
        duration = duration,
        streamUrl = streamUrl,
        nftPolicyId = nftPolicyId,
        nftName = nftName,
        mintingStatus = mintingStatus,
        marketplaceStatus = marketplaceStatus
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
            return ops
        }
    }
}
