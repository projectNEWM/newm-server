package io.projectnewm.server.features.song.repo

import io.ktor.util.logging.Logger
import io.projectnewm.server.exception.HttpForbiddenException
import io.projectnewm.server.exception.HttpUnprocessableEntityException
import io.projectnewm.server.features.song.database.SongEntity
import io.projectnewm.server.features.song.model.Song
import io.projectnewm.server.features.user.database.UserTable
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.MarkerFactory
import java.util.UUID

internal class SongRepositoryImpl(
    private val logger: Logger
) : SongRepository {

    private val marker = MarkerFactory.getMarker(javaClass.simpleName)

    override suspend fun add(song: Song, ownerId: UUID): UUID {
        logger.debug(marker, "add: song = $song")
        val title = song.title ?: throw HttpUnprocessableEntityException("missing title")
        return transaction {
            SongEntity.new {
                this.ownerId = EntityID(ownerId, UserTable)
                this.title = title
                genre = song.genre
                coverArtUrl = song.coverArtUrl
                description = song.description
                credits = song.credits
            }.id.value
        }
    }

    override suspend fun update(song: Song, songId: UUID, requesterId: UUID) {
        logger.debug(marker, "update: song = $song")
        transaction {
            val entity = SongEntity[songId]
            entity.checkRequester(requesterId)
            song.title?.let { entity.title = it }
            song.genre?.let { entity.genre = it }
            song.coverArtUrl?.let { entity.coverArtUrl = it }
            song.description?.let { entity.description = it }
            song.credits?.let { entity.credits = it }
        }
    }

    override suspend fun delete(songId: UUID, requesterId: UUID) {
        logger.debug(marker, "delete: songId = $songId")
        transaction {
            val entity = SongEntity[songId]
            entity.checkRequester(requesterId)
            entity.delete()
        }
    }

    override suspend fun get(songId: UUID): Song {
        logger.debug(marker, "get: songId = $songId")
        return transaction {
            SongEntity[songId].toModel()
        }
    }

    override suspend fun getAllByOwnerId(ownerId: UUID): List<Song> {
        logger.debug(marker, "getAll: ownerId = $ownerId")
        return transaction {
            SongEntity.getAllByOwnerId(ownerId).map(SongEntity::toModel)
        }
    }

    private fun SongEntity.checkRequester(requesterId: UUID) {
        if (ownerId.value != requesterId) throw HttpForbiddenException("operation allowed only by owner")
    }
}
