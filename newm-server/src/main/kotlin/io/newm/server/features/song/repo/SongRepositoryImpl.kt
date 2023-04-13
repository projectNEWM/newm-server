package io.newm.server.features.song.repo

import com.amazonaws.HttpMethod
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.s3.AmazonS3
import com.google.iot.cbor.CborInteger
import io.ktor.server.application.ApplicationEnvironment
import io.ktor.util.logging.Logger
import io.newm.chain.grpc.Utxo
import io.newm.chain.grpc.outputUtxo
import io.newm.chain.util.toHexString
import io.newm.server.aws.s3.createPresignedPost
import io.newm.server.aws.s3.model.ContentLengthRangeCondition
import io.newm.server.aws.s3.model.PresignedPost
import io.newm.server.config.repo.ConfigRepository
import io.newm.server.features.cardano.database.KeyTable
import io.newm.server.features.cardano.model.Key
import io.newm.server.features.cardano.repo.CardanoRepository
import io.newm.server.features.distribution.DistributionRepository
import io.newm.server.features.song.database.SongEntity
import io.newm.server.features.song.model.MintingStatus
import io.newm.server.features.song.model.Song
import io.newm.server.features.song.model.SongFilters
import io.newm.server.features.user.database.UserTable
import io.newm.server.ktx.checkLength
import io.newm.shared.exception.HttpForbiddenException
import io.newm.shared.exception.HttpUnprocessableEntityException
import io.newm.shared.koin.inject
import io.newm.shared.ktx.debug
import io.newm.shared.ktx.getConfigChild
import io.newm.shared.ktx.getConfigString
import io.newm.shared.ktx.getLong
import io.newm.shared.ktx.getString
import io.newm.shared.ktx.toDate
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.core.parameter.parametersOf
import java.time.Instant
import java.util.UUID

private const val CONFIG_ID_MINT_PRICE = "mint.price"

internal class SongRepositoryImpl(
    private val environment: ApplicationEnvironment,
    private val s3: AmazonS3,
    private val configRepository: ConfigRepository,
    private val cardanoRepository: CardanoRepository,
    private val distributionRepository: DistributionRepository,
) : SongRepository {

    private val logger: Logger by inject { parametersOf(javaClass.simpleName) }

    override suspend fun add(song: Song, ownerId: UUID): UUID {
        logger.debug { "add: song = $song" }
        val title = song.title ?: throw HttpUnprocessableEntityException("missing title")
        val genres = song.genres ?: throw HttpUnprocessableEntityException("missing genres")
        song.checkFieldLengths()
        return transaction {
            SongEntity.new {
                this.ownerId = EntityID(ownerId, UserTable)
                this.title = title
                this.genres = genres.toTypedArray()
                moods = song.moods?.toTypedArray()
                coverArtUrl = song.coverArtUrl
                description = song.description
                credits = song.credits
                duration = song.duration
                streamUrl = song.streamUrl
                nftPolicyId = song.nftPolicyId
                nftName = song.nftName
            }.id.value
        }
    }

    override suspend fun update(songId: UUID, song: Song, requesterId: UUID?) {
        logger.debug { "update: songId = $songId, song = $song, requesterId = $requesterId" }
        song.checkFieldLengths()
        transaction {
            val entity = SongEntity[songId]
            requesterId?.let { entity.checkRequester(it) }
            with(song) {
                title?.let { entity.title = it }
                genres?.let { entity.genres = it.toTypedArray() }
                moods?.let { entity.moods = it.toTypedArray() }
                coverArtUrl?.let { entity.coverArtUrl = it }
                description?.let { entity.description = it }
                credits?.let { entity.credits = it }
                duration?.let { entity.duration = it }
                streamUrl?.let { entity.streamUrl = it }
                nftPolicyId?.let { entity.nftPolicyId = it }
                nftName?.let { entity.nftName = it }
                if (requesterId == null) {
                    // don't allow updating these fields when invoked from REST API
                    mintingStatus?.let { entity.mintingStatus = it }
                    marketplaceStatus?.let { entity.marketplaceStatus = it }
                    paymentKeyId?.let { entity.paymentKeyId = EntityID(it, KeyTable) }
                }
            }
        }
    }

    override suspend fun delete(songId: UUID, requesterId: UUID) {
        logger.debug { "delete: songId = $songId, requesterId = $requesterId" }
        transaction {
            val entity = SongEntity[songId]
            entity.checkRequester(requesterId)
            entity.delete()
        }
    }

    override suspend fun get(songId: UUID): Song {
        logger.debug { "get: songId = $songId" }
        return transaction {
            SongEntity[songId].toModel()
        }
    }

    override suspend fun getAll(filters: SongFilters, offset: Int, limit: Int): List<Song> {
        logger.debug { "getAll: filters = $filters, offset = $offset, limit = $limit" }
        return transaction {
            SongEntity.all(filters)
                .limit(n = limit, offset = offset.toLong())
                .map(SongEntity::toModel)
        }
    }

    override suspend fun getAllCount(filters: SongFilters): Long {
        logger.debug { "getAllCount: filters = $filters" }
        return transaction {
            SongEntity.all(filters).count()
        }
    }

    override suspend fun getGenres(filters: SongFilters, offset: Int, limit: Int): List<String> {
        logger.debug { "getGenres: filters = $filters, offset = $offset, limit = $limit" }
        return transaction {
            SongEntity.genres(filters)
                .limit(n = limit, offset = offset.toLong())
                .toList()
        }
    }

    override suspend fun getGenreCount(filters: SongFilters): Long {
        logger.debug { "getGenresCount: filters = $filters" }
        return transaction {
            SongEntity.genres(filters).count()
        }
    }

    override suspend fun generateAudioUploadUrl(
        songId: UUID,
        requesterId: UUID,
        fileName: String
    ): String {
        logger.debug { "generateAudioUploadUrl: songId = $songId, fileName = $fileName" }

        if ('/' in fileName) throw HttpUnprocessableEntityException("Invalid fileName: $fileName")

        checkRequester(songId, requesterId)

        val config = environment.getConfigChild("aws.s3.audio")
        val expiration = Instant.now()
            .plusSeconds(config.getLong("timeToLive"))
            .toDate()

        return s3.generatePresignedUrl(
            config.getString("bucketName"),
            "$songId/$fileName",
            expiration,
            HttpMethod.PUT
        ).toString()
    }

    override suspend fun generateAudioUploadPost(
        songId: UUID,
        requesterId: UUID,
        fileName: String
    ): PresignedPost {
        logger.debug { "generateAudioUploadUrl: songId = $songId, fileName = $fileName" }

        if ('/' in fileName) throw HttpUnprocessableEntityException("Invalid fileName: $fileName")

        checkRequester(songId, requesterId)

        val config = environment.getConfigChild("aws.s3.audio")

        return s3.createPresignedPost {
            bucket = config.getString("bucketName")
            key = "$songId/$fileName"
            credentials = BasicAWSCredentials(
                environment.getConfigString("aws.accessKeyId"),
                environment.getConfigString("aws.secretKey")
            )
            conditions = listOf(
                ContentLengthRangeCondition(
                    config.getLong("minUploadSizeMB") * 1024 * 1024,
                    config.getLong("maxUploadSizeMB") * 1024 * 1024
                )
            )
            expiresSeconds = config.getLong("timeToLive")
        }
    }

    override suspend fun processStreamTokenAgreement(songId: UUID, requesterId: UUID, accepted: Boolean) {
        logger.debug { "processStreamTokenAgreement: songId = $songId, accepted = $accepted" }

        checkRequester(songId, requesterId)

        val bucketName = environment.getConfigString("aws.s3.agreement.bucketName")
        val fileName = environment.getConfigString("aws.s3.agreement.fileName")
        val filePath = "$songId/$fileName"

        if (accepted) {
            if (!s3.doesObjectExist(bucketName, filePath)) {
                throw HttpUnprocessableEntityException("missing: $filePath")
            }
            update(songId, Song(mintingStatus = MintingStatus.StreamTokenAgreementApproved))
        } else {
            update(songId, Song(mintingStatus = MintingStatus.Undistributed))
            s3.deleteObject(bucketName, filePath)
        }
    }

    override suspend fun getMintingPaymentAmount(songId: UUID, requesterId: UUID): String {
        logger.debug { "getMintingPaymentAmount: songId = $songId" }
        // We might need to change this code in the future if we're charging NEWM tokens in addition to ada
        return CborInteger.create(configRepository.getLong(CONFIG_ID_MINT_PRICE)).toCborByteArray().toHexString()
    }

    override suspend fun generateMintingPaymentTransaction(
        songId: UUID,
        requesterId: UUID,
        sourceUtxos: List<Utxo>,
        changeAddress: String
    ): String {
        logger.debug { "generateMintingPaymentTransaction: songId = $songId" }

        checkRequester(songId, requesterId)

        val key = Key.generateNew()
        val keyId = cardanoRepository.add(key)
        val amount = configRepository.getLong(CONFIG_ID_MINT_PRICE)
        val transaction = cardanoRepository.buildTransaction {
            this.sourceUtxos.addAll(sourceUtxos)
            this.outputUtxos.add(
                outputUtxo {
                    address = key.address
                    lovelace = amount.toString()
                }
            )
            this.changeAddress = changeAddress
        }

        update(songId, Song(mintingStatus = MintingStatus.MintingPaymentRequested, paymentKeyId = keyId))
        return transaction.transactionCbor.toByteArray().toHexString()
    }

    override suspend fun distribute(songId: UUID) {
        val song = SongEntity[songId]

        distributionRepository.distributeSong(song)
    }

    private fun checkRequester(songId: UUID, requesterId: UUID) = transaction {
        SongEntity[songId].checkRequester(requesterId)
    }

    private fun SongEntity.checkRequester(requesterId: UUID) {
        if (ownerId.value != requesterId) throw HttpForbiddenException("operation allowed only by owner")
    }

    private fun Song.checkFieldLengths() {
        title?.checkLength("title")
        genres?.forEachIndexed { index, genre -> genre.checkLength("genres$index") }
        moods?.forEachIndexed { index, mood -> mood.checkLength("moods$index") }
        description?.checkLength("description", 250)
        credits?.checkLength("credits")
        nftName?.checkLength("nftName")
    }
}
