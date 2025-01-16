package io.newm.server.features.song.database

import io.newm.server.features.song.model.SongSmartLink
import io.newm.server.typealiases.SongId
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import java.time.LocalDateTime
import java.util.UUID

class SongSmartLinkEntity(
    id: EntityID<UUID>
) : UUIDEntity(id) {
    val createdAt: LocalDateTime by SongSmartLinkTable.createdAt
    var songId: EntityID<SongId> by SongSmartLinkTable.songId
    var storeName: String by SongSmartLinkTable.storeName
    var url: String by SongSmartLinkTable.url

    fun toModel(): SongSmartLink =
        SongSmartLink(
            id = id.value,
            storeName = storeName,
            url = url
        )

    companion object : UUIDEntityClass<SongSmartLinkEntity>(SongSmartLinkTable) {
        fun findBySongId(
            songId: SongId
        ): List<SongSmartLinkEntity> = find { SongSmartLinkTable.songId eq songId }.toList()
    }
}
