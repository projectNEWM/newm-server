package io.newm.server.features.playlist.repo

import io.github.oshai.kotlinlogging.KotlinLogging
import io.newm.server.features.playlist.database.PlaylistEntity
import io.newm.server.features.playlist.model.Playlist
import io.newm.server.features.playlist.model.PlaylistFilters
import io.newm.server.features.song.database.ReleaseEntity
import io.newm.server.features.song.model.Song
import io.newm.server.features.user.database.UserTable
import io.newm.server.ktx.checkLength
import io.newm.server.typealiases.SongId
import io.newm.server.typealiases.UserId
import io.newm.shared.exception.HttpForbiddenException
import io.newm.shared.exception.HttpUnprocessableEntityException
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

internal class PlaylistRepositoryImpl : PlaylistRepository {
    private val logger = KotlinLogging.logger {}

    override suspend fun add(
        playlist: Playlist,
        ownerId: UserId
    ): UUID {
        logger.debug { "add: playlist = $playlist" }
        val name = playlist.name ?: throw HttpUnprocessableEntityException("missing name")
        playlist.checkFieldLengths()
        return transaction {
            PlaylistEntity
                .new {
                    this.ownerId = EntityID(ownerId, UserTable)
                    this.name = name
                }.id.value
        }
    }

    override suspend fun update(
        playlist: Playlist,
        playlistId: UUID,
        requesterId: UserId
    ) {
        logger.debug { "update: playlist = $playlist" }
        playlist.checkFieldLengths()
        transaction {
            val entity = PlaylistEntity[playlistId]
            entity.checkRequester(requesterId)
            playlist.name?.let { entity.name = it }
        }
    }

    override suspend fun delete(
        playlistId: UUID,
        requesterId: UserId
    ) {
        logger.debug { "delete: playlistId = $playlistId" }
        transaction {
            val entity = PlaylistEntity[playlistId]
            entity.checkRequester(requesterId)
            entity.delete()
        }
    }

    override suspend fun get(playlistId: UUID): Playlist {
        logger.debug { "get: playlistId = $playlistId" }
        return transaction {
            PlaylistEntity[playlistId].toModel()
        }
    }

    override suspend fun getAll(
        filters: PlaylistFilters,
        offset: Int,
        limit: Int
    ): List<Playlist> {
        logger.debug { "getAll: filters = $filters, offset = $offset, limit = $limit" }
        return transaction {
            PlaylistEntity
                .all(filters)
                .limit(n = limit, offset = offset.toLong())
                .map(PlaylistEntity::toModel)
        }
    }

    override suspend fun getAllCount(filters: PlaylistFilters): Long {
        logger.debug { "getAllCount: filters = $filters" }
        return transaction {
            PlaylistEntity.all(filters).count()
        }
    }

    override suspend fun addSong(
        playlistId: UUID,
        songId: SongId,
        requesterId: UserId
    ) {
        logger.debug { "addSong: playlistId = $playlistId, songId = $songId" }
        return transaction {
            val entity = PlaylistEntity[playlistId]
            entity.checkRequester(requesterId)
            entity.addSong(songId)
        }
    }

    override suspend fun deleteSong(
        playlistId: UUID,
        songId: SongId,
        requesterId: UserId
    ) {
        logger.debug { "deleteSong: playlistId = $playlistId, songId = $songId" }
        transaction {
            val entity = PlaylistEntity[playlistId]
            entity.checkRequester(requesterId)
            entity.deleteSong(songId)
        }
    }

    override suspend fun getSongs(
        playlistId: UUID,
        offset: Int,
        limit: Int
    ): List<Song> {
        logger.debug { "getSongs: playlistId = $playlistId, offset = $offset, limit = $limit" }
        return transaction {
            PlaylistEntity[playlistId].songs.limit(n = limit, offset = offset.toLong()).map { songEntity ->
                val release = ReleaseEntity[songEntity.releaseId!!].toModel()
                songEntity.toModel(release)
            }
        }
    }

    private fun PlaylistEntity.checkRequester(requesterId: UserId) {
        if (ownerId.value != requesterId) throw HttpForbiddenException("operation allowed only by owner")
    }

    private fun Playlist.checkFieldLengths() {
        name?.checkLength("name")
    }
}
