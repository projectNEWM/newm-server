package io.newm.server.features.song.repo

import com.google.iot.cbor.CborInteger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.URLBuilder
import io.ktor.http.URLProtocol
import io.ktor.http.encodedPath
import io.ktor.server.application.ApplicationEnvironment
import io.ktor.utils.io.ByteReadChannel
import io.newm.chain.grpc.Utxo
import io.newm.chain.grpc.outputUtxo
import io.newm.chain.util.toAdaString
import io.newm.chain.util.toHexString
import io.newm.server.aws.cloudfront.cloudfrontAudioStreamData
import io.newm.server.aws.s3.doesObjectExist
import io.newm.server.aws.s3.s3UrlStringOf
import io.newm.server.config.repo.ConfigRepository
import io.newm.server.config.repo.ConfigRepository.Companion.CONFIG_KEY_DISTRIBUTION_PRICE_USD
import io.newm.server.config.repo.ConfigRepository.Companion.CONFIG_KEY_MINT_PRICE
import io.newm.server.config.repo.ConfigRepository.Companion.CONFIG_KEY_SONG_SMART_LINKS_CACHE_TTL
import io.newm.server.config.repo.ConfigRepository.Companion.CONFIG_KEY_SONG_SMART_LINKS_USE_DISTRIBUTOR
import io.newm.server.features.cardano.database.KeyTable
import io.newm.server.features.cardano.model.Key
import io.newm.server.features.cardano.repo.CardanoRepository
import io.newm.server.features.collaboration.model.CollaborationStatus
import io.newm.server.features.collaboration.model.CollaboratorFilters
import io.newm.server.features.collaboration.repo.CollaborationRepository
import io.newm.server.features.distribution.DistributionRepository
import io.newm.server.features.email.repo.EmailRepository
import io.newm.server.features.minting.MintingStatusSqsMessage
import io.newm.server.features.minting.repo.MintingRepository
import io.newm.server.features.release.repo.OutletReleaseRepository
import io.newm.server.features.song.database.ReleaseEntity
import io.newm.server.features.song.database.ReleaseTable
import io.newm.server.features.song.database.SongEntity
import io.newm.server.features.song.database.SongErrorHistoryTable
import io.newm.server.features.song.database.SongReceiptEntity
import io.newm.server.features.song.database.SongReceiptTable
import io.newm.server.features.song.database.SongSmartLinkEntity
import io.newm.server.features.song.database.SongTable
import io.newm.server.features.song.model.AudioEncodingStatus
import io.newm.server.features.song.model.AudioStreamData
import io.newm.server.features.song.model.AudioUploadReport
import io.newm.server.features.song.model.MintPaymentResponse
import io.newm.server.features.song.model.MintingStatus
import io.newm.server.features.song.model.RefundPaymentResponse
import io.newm.server.features.song.model.Release
import io.newm.server.features.song.model.ReleaseType
import io.newm.server.features.song.model.Song
import io.newm.server.features.song.model.SongFilters
import io.newm.server.features.song.model.SongSmartLink
import io.newm.server.features.user.database.UserEntity
import io.newm.server.features.user.database.UserTable
import io.newm.server.features.user.model.UserVerificationStatus
import io.newm.server.ktx.asValidUrl
import io.newm.server.ktx.await
import io.newm.server.ktx.checkLength
import io.newm.server.ktx.getSecureConfigString
import io.newm.server.model.FilterCriteria
import io.newm.server.typealiases.ReleaseId
import io.newm.server.typealiases.SongId
import io.newm.server.typealiases.UserId
import io.newm.shared.exception.HttpConflictException
import io.newm.shared.exception.HttpForbiddenException
import io.newm.shared.exception.HttpUnprocessableEntityException
import io.newm.shared.koin.inject
import io.newm.shared.ktx.getConfigChild
import io.newm.shared.ktx.getConfigString
import io.newm.shared.ktx.getInt
import io.newm.shared.ktx.getLong
import io.newm.shared.ktx.getString
import io.newm.shared.ktx.orNull
import io.newm.shared.ktx.orZero
import io.newm.shared.ktx.propertiesFromResource
import io.newm.shared.ktx.toTempFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.apache.tika.Tika
import org.jaudiotagger.audio.AudioFileIO
import org.jetbrains.annotations.VisibleForTesting
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import java.net.URI
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Properties
import java.util.UUID

internal class SongRepositoryImpl(
    private val environment: ApplicationEnvironment,
    private val s3: S3Client,
    private val configRepository: ConfigRepository,
    private val cardanoRepository: CardanoRepository,
    private val distributionRepository: DistributionRepository,
    private val collaborationRepository: CollaborationRepository,
    private val emailRepository: EmailRepository,
    private val outletReleaseRepository: OutletReleaseRepository
) : SongRepository {
    private val logger = KotlinLogging.logger {}
    private val json: Json by inject()
    private val queueUrl by lazy { environment.getConfigString("aws.sqs.minting.queueUrl") }
    private val mimeTypes: Properties by lazy {
        propertiesFromResource("audio-mime-types.properties")
    }
    private val mintingRepository: MintingRepository by inject()

    override suspend fun add(
        song: Song,
        ownerId: UserId
    ): SongId {
        logger.debug { "add: song = $song" }
        val title = song.title ?: throw HttpUnprocessableEntityException("missing title")
        val genres = song.genres ?: throw HttpUnprocessableEntityException("missing genres")
        song.checkFieldLengths()
        return transaction {
            title.checkTitleUnique(ownerId, song.mintingStatus)
            val releaseId =
                ReleaseEntity
                    .new {
                        archived = false
                        this.ownerId = EntityID(ownerId, UserTable)
                        this.title = title
                        // TODO: Refactor 'single' hardcoding once we have album support in the UI/UX
                        releaseType = ReleaseType.SINGLE
                        barcodeType = song.barcodeType
                        barcodeNumber = song.barcodeNumber
                        releaseDate = song.releaseDate
                        publicationDate = song.publicationDate
                        coverArtUrl = song.coverArtUrl?.asValidUrl()
                        hasSubmittedForDistribution = false
                        errorMessage = song.errorMessage
                    }.id.value
            SongEntity
                .new {
                    archived = song.archived == true
                    this.ownerId = EntityID(ownerId, UserTable)
                    this.title = title
                    this.genres = genres
                    moods = song.moods
                    description = song.description
                    this.releaseId = EntityID(releaseId, ReleaseTable)
                    track = song.track
                    language = song.language
                    coverRemixSample = song.coverRemixSample == true
                    compositionCopyrightOwner = song.compositionCopyrightOwner
                    compositionCopyrightYear = song.compositionCopyrightYear
                    phonographicCopyrightOwner = song.phonographicCopyrightOwner
                    phonographicCopyrightYear = song.phonographicCopyrightYear
                    parentalAdvisory = song.parentalAdvisory
                    isrc = song.isrc
                    iswc = song.iswc
                    ipis = song.ipis
                    lyricsUrl = song.lyricsUrl?.asValidUrl()
                    instrumental = song.instrumental ?: song.genres.contains("Instrumental")
                    this.mintingStatus = MintingStatus.Undistributed
                }.id.value
        }
    }

    override suspend fun update(
        songId: SongId,
        song: Song,
        requesterId: UserId?
    ) {
        logger.debug { "update: songId = $songId, song = $song, requesterId = $requesterId" }
        song.checkFieldLengths()
        transaction {
            val songEntity = SongEntity[songId]
            val releaseEntity = ReleaseEntity[songEntity.releaseId!!]
            requesterId?.let { songEntity.checkRequester(it) }

            with(song) {
                archived?.let { songEntity.archived = it }
                title?.let {
                    if (!it.equals(songEntity.title, ignoreCase = true)) {
                        it.checkTitleUnique(songEntity.ownerId.value, songEntity.mintingStatus)
                    }
                    songEntity.title = it
                }
                genres?.let { songEntity.genres = it }
                moods?.let { songEntity.moods = it }
                coverArtUrl?.let { releaseEntity.coverArtUrl = it.orNull()?.asValidUrl() }
                description?.let { songEntity.description = it.orNull() }
                track?.let { songEntity.track = it }
                language?.let { songEntity.language = it.orNull() }
                coverRemixSample?.let { songEntity.coverRemixSample = it }
                compositionCopyrightOwner?.let { songEntity.compositionCopyrightOwner = it.orNull() }
                compositionCopyrightYear?.let { songEntity.compositionCopyrightYear = it }
                phonographicCopyrightOwner?.let { songEntity.phonographicCopyrightOwner = it.orNull() }
                phonographicCopyrightYear?.let { songEntity.phonographicCopyrightYear = it }
                parentalAdvisory?.let { songEntity.parentalAdvisory = it.orNull() }
                barcodeType?.let { releaseEntity.barcodeType = it }
                barcodeNumber?.let { releaseEntity.barcodeNumber = it }
                isrc?.let { songEntity.isrc = it.orNull() }
                iswc?.let { songEntity.iswc = it.orNull() }
                ipis?.let { songEntity.ipis = it }
                // only allow updating release date if we have never submitted for distribution
                if (!releaseEntity.hasSubmittedForDistribution) {
                    releaseDate?.let { releaseEntity.releaseDate = it }
                }
                publicationDate?.let { releaseEntity.publicationDate = it }
                lyricsUrl?.let { songEntity.lyricsUrl = it.orNull()?.asValidUrl() }
                instrumental?.let { songEntity.instrumental = it }

                if (requesterId == null) {
                    // don't allow updating these fields when invoked from REST API
                    tokenAgreementUrl?.let { songEntity.tokenAgreementUrl = it.orNull()?.asValidUrl() }
                    originalAudioUrl?.let { songEntity.originalAudioUrl = it.orNull()?.asValidUrl() }
                    clipUrl?.let { songEntity.clipUrl = it.orNull()?.asValidUrl() }
                    streamUrl?.let { songEntity.streamUrl = it.orNull()?.asValidUrl() }
                    duration?.let { songEntity.duration = it }
                    nftPolicyId?.let { songEntity.nftPolicyId = it.orNull() }
                    nftName?.let { songEntity.nftName = it.orNull() }
                    mintingTxId?.let { songEntity.mintingTxId = it.orNull() }
                    audioEncodingStatus?.let { songEntity.audioEncodingStatus = it }
                    mintingStatus?.let { songEntity.mintingStatus = it }
                    marketplaceStatus?.let { songEntity.marketplaceStatus = it }
                    paymentKeyId?.let { songEntity.paymentKeyId = EntityID(it, KeyTable) }
                    arweaveLyricsUrl?.let { songEntity.arweaveLyricsUrl = it.orNull()?.asValidUrl() }
                    arweaveClipUrl?.let { songEntity.arweaveClipUrl = it.orNull()?.asValidUrl() }
                    arweaveTokenAgreementUrl?.let { songEntity.arweaveTokenAgreementUrl = it.orNull()?.asValidUrl() }
                    distributionTrackId?.let { songEntity.distributionTrackId = it }
                    mintCostLovelace?.let { releaseEntity.mintCostLovelace = it }
                    forceDistributed?.let { releaseEntity.forceDistributed = it }
                    errorMessage?.let { releaseEntity.errorMessage = it.orNull() }
                }
            }
        }
    }

    override suspend fun update(
        releaseId: ReleaseId,
        release: Release,
        requesterId: UserId?
    ) {
        logger.debug { "update: releaseId = $releaseId, release = $release, requesterId = $requesterId" }
        transaction {
            val releaseEntity = ReleaseEntity[releaseId]
            requesterId?.let { releaseEntity.checkRequester(it) }

            with(release) {
                archived?.let { releaseEntity.archived = it }
                title?.let { releaseEntity.title = it }
                coverArtUrl?.let { releaseEntity.coverArtUrl = it.orNull()?.asValidUrl() }
                barcodeType?.let { releaseEntity.barcodeType = it }
                barcodeNumber?.let { releaseEntity.barcodeNumber = it }
                releaseDate?.let { releaseEntity.releaseDate = it }
                publicationDate?.let { releaseEntity.publicationDate = it }
                releaseType?.let { releaseEntity.releaseType = it }
                preSavePage?.let { releaseEntity.preSavePage = it }

                if (requesterId == null) {
                    // don't allow updating these fields when invoked from REST API
                    arweaveCoverArtUrl?.let { releaseEntity.arweaveCoverArtUrl = it.orNull()?.asValidUrl() }
                    hasSubmittedForDistribution?.let { releaseEntity.hasSubmittedForDistribution = it }
                    distributionReleaseId?.let { releaseEntity.distributionReleaseId = it }
                    forceDistributed?.let { releaseEntity.forceDistributed = it }
                    errorMessage?.let { releaseEntity.errorMessage = it.orNull() }
                    mintCostLovelace?.let { releaseEntity.mintCostLovelace = it }
                }
            }
        }
    }

    override fun set(
        songId: SongId,
        editor: (SongEntity) -> Unit
    ) {
        logger.debug { "set: songId = $songId" }
        transaction {
            val entity = SongEntity[songId]
            editor(entity)
        }
    }

    override suspend fun delete(
        songId: SongId,
        requesterId: UserId
    ) {
        logger.debug { "delete: songId = $songId, requesterId = $requesterId" }
        transaction {
            val entity = SongEntity[songId]
            entity.checkRequester(requesterId)
            entity.delete()
        }
    }

    override suspend fun get(songId: SongId): Song {
        logger.debug { "get: songId = $songId" }
        return transaction {
            SongEntity[songId].let {
                val release = ReleaseEntity[it.releaseId!!].toModel()
                it.toModel(release)
            }
        }
    }

    override suspend fun getRelease(releaseId: ReleaseId): Release {
        logger.debug { "getRelease: releaseId = $releaseId" }
        return transaction {
            ReleaseEntity[releaseId].toModel()
        }
    }

    override suspend fun getAll(
        filters: SongFilters,
        offset: Int,
        limit: Int
    ): List<Song> {
        logger.debug { "getAll: filters = $filters, offset = $offset, limit = $limit" }
        return transaction {
            SongEntity
                .all(filters)
                .offset(start = offset.toLong())
                .limit(count = limit)
                .map {
                    val release = ReleaseEntity[it.releaseId!!].toModel()
                    it.toModel(release)
                }
        }
    }

    override suspend fun getAllCount(filters: SongFilters): Long {
        logger.debug { "getAllCount: filters = $filters" }
        return transaction {
            SongEntity.all(filters).count()
        }
    }

    override suspend fun getAllByReleaseId(id: ReleaseId): List<Song> {
        logger.debug { "getAllByReleaseId: id = $id" }
        return transaction {
            val release = ReleaseEntity[id].toModel()
            SongEntity
                .wrapRows(
                    SongTable.selectAll().where { SongTable.releaseId eq id }
                ).map { it.toModel(release) }
        }
    }

    override suspend fun getGenres(
        filters: SongFilters,
        offset: Int,
        limit: Int
    ): List<String> {
        logger.debug { "getGenres: filters = $filters, offset = $offset, limit = $limit" }
        return transaction {
            SongEntity
                .genres(filters)
                .offset(start = offset.toLong())
                .limit(count = limit)
                .toList()
        }
    }

    override suspend fun getGenreCount(filters: SongFilters): Long {
        logger.debug { "getGenresCount: filters = $filters" }
        return transaction {
            SongEntity.genres(filters).count()
        }
    }

    override suspend fun uploadAudio(
        songId: SongId,
        requesterId: UserId,
        data: ByteReadChannel
    ): AudioUploadReport {
        // File I/O is blocking so make sure we're on the IO dispatcher
        return withContext(Dispatchers.IO) {
            logger.debug { "uploadAudio: songId = $songId" }

            checkRequester(songId, requesterId)
            val config = environment.getConfigChild("aws.s3.audio")
            val file = try {
                data.toTempFile()
            } catch (e: Throwable) {
                logger.error(e) { "Failed to create audio temp file" }
                throw e
            }
            try {
                val size = file.length()
                val minSize = config.getLong("minFileSize")
                if (size < minSize) throw HttpUnprocessableEntityException("File is too small: $size bytes")
                val maxSize = config.getLong("maxFileSize")
                if (size > maxSize) throw HttpUnprocessableEntityException("File is too large: $size bytes")

                // enforce supported format
                val type = Tika().detect(file)
                val ext =
                    mimeTypes.getProperty(type)
                        ?: throw HttpUnprocessableEntityException("Unsupported media type: $type")

                // enforce duration
                val header = AudioFileIO.readAs(file, ext).audioHeader
                val duration = header.trackLength
                val minDuration = config.getInt("minDuration")
                if (duration < minDuration) throw HttpUnprocessableEntityException("Duration is too short: $duration secs")

                // enforce sampling rate
                val sampleRate = header.sampleRateAsNumber
                val minSampleRate = config.getInt("minSampleRate")
                if (sampleRate < minSampleRate) throw HttpUnprocessableEntityException("Sample rate is too low: $sampleRate Hz")

                val bucket = config.getString("bucketName")
                val key = "$songId/audio.$ext"
                val request = PutObjectRequest
                    .builder()
                    .bucket(bucket)
                    .key(key)
                    .build()
                s3.putObject(request, file.toPath())

                val url = s3UrlStringOf(bucket, key)
                transaction {
                    with(SongEntity[songId]) {
                        originalAudioUrl = url
                        audioEncodingStatus = AudioEncodingStatus.Started
                    }
                }
                AudioUploadReport(url, type, size, duration, sampleRate)
            } catch (throwable: Throwable) {
                transaction { SongEntity[songId].audioEncodingStatus = AudioEncodingStatus.Failed }
                throw throwable
            } finally {
                file.delete()
            }
        }
    }

    override suspend fun generateAudioStreamData(songId: SongId): AudioStreamData {
        val song = get(songId)
        if (song.streamUrl == null) {
            throw HttpUnprocessableEntityException("streamUrl is null")
        }

        val songStreamUrl = URI.create(song.streamUrl).toURL()
        val mediaHostUrl = URI.create(environment.getConfigString("aws.cloudFront.audioStream.hostUrl")).toURL()
        val kpid = environment.getSecureConfigString("aws.cloudFront.audioStream.keyPairId")
        val pk = environment.getSecureConfigString("aws.cloudFront.audioStream.privateKey")
        val cookieDom = environment.getConfigString("ktor.deployment.cookieDomain")

        // fix up the url so that the url does not point to old cloudfront distros
        val streamUrl =
            URLBuilder()
                .apply {
                    protocol = URLProtocol.createOrDefault(mediaHostUrl.protocol)
                    host = mediaHostUrl.host
                    encodedPath = songStreamUrl.path
                }.build()

        return cloudfrontAudioStreamData {
            url = streamUrl.toString()
            keyPairId = kpid
            privateKey = pk
            cookieDomain = cookieDom
        }
    }

    override suspend fun processStreamTokenAgreement(
        songId: SongId,
        requesterId: UserId,
        accepted: Boolean
    ) {
        logger.debug { "processStreamTokenAgreement: songId = $songId, accepted = $accepted" }

        checkRequester(songId, requesterId, verified = true)

        val bucketName = environment.getConfigString("aws.s3.agreement.bucketName")
        val fileName = environment.getConfigString("aws.s3.agreement.fileName")
        val key = "$songId/$fileName"

        if (accepted) {
            if (!s3.doesObjectExist(bucketName, key)) {
                throw HttpUnprocessableEntityException("missing: $key")
            }
            update(
                songId = songId,
                song =
                    Song(
                        mintingStatus = MintingStatus.StreamTokenAgreementApproved,
                        tokenAgreementUrl = s3UrlStringOf(bucketName, key)
                    )
            )
        } else {
            update(songId, Song(mintingStatus = MintingStatus.Undistributed))
            val request = DeleteObjectRequest
                .builder()
                .bucket(bucketName)
                .key(key)
                .build()
            s3.deleteObject(request)
        }
    }

    override suspend fun processAudioEncoding(songId: SongId) {
        logger.debug { "processAudioEncoding: songId = $songId" }
        with(get(songId)) {
            when (audioEncodingStatus) {
                AudioEncodingStatus.Started -> {
                    if (clipUrl != null && streamUrl != null) {
                        update(songId, Song(audioEncodingStatus = AudioEncodingStatus.Completed))
                        if (mintingStatus?.ordinal.orZero() >= MintingStatus.MintingPaymentSubmitted.ordinal) {
                            sendMintingStartedNotification(songId)
                        }
                    }
                }

                AudioEncodingStatus.Completed -> {}

                else -> return
            }
            if (mintingStatus == MintingStatus.AwaitingAudioEncoding) {
                updateSongMintingStatus(songId, MintingStatus.AwaitingCollaboratorApproval)
            }
        }
    }

    override suspend fun processCollaborations(songId: SongId) {
        logger.debug { "processCollaborations: songId = $songId" }
        if (transaction { SongEntity[songId].mintingStatus } == MintingStatus.AwaitingCollaboratorApproval) {
            val collaborations =
                collaborationRepository.getAllBySongId(songId).filter { it.royaltyRate.orZero() > BigDecimal.ZERO }
            val allAccepted = collaborations.all { it.status == CollaborationStatus.Accepted }
            if (allAccepted) {
                updateSongMintingStatus(songId, MintingStatus.ReadyToDistribute)
            } else {
                collaborations.filter { it.status != CollaborationStatus.Accepted }.forEach {
                    logger.info { "AwaitingCollaboratorApproval ($songId): ${it.email} - ${it.status}" }
                }
            }
        }
    }

    override suspend fun getMintingPaymentEstimate(collaborators: Int): MintPaymentResponse {
        logger.debug { "getMintingPaymentEstimate: collaborators = $collaborators" }
        val mintCostBase = configRepository.getLong(CONFIG_KEY_MINT_PRICE)
        // defined in whole usd cents with 6 decimals
        val dspPriceUsd = configRepository.getLong(CONFIG_KEY_DISTRIBUTION_PRICE_USD)
        val minUtxo: Long = cardanoRepository.queryStreamTokenMinUtxo()
        val usdAdaExchangeRate = cardanoRepository.queryAdaUSDPrice().toBigInteger()

        return calculateMintPaymentResponse(
            minUtxo,
            collaborators,
            dspPriceUsd,
            usdAdaExchangeRate,
            mintCostBase,
        )
    }

    override suspend fun getMintingPaymentAmount(
        songId: SongId,
        requesterId: UserId
    ): MintPaymentResponse {
        logger.debug { "getMintingPaymentAmount: songId = $songId" }
        // TODO: We might need to change this code in the future if we're charging NEWM tokens in addition to ada
        val numberOfCollaborators =
            collaborationRepository
                .getAllBySongId(songId)
                .count { it.royaltyRate.orZero() > BigDecimal.ZERO }
        val mintCostBase = configRepository.getLong(CONFIG_KEY_MINT_PRICE)
        // defined in whole usd cents with 6 decimals
        val dspPriceUsd = configRepository.getLong(CONFIG_KEY_DISTRIBUTION_PRICE_USD)
        val minUtxo: Long = cardanoRepository.queryStreamTokenMinUtxo()
        val usdAdaExchangeRate = cardanoRepository.queryAdaUSDPrice().toBigInteger()

        return calculateMintPaymentResponse(
            minUtxo,
            numberOfCollaborators,
            dspPriceUsd,
            usdAdaExchangeRate,
            mintCostBase,
        ).also {
            // Save the total cost to distribute and mint to the database
            val totalCostLovelace = BigDecimal(it.adaPrice!!).movePointRight(6).toLong()
            update(songId, Song(mintCostLovelace = totalCostLovelace))

            // Save the receipt to the database
            saveOrUpdateReceipt(songId, it)
        }
    }

    @VisibleForTesting
    internal fun calculateMintPaymentResponse(
        minUtxo: Long,
        numberOfCollaborators: Int,
        dspPriceUsd: Long,
        usdAdaExchangeRate: BigInteger,
        mintCostBase: Long,
    ): MintPaymentResponse {
        val changeAmountLovelace = 1000000L // 1 ada
        val dspPriceLovelace =
            dspPriceUsd
                .toBigDecimal()
                .divide(usdAdaExchangeRate.toBigDecimal(), 6, RoundingMode.CEILING)
                .times(1000000.toBigDecimal())
                .toBigInteger()

        val sendTokenFee = (numberOfCollaborators * minUtxo)
        val mintCostLovelace = mintCostBase + sendTokenFee

        // usdPrice does not include the extra changeAmountLovelace that we request the wallet to provide as it
        // is returned to the user.
        val usdPrice =
            usdAdaExchangeRate * (mintCostLovelace.toBigInteger() + dspPriceLovelace) / 1000000.toBigInteger()

        val mintPriceUsd = usdAdaExchangeRate * mintCostBase.toBigInteger() / 1000000.toBigInteger()
        val sendTokenFeeUsd = usdAdaExchangeRate * sendTokenFee.toBigInteger() / 1000000.toBigInteger()
        val sendTokenFeePerArtistUsd = usdAdaExchangeRate * minUtxo.toBigInteger() / 1000000.toBigInteger()

        // we send an extra changeAmountLovelace to ensure we have enough ada to cover a return utxo
        return MintPaymentResponse(
            cborHex =
                CborInteger
                    .create(mintCostLovelace + dspPriceLovelace.toLong() + changeAmountLovelace)
                    .toCborByteArray()
                    .toHexString(),
            adaPrice = (mintCostLovelace.toBigInteger() + dspPriceLovelace).toAdaString(),
            usdPrice = usdPrice.toAdaString(),
            dspPriceAda = dspPriceLovelace.toAdaString(),
            dspPriceUsd = dspPriceUsd.toBigInteger().toAdaString(),
            mintPriceAda = mintCostBase.toBigInteger().toAdaString(),
            mintPriceUsd = mintPriceUsd.toAdaString(),
            collabPriceAda = sendTokenFee.toBigInteger().toAdaString(),
            collabPriceUsd = sendTokenFeeUsd.toAdaString(),
            collabPerArtistPriceAda = minUtxo.toBigInteger().toAdaString(),
            collabPerArtistPriceUsd = sendTokenFeePerArtistUsd.toAdaString(),
            usdAdaExchangeRate = usdAdaExchangeRate.toAdaString(),
        )
    }

    override suspend fun generateMintingPaymentTransaction(
        songId: SongId,
        requesterId: UserId,
        sourceUtxos: List<Utxo>,
        changeAddress: String
    ): String {
        logger.debug { "generateMintingPaymentTransaction: songId = $songId" }

        checkRequester(songId, requesterId, verified = true)

        val song = get(songId)
        val key = Key.generateNew()
        val keyId = cardanoRepository.saveKey(key)
        val transaction =
            cardanoRepository.buildTransaction {
                this.sourceUtxos.addAll(sourceUtxos)
                this.outputUtxos.add(
                    outputUtxo {
                        address = key.address
                        lovelace = song.mintCostLovelace.toString()
                    }
                )
                this.changeAddress = changeAddress
            }

        update(songId, Song(paymentKeyId = keyId))
        updateSongMintingStatus(songId, MintingStatus.MintingPaymentRequested)
        return transaction.transactionCbor.toByteArray().toHexString()
    }

    override suspend fun refundMintingPayment(
        songId: SongId,
        walletAddress: String
    ): RefundPaymentResponse {
        logger.debug { "refundMintingPayment: songId = $songId" }

        val song = get(songId)
        val keyId = song.paymentKeyId ?: throw HttpUnprocessableEntityException("missing paymentKeyId")
        val cashRegisterKey =
            requireNotNull(cardanoRepository.getKeyByName("cashRegister")) { "cashRegister key not defined!" }
        val cashRegisterUtxos =
            cardanoRepository
                .queryLiveUtxos(cashRegisterKey.address)
                .filter { it.nativeAssetsCount == 0 }
                .sortedByDescending { it.lovelace.toLong() }
                .take(5)
        require(cashRegisterUtxos.isNotEmpty()) { "cashRegister has no utxos!" }
        val paymentKey = cardanoRepository.getKey(keyId)
        val paymentKeyUtxos = cardanoRepository.queryLiveUtxos(paymentKey.address)
        val transaction =
            cardanoRepository.buildTransaction {
                with(sourceUtxos) {
                    addAll(paymentKeyUtxos)
                    addAll(cashRegisterUtxos)
                }

                with(outputUtxos) {
                    add(
                        outputUtxo {
                            address = walletAddress
                            lovelace = song.mintCostLovelace.toString()
                        }
                    )
                }

                changeAddress = cashRegisterKey.address
                with(signingKeys) {
                    add(paymentKey.toSigningKey())
                    add(cashRegisterKey.toSigningKey())
                }
            }
        val submitTransactionResponse = cardanoRepository.submitTransaction(transaction.transactionCbor)
        if (submitTransactionResponse.result != "MsgAcceptTx") {
            throw HttpUnprocessableEntityException("Failed to submit transaction: $submitTransactionResponse")
        }
        update(songId, Song(mintingStatus = MintingStatus.Declined, mintingTxId = submitTransactionResponse.txId))
        return RefundPaymentResponse(submitTransactionResponse.txId, submitTransactionResponse.result)
    }

    override suspend fun updateSongMintingStatus(
        songId: SongId,
        mintingStatus: MintingStatus,
        errorMessage: String
    ) {
        // Update DB
        update(
            songId,
            Song(
                mintingStatus = mintingStatus,
                errorMessage = errorMessage,
            )
        )

// TODO: This code is causing a crash - needs to be tested/fixed before re-enabling
//        mintingRepository.add(
//            MintingStatusTransactionEntity.new {
//                this.mintingStatus = mintingStatus.name
//                this.songId = EntityID(songId, SongTable)
//                this.logMessage = errorMessage
//                this.createdAt = createdAt
//            }
//        )

        if (errorMessage.isNotBlank()) {
            // We want to record a history of errors so that they are not lost even after a song is reprocessed and
            // the error is cleared.
            transaction {
                SongErrorHistoryTable.insert { row ->
                    row[this.id] = UUID.randomUUID()
                    row[this.createdAt] = LocalDateTime.now()
                    row[this.songId] = songId
                    row[this.errorMessage] = errorMessage
                }
            }
        }

        when (mintingStatus) {
            MintingStatus.MintingPaymentSubmitted,
            MintingStatus.MintingPaymentReceived,
            MintingStatus.AwaitingAudioEncoding,
            MintingStatus.AwaitingCollaboratorApproval,
            MintingStatus.ReadyToDistribute,
            MintingStatus.SubmittedForDistribution,
            MintingStatus.Distributed,
            MintingStatus.Pending,
            MintingStatus.Minted -> {
                // Update SQS
                val messageToSend =
                    json.encodeToString(
                        MintingStatusSqsMessage(
                            songId = songId,
                            mintingStatus = mintingStatus
                        )
                    )
                logger.info { "sending: $messageToSend" }
                SendMessageRequest
                    .builder()
                    .queueUrl(queueUrl)
                    .messageBody(messageToSend)
                    .build()
                    .await()
                logger.info { "sent: $messageToSend" }
            }

            else -> {}
        }

        when (mintingStatus) {
            MintingStatus.MintingPaymentSubmitted -> {
                if (transaction { SongEntity[songId].audioEncodingStatus } == AudioEncodingStatus.Completed) {
                    sendMintingStartedNotification(songId)
                }
            }

            MintingStatus.Minted -> {
                logger.info { "Minted song $songId SUCCESS!" }
                sendMintingNotification("succeeded", songId)
            }

            MintingStatus.MintingPaymentTimeout,
            MintingStatus.MintingPaymentException,
            MintingStatus.DistributionException,
            MintingStatus.SubmittedForDistributionException,
            MintingStatus.ArweaveUploadException,
            MintingStatus.MintingException,
            MintingStatus.ReleaseCheckException -> {
                logger.info { "Minting song $songId FAILED with $mintingStatus" }
                sendMintingNotification("failed", songId)
            }

            MintingStatus.Declined -> {
                logger.info { "Minting song $songId DECLINED!" }
                sendMintingNotification("declined", songId)
            }

            MintingStatus.Released -> {
                logger.info { "Released song $songId SUCCESS!" }
                sendMintingNotification("released", songId)
            }

            else -> Unit
        }
    }

    override fun saveOrUpdateReceipt(
        songId: SongId,
        mintPaymentResponse: MintPaymentResponse
    ) {
        logger.debug { "saveOrUpdateReceipt: songId = $songId" }
        transaction {
            SongReceiptEntity.find { SongReceiptTable.songId eq songId }.firstOrNull()?.let { receipt ->
                // Update existing receipt
                receipt.createdAt = LocalDateTime.now()
                receipt.adaPrice = BigDecimal(mintPaymentResponse.adaPrice).movePointRight(6).toLong()
                receipt.usdPrice = BigDecimal(mintPaymentResponse.usdPrice).movePointRight(6).toLong()
                receipt.adaDspPrice = BigDecimal(mintPaymentResponse.dspPriceAda).movePointRight(6).toLong()
                receipt.usdDspPrice = BigDecimal(mintPaymentResponse.dspPriceUsd).movePointRight(6).toLong()
                receipt.adaMintPrice = BigDecimal(mintPaymentResponse.mintPriceAda).movePointRight(6).toLong()
                receipt.usdMintPrice = BigDecimal(mintPaymentResponse.mintPriceUsd).movePointRight(6).toLong()
                receipt.adaCollabPrice = BigDecimal(mintPaymentResponse.collabPriceAda).movePointRight(6).toLong()
                receipt.usdCollabPrice = BigDecimal(mintPaymentResponse.collabPriceUsd).movePointRight(6).toLong()
                receipt.usdAdaExchangeRate =
                    BigDecimal(mintPaymentResponse.usdAdaExchangeRate).movePointRight(6).toLong()
            } ?: run {
                // Create new receipt
                SongReceiptEntity.new {
                    this.songId = EntityID(songId, SongTable)
                    createdAt = LocalDateTime.now()
                    adaPrice = BigDecimal(mintPaymentResponse.adaPrice).movePointRight(6).toLong()
                    usdPrice = BigDecimal(mintPaymentResponse.usdPrice).movePointRight(6).toLong()
                    adaDspPrice = BigDecimal(mintPaymentResponse.dspPriceAda).movePointRight(6).toLong()
                    usdDspPrice = BigDecimal(mintPaymentResponse.dspPriceUsd).movePointRight(6).toLong()
                    adaMintPrice = BigDecimal(mintPaymentResponse.mintPriceAda).movePointRight(6).toLong()
                    usdMintPrice = BigDecimal(mintPaymentResponse.mintPriceUsd).movePointRight(6).toLong()
                    adaCollabPrice = BigDecimal(mintPaymentResponse.collabPriceAda).movePointRight(6).toLong()
                    usdCollabPrice = BigDecimal(mintPaymentResponse.collabPriceUsd).movePointRight(6).toLong()
                    usdAdaExchangeRate = BigDecimal(mintPaymentResponse.usdAdaExchangeRate).movePointRight(6).toLong()
                }
            }
        }
    }

    override suspend fun getSmartLinks(songId: SongId): List<SongSmartLink> {
        logger.debug { "getSmartLinks: songId = $songId" }
        val ttl = configRepository.getLong(CONFIG_KEY_SONG_SMART_LINKS_CACHE_TTL)
        val minCreatedAt = if (ttl > 0) {
            LocalDateTime
                .now()
                .minusSeconds(ttl)
        } else {
            // cache never expires
            LocalDateTime.MIN
        }
        val cachedSmartLinks = transaction {
            with(SongSmartLinkEntity.findBySongId(songId)) {
                takeIf { any { it.createdAt >= minCreatedAt } } ?: run {
                    forEach { it.delete() }
                    null
                }
            }
        }
        if (!cachedSmartLinks.isNullOrEmpty()) {
            logger.debug { "Found ${cachedSmartLinks.size} cached smart-links" }
            return cachedSmartLinks.map { it.toModel() }
        }
        val networkSmartLinks = if (configRepository.getBoolean(CONFIG_KEY_SONG_SMART_LINKS_USE_DISTRIBUTOR)) {
            val (distributionUserId, distributionReleaseId) = transaction {
                SongEntity[songId].run {
                    UserEntity[ownerId].distributionUserId to releaseId?.let { ReleaseEntity[it].distributionReleaseId }
                }
            }
            if (distributionUserId == null || distributionReleaseId == null) {
                logger.debug { "No distribution: userId = $distributionUserId, releaseId = $distributionReleaseId" }
                return emptyList()
            }
            distributionRepository.getSmartLinks(distributionUserId, distributionReleaseId)
        } else {
            outletReleaseRepository.getSmartLinks(songId)
        }.filter { it.url.isNotEmpty() }
        logger.debug { "Found ${networkSmartLinks.size} network smart-links" }
        return transaction {
            val songEntityId = EntityID(songId, SongTable)
            networkSmartLinks.map {
                SongSmartLinkEntity.new {
                    this.songId = songEntityId
                    this.storeName = it.storeName
                    this.url = it.url
                }
            }
        }.map { it.toModel() }
    }

    private suspend fun sendMintingStartedNotification(songId: SongId) {
        collaborationRepository.invite(songId)
        sendMintingNotification("started", songId)
    }

    private suspend fun sendMintingNotification(
        path: String,
        songId: SongId
    ) {
        val (release, song, owner) =
            transaction {
                val song = SongEntity[songId]
                val release = ReleaseEntity[song.releaseId!!]
                val owner = UserEntity[song.ownerId]
                Triple(release, song, owner)
            }

        val collaborations =
            collaborationRepository
                .getAllBySongId(song.id.value)
                .filter { it.royaltyRate.orZero() > BigDecimal.ZERO }

        val collaborators =
            collaborationRepository.getCollaborators(
                userId = owner.id.value,
                filters = CollaboratorFilters(emails = FilterCriteria(includes = collaborations.mapNotNull { it.email })),
                offset = 0,
                limit = Int.MAX_VALUE
            )

        emailRepository.send(
            to = owner.email,
            subject = environment.getConfigString("mintingNotifications.$path.subject"),
            messageUrl = environment.getConfigString("mintingNotifications.$path.messageUrl"),
            messageArgs =
                mapOf(
                    "owner" to owner.stageOrFullName,
                    "song" to song.title,
                    "collabs" to
                        collaborations.joinToString(separator = "") { collaboration ->
                            "<li>${
                                collaborators
                                    .firstOrNull { it.email.equals(collaboration.email, ignoreCase = true) }
                                    ?.user
                                    ?.stageOrFullName ?: collaboration.email
                            }: ${collaboration.royaltyRate}%</li>"
                        },
                    "errors" to release.errorMessage.orEmpty()
                )
        )
    }

    override suspend fun distribute(songId: SongId) {
        val song = fixupSongCopyrights(get(songId))
        val release = transaction { ReleaseEntity[song.releaseId!!].toModel() }

        distributionRepository.distributeRelease(release)
    }

    override suspend fun redistribute(songId: SongId) {
        val song = fixupSongCopyrights(get(songId))
        val release = transaction { ReleaseEntity[song.releaseId!!].toModel() }

        distributionRepository.redistributeRelease(release)
    }

    private suspend fun fixupSongCopyrights(song: Song): Song {
        val user = transaction { UserEntity[song.ownerId!!].toModel() }
        // update with default user's company, stagename, or fullname and current year
        val thisYear = Instant.now().atZone(ZoneId.systemDefault()).year
        val updatedSong =
            song.copy(
                compositionCopyrightOwner =
                    song.compositionCopyrightOwner ?: user.companyOrStageOrFullName,
                compositionCopyrightYear =
                    song.compositionCopyrightYear ?: thisYear,
                phonographicCopyrightOwner =
                    song.phonographicCopyrightOwner ?: user.companyOrStageOrFullName,
                phonographicCopyrightYear =
                    song.phonographicCopyrightYear ?: thisYear,
            )

        update(song.id!!, updatedSong)
        return updatedSong
    }

    private fun checkRequester(
        songId: SongId,
        requesterId: UserId,
        verified: Boolean = false
    ) = transaction {
        SongEntity[songId].checkRequester(requesterId, verified)
    }

    private fun SongEntity.checkRequester(
        requesterId: UserId,
        verified: Boolean = false
    ) {
        if (ownerId.value != requesterId) throw HttpForbiddenException("operation allowed only by owner")
        if (verified && UserEntity[requesterId].verificationStatus != UserVerificationStatus.Verified) {
            throw HttpUnprocessableEntityException("operation allowed only after owner is KYC verified")
        }
    }

    private fun ReleaseEntity.checkRequester(
        requesterId: UserId,
        verified: Boolean = false
    ) {
        if (ownerId.value != requesterId) throw HttpForbiddenException("operation allowed only by owner")
        if (verified && UserEntity[requesterId].verificationStatus != UserVerificationStatus.Verified) {
            throw HttpUnprocessableEntityException("operation allowed only after owner is KYC verified")
        }
    }

    private fun String.checkTitleUnique(
        ownerId: UserId,
        mintingStatus: MintingStatus?
    ) {
        if (SongEntity.exists(ownerId, this) && mintingStatus != MintingStatus.Undistributed) {
            throw HttpConflictException("Title already exists: $this")
        }
    }

    private fun Song.checkFieldLengths() {
        title?.checkLength("title")
        genres?.forEachIndexed { index, genre -> genre.checkLength("genres$index") }
        moods?.forEachIndexed { index, mood -> mood.checkLength("moods$index") }
        description?.checkLength("description", 250)
        language?.checkLength("language")
        compositionCopyrightOwner?.checkLength("compositionCopyrightOwner")
        phonographicCopyrightOwner?.checkLength("phonographicCopyrightOwner")
        parentalAdvisory?.checkLength("parentalAdvisory")
        barcodeNumber?.checkLength("barcodeNumber")
        isrc?.checkLength("isrc")
        iswc?.checkLength("iswc")
        ipis?.forEachIndexed { index, ipi -> ipi.checkLength("ipi$index") }
        nftName?.checkLength("nftName")
    }
}
