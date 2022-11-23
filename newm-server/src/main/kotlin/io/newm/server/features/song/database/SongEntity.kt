package io.newm.server.features.song.database

import io.newm.server.features.song.model.MarketplaceStatus
import io.newm.server.features.song.model.MintingStatus
import io.newm.server.features.song.model.Song
import io.newm.server.features.song.model.SongFilters
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
    var genre: String by SongTable.genre
    var coverArtUrl: String? by SongTable.coverArtUrl
    var description: String? by SongTable.description
    var credits: String? by SongTable.credits
    var streamUrl: String? by SongTable.streamUrl
    var nftPolicyId: String? by SongTable.nftPolicyId
    var nftName: String? by SongTable.nftName
    var mintingStatus: MintingStatus? by SongTable.mintingStatus
    var marketplaceStatus: MarketplaceStatus? by SongTable.marketplaceStatus

    fun toModel(): Song = Song(
        id = id.value,
        ownerId = ownerId.value,
        createdAt = createdAt,
        title = title,
        genre = genre,
        coverArtUrl = coverArtUrl,
        description = description,
        credits = credits,
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
            val fields = SongTable.slice(SongTable.id.count(), SongTable.genre)
            val query = if (ops.isEmpty()) fields.selectAll() else fields.select(AndOp(ops))
            return query.groupBy(SongTable.genre)
                .orderBy(SongTable.genre.count(), SortOrder.DESC)
                .mapLazy { it[SongTable.genre] }
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
                ops += SongTable.genre inList it
            }
            return ops
        }
    }
}
