package io.newm.server.features.song.repo

import com.amazonaws.HttpMethod
import com.amazonaws.services.s3.AmazonS3
import io.ktor.server.application.ApplicationEnvironment
import io.ktor.util.logging.Logger
import io.newm.server.exception.HttpForbiddenException
import io.newm.server.exception.HttpUnprocessableEntityException
import io.newm.server.ext.getConfigLong
import io.newm.server.ext.getConfigString
import io.newm.server.ext.toDate
import io.newm.server.features.song.database.SongEntity
import io.newm.server.features.song.model.Song
import io.newm.server.features.song.model.SongFilters
import io.newm.server.features.user.database.UserTable
import java.time.Instant
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.MarkerFactory
import java.util.UUID

internal class SongRepositoryImpl(
    private val logger: Logger,
    private val environment: ApplicationEnvironment,
    private val s3: AmazonS3
) : SongRepository {

    private val marker = MarkerFactory.getMarker(javaClass.simpleName)

    override suspend fun add(song: Song, ownerId: UUID): UUID {
        logger.debug(marker, "add: song = $song")
        val title = song.title ?: throw HttpUnprocessableEntityException("missing title")
        val genre = song.genre ?: throw HttpUnprocessableEntityException("missing genre")
        return transaction {
            SongEntity.new {
                this.ownerId = EntityID(ownerId, UserTable)
                this.title = title
                this.genre = genre
                coverArtUrl = song.coverArtUrl
                description = song.description
                credits = song.credits
                streamUrl = song.streamUrl
                nftPolicyId = song.nftPolicyId
                nftName = song.nftName
                mintingStatus = song.mintingStatus
                marketplaceStatus = song.marketplaceStatus
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
            song.streamUrl?.let { entity.streamUrl = it }
            song.nftPolicyId?.let { entity.nftPolicyId = it }
            song.nftName?.let { entity.nftName = it }
            song.mintingStatus?.let { entity.mintingStatus = it }
            song.marketplaceStatus?.let { entity.marketplaceStatus = it }
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

    override suspend fun getAll(filters: SongFilters, offset: Int, limit: Int): List<Song> {
        logger.debug(marker, "getAll: filters = $filters, offset = $offset, limit = $limit")
        return transaction {
            SongEntity.all(filters)
                .limit(n = limit, offset = offset.toLong())
                .map(SongEntity::toModel)
        }
    }

    override suspend fun getGenres(filters: SongFilters, offset: Int, limit: Int): List<String> {
        logger.debug(marker, "getGenres: filters = $filters, offset = $offset, limit = $limit")
        return transaction {
            SongEntity.genres(filters)
                .limit(n = limit, offset = offset.toLong())
                .toList()
        }
    }

    override suspend fun generateUploadUrl(songId: UUID, requesterId: UUID, fileName: String): String {
        logger.debug(marker, "generateUploadUrl: songId = $songId, fileName = $fileName")

        if ('/' in fileName) throw HttpUnprocessableEntityException("Invalid fileName: $fileName")

        transaction {
            SongEntity[songId].checkRequester(requesterId)
        }

        val expiration = Instant.now()
            .plusSeconds(environment.getConfigLong("aws.s3.audioUpload.timeToLive"))
            .toDate()

        return s3.generatePresignedUrl(
            environment.getConfigString("aws.s3.audioUpload.bucketName"),
            "$songId/$fileName",
            expiration,
            HttpMethod.PUT
        ).toString()
    }

    override suspend fun updateStreamUrl(songId: UUID, streamUrl: String) {
        logger.debug(marker, "updateStreamUrl: songId = $songId, streamUrl = $streamUrl")
        transaction {
            val entity = SongEntity[songId]
            entity.streamUrl = streamUrl
        }
    }

    private fun SongEntity.checkRequester(requesterId: UUID) {
        if (ownerId.value != requesterId) throw HttpForbiddenException("operation allowed only by owner")
    }
}
