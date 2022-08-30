package io.newm.server.features.song.database

import io.newm.server.features.song.model.Song
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.SizedIterable
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
        fun getAllByOwnerId(ownerId: UUID): SizedIterable<SongEntity> = SongEntity.find {
            SongTable.ownerId eq ownerId
        }
    }
}
