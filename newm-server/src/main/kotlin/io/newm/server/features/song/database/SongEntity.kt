package io.newm.server.features.song.database

import io.newm.server.features.song.model.Song
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.neq
import java.util.UUID

class SongEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    val createdAt by SongTable.createdAt
    var ownerId by SongTable.ownerId
    var title by SongTable.title
    var genre by SongTable.genre
    var coverArtUrl by SongTable.coverArtUrl
    var description by SongTable.description
    var credits by SongTable.credits
    var streamUrl by SongTable.streamUrl
    var nftPolicyId by SongTable.nftPolicyId
    var nftName by SongTable.nftName

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
        nftName = nftName
    )

    companion object : UUIDEntityClass<SongEntity>(SongTable) {
        fun allByOwnerId(ownerId: UUID): SizedIterable<SongEntity> = SongEntity.find {
            SongTable.ownerId eq ownerId
        }

        fun allByGenre(genre: String): SizedIterable<SongEntity> = SongEntity.find {
            SongTable.genre eq genre
        }

        fun genres(ownerId: UUID?): SizedIterable<String> {
            val where = ownerId?.let {
                (SongTable.ownerId eq ownerId) and (SongTable.genre neq null)
            } ?: (SongTable.genre neq null)
            return SongTable.slice(SongTable.id.count(), SongTable.genre)
                .select(where)
                .groupBy(SongTable.genre)
                .orderBy(SongTable.genre.count(), SortOrder.DESC)
                .mapLazy { it[SongTable.genre]!! }
        }
    }
}
