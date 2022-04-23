package io.projectnewm.server.features.song.database

import io.projectnewm.server.features.song.model.Song
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.SizedIterable
import java.util.UUID

class SongEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    val createdAt by SongTable.createdAt
    var ownerId by SongTable.ownerId
    var title by SongTable.title
    var genres by SongTable.genres
    var covertArtUrl by SongTable.covertArtUrl
    var description by SongTable.description
    var credits by SongTable.credits

    fun toModel(): Song = Song(
        id = id.value,
        ownerId = ownerId.value,
        createdAt = createdAt,
        title = title,
        genres = genres?.toList(),
        covertArtUrl = covertArtUrl,
        description = description,
        credits = credits
    )

    companion object : UUIDEntityClass<SongEntity>(SongTable) {
        fun getAllByOwnerId(ownerId: UUID): SizedIterable<SongEntity> = SongEntity.find {
            SongTable.ownerId eq ownerId
        }
    }
}
