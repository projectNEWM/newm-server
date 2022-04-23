package io.projectnewm.server.features.playlist.repo

import io.ktor.util.logging.Logger
import io.projectnewm.server.exception.HttpForbiddenException
import io.projectnewm.server.exception.HttpUnprocessableEntityException
import io.projectnewm.server.features.playlist.database.PlaylistEntity
import io.projectnewm.server.features.playlist.model.Playlist
import io.projectnewm.server.features.song.database.SongEntity
import io.projectnewm.server.features.song.model.Song
import io.projectnewm.server.features.user.database.UserTable
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.MarkerFactory
import java.util.UUID

internal class PlaylistRepositoryImpl(
    private val logger: Logger
) : PlaylistRepository {

    private val marker = MarkerFactory.getMarker(javaClass.simpleName)

    override suspend fun add(playlist: Playlist, ownerId: UUID) {
        logger.debug(marker, "add: playlist = $playlist")
        val name = playlist.name ?: throw HttpUnprocessableEntityException("missing name")
        transaction {
            PlaylistEntity.new {
                this.ownerId = EntityID(ownerId, UserTable)
                this.name = name
            }
        }
    }

    override suspend fun update(playlist: Playlist, playlistId: UUID, requesterId: UUID) {
        logger.debug(marker, "update: playlist = $playlist")
        transaction {
            val entity = PlaylistEntity[playlistId]
            entity.checkRequester(requesterId)
            playlist.name?.let { entity.name = it }
        }
    }

    override suspend fun delete(playlistId: UUID, requesterId: UUID) {
        logger.debug(marker, "delete: playlistId = $playlistId")
        transaction {
            val entity = PlaylistEntity[playlistId]
            entity.checkRequester(requesterId)
            entity.delete()
        }
    }

    override suspend fun get(playlistId: UUID): Playlist {
        logger.debug(marker, "get: playlistId = $playlistId")
        return transaction {
            PlaylistEntity[playlistId].toModel()
        }
    }

    override suspend fun getAllByOwnerId(ownerId: UUID): List<Playlist> {
        logger.debug(marker, "getAll: ownerId = $ownerId")
        return transaction {
            PlaylistEntity.getAllByOwnerId(ownerId).map(PlaylistEntity::toModel)
        }
    }

    override suspend fun addSong(playlistId: UUID, songId: UUID, requesterId: UUID) {
        logger.debug(marker, "addSong: playlistId = $playlistId, songId = $songId")
        return transaction {
            val entity = PlaylistEntity[playlistId]
            entity.checkRequester(requesterId)
            entity.addSong(songId)
        }
    }

    override suspend fun deleteSong(playlistId: UUID, songId: UUID, requesterId: UUID) {
        logger.debug(marker, "deleteSong: playlistId = $playlistId, songId = $songId")
        transaction {
            val entity = PlaylistEntity[playlistId]
            entity.checkRequester(requesterId)
            entity.deleteSong(songId)
        }
    }

    override suspend fun getSongs(playlistId: UUID): List<Song> {
        logger.debug(marker, "getSongs: playlistId = $playlistId")
        return transaction {
            PlaylistEntity[playlistId].songs.map(SongEntity::toModel)
        }
    }

    private fun PlaylistEntity.checkRequester(requesterId: UUID) {
        if (ownerId.value != requesterId) throw HttpForbiddenException("operation allowed only by owner")
    }
}
