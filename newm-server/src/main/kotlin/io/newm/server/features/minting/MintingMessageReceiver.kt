package io.newm.server.features.minting

import io.github.oshai.kotlinlogging.KotlinLogging
import io.newm.chain.grpc.monitorPaymentAddressRequest
import io.newm.server.aws.SqsMessageReceiver
import io.newm.server.config.repo.ConfigRepository
import io.newm.server.config.repo.ConfigRepository.Companion.CONFIG_KEY_EVEARA_STATUS_CHECK_MINUTES
import io.newm.server.config.repo.ConfigRepository.Companion.CONFIG_KEY_MINT_MONITOR_PAYMENT_ADDRESS_TIMEOUT_MIN
import io.newm.server.config.repo.ConfigRepository.Companion.CONFIG_KEY_OUTLET_STATUS_CHECK_MINUTES
import io.newm.server.features.arweave.repo.ArweaveRepository
import io.newm.server.features.cardano.repo.CardanoRepository
import io.newm.server.features.daemon.QuartzSchedulerDaemon
import io.newm.server.features.minting.repo.MintingRepository
import io.newm.server.features.scheduler.EvearaReleaseStatusJob
import io.newm.server.features.scheduler.OutletReleaseStatusJob
import io.newm.server.features.song.model.MintingStatus
import io.newm.server.features.song.model.Song
import io.newm.server.features.song.repo.SongRepository
import io.newm.server.logging.captureToSentry
import io.newm.shared.koin.inject
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.quartz.JobBuilder.newJob
import org.quartz.JobKey
import org.quartz.SimpleScheduleBuilder.simpleSchedule
import org.quartz.TriggerBuilder.newTrigger
import software.amazon.awssdk.services.sqs.model.Message

class MintingMessageReceiver : SqsMessageReceiver {
    private val log = KotlinLogging.logger {}
    private val songRepository: SongRepository by inject()
    private val cardanoRepository: CardanoRepository by inject()
    private val arweaveRepository: ArweaveRepository by inject()
    private val mintingRepository: MintingRepository by inject()
    private val configRepository: ConfigRepository by inject()
    private val quartzSchedulerDaemon: QuartzSchedulerDaemon by inject()
    private val json: Json by inject()

    override suspend fun onMessageReceived(message: Message) {
        log.info { "received: ${message.body()}" }
        val mintingStatusSqsMessage: MintingStatusSqsMessage = json.decodeFromString(message.body())
        val dbSong = songRepository.get(mintingStatusSqsMessage.songId)
        if (dbSong.mintingStatus == MintingStatus.Released) {
            // Sometimes, we will manually reprocess a song. If it is already minted & released successfully when we do
            // dead-letter queue reprocessing, we can safely ignore these messages and let them be successfully
            // consumed.
            log.info { "DB MintingStatus: ${dbSong.mintingStatus} is already Released. Ignoring SQS message." }
            return
        }
        if (dbSong.mintingStatus != mintingStatusSqsMessage.mintingStatus) {
            // sometimes, we will send an SQS message manually to re-process a given song. This is fine, but we need to
            // sync the DB with the SQS message.
            log.warn {
                "DB MintingStatus: ${dbSong.mintingStatus}(${dbSong.mintingStatus?.ordinal}) does not match SQS MintingStatus: ${mintingStatusSqsMessage.mintingStatus}(${mintingStatusSqsMessage.mintingStatus.ordinal})... updating db"
            }
            songRepository.update(
                mintingStatusSqsMessage.songId,
                Song(
                    mintingStatus = mintingStatusSqsMessage.mintingStatus,
                    errorMessage = "",
                )
            )
        }

        when (mintingStatusSqsMessage.mintingStatus) {
            MintingStatus.MintingPaymentSubmitted -> {
                coroutineScope {
                    // Monitor for payment in a new coroutine so we can continue
                    // to process other song messages from the SQS Queue.
                    launch {
                        try {
                            val song = songRepository.get(mintingStatusSqsMessage.songId)
                            val paymentKey = cardanoRepository.getKey(song.paymentKeyId!!)
                            val response =
                                cardanoRepository.awaitPayment(
                                    monitorPaymentAddressRequest {
                                        address = paymentKey.address
                                        lovelace = song.mintCostLovelace!!.toString()
                                        timeoutMs =
                                            configRepository
                                                .getLong(
                                                    CONFIG_KEY_MINT_MONITOR_PAYMENT_ADDRESS_TIMEOUT_MIN
                                                ).minutes.inWholeMilliseconds
                                    }.also {
                                        log.info {
                                            "Awaiting payment for song: ${song.id} on address: ${it.address} for ${it.lovelace} lovelace."
                                        }
                                    }
                                )
                            if (response.success) {
                                log.info { "Payment received for song: ${song.id}" }
                                // We got paid!!! Move -> MintingPaymentReceived
                                songRepository.updateSongMintingStatus(
                                    songId = mintingStatusSqsMessage.songId,
                                    mintingStatus = MintingStatus.MintingPaymentReceived
                                )
                            } else {
                                // We timed out waiting for payment.
                                val errorMessage = "Timed out waiting for payment!: ${response.message}"
                                songRepository.updateSongMintingStatus(
                                    songId = mintingStatusSqsMessage.songId,
                                    mintingStatus = MintingStatus.MintingPaymentTimeout,
                                    errorMessage = errorMessage,
                                )
                            }
                        } catch (e: Throwable) {
                            val errorMessage = "Error while waiting for payment!"
                            log.error(e) { errorMessage }
                            songRepository.updateSongMintingStatus(
                                songId = mintingStatusSqsMessage.songId,
                                mintingStatus = MintingStatus.MintingPaymentException,
                                errorMessage = "$errorMessage: ${e.message}",
                            )
                        }
                    }
                }
            }

            MintingStatus.MintingPaymentReceived -> {
                // Payment received. Move -> AwaitingAudioEncoding
                songRepository.updateSongMintingStatus(
                    songId = mintingStatusSqsMessage.songId,
                    mintingStatus = MintingStatus.AwaitingAudioEncoding,
                )
            }

            MintingStatus.AwaitingAudioEncoding -> {
                songRepository.processAudioEncoding(mintingStatusSqsMessage.songId)
            }

            MintingStatus.AwaitingCollaboratorApproval -> {
                songRepository.processCollaborations(mintingStatusSqsMessage.songId)
            }

            MintingStatus.ReadyToDistribute -> {
                try {
                    if (!cardanoRepository.isMainnet()) {
                        // force a track into the “An error occurred” status when I attempt to distribute a song in the TestNet environment
                        val song = songRepository.get(mintingStatusSqsMessage.songId)
                        if (song.title?.contains("[ForceError]", true) == true) {
                            throw ForceTrackToFailException("An error was forced to occur for testing purposes")
                        }
                    }
                    songRepository.distribute(mintingStatusSqsMessage.songId)

                    // Done submitting distributing. Move -> SubmittedForDistribution
                    songRepository.updateSongMintingStatus(
                        songId = mintingStatusSqsMessage.songId,
                        mintingStatus = MintingStatus.SubmittedForDistribution,
                    )
                } catch (e: Throwable) {
                    val errorMessage = "Error while distributing!"
                    log.error(e) { errorMessage }
                    songRepository.updateSongMintingStatus(
                        songId = mintingStatusSqsMessage.songId,
                        mintingStatus = MintingStatus.DistributionException,
                        errorMessage = "$errorMessage: ${e.message}",
                    )
                    throw DistributeAndMintException(errorMessage, e).also { it.captureToSentry() }
                }
            }

            MintingStatus.SubmittedForDistribution -> {
                try {
                    // Schedule a job to check the release status every 24 hours
                    val song = songRepository.get(mintingStatusSqsMessage.songId)

                    val jobKey = JobKey("EvearaReleaseStatusJob-${song.id}", "EvearaReleaseStatusJobGroup")
                    if (quartzSchedulerDaemon.jobExists(jobKey)) {
                        log.warn { "Job $jobKey is already scheduled" }
                        return
                    }
                    val jobDetail =
                        newJob(EvearaReleaseStatusJob::class.java)
                            .withIdentity(jobKey)
                            .usingJobData("songId", song.id.toString())
                            .usingJobData("userId", song.ownerId.toString())
                            .requestRecovery(true)
                            .build()
                    val trigger =
                        newTrigger()
                            .forJob(jobDetail)
                            .withSchedule(
                                simpleSchedule()
                                    .withIntervalInMinutes(
                                        configRepository.getInt(
                                            CONFIG_KEY_EVEARA_STATUS_CHECK_MINUTES
                                        )
                                    ).repeatForever()
                            ).build()

                    quartzSchedulerDaemon.scheduleJob(jobDetail, trigger)

                    if (!cardanoRepository.isMainnet()) {
                        // If we are on testnet, pretend that the song is already successfully distributed
                        if (song.title?.contains("[NoForce]", true) != true) {
                            songRepository.update(song.id!!, Song(forceDistributed = true))
                        }
                    }
                } catch (e: Throwable) {
                    val errorMessage = "Error while creating distribution check job!"
                    log.error(e) { errorMessage }
                    songRepository.updateSongMintingStatus(
                        songId = mintingStatusSqsMessage.songId,
                        mintingStatus = MintingStatus.SubmittedForDistributionException,
                        errorMessage = "$errorMessage: ${e.message}",
                    )
                    throw DistributeAndMintException(errorMessage, e).also { it.captureToSentry() }
                }
            }

            MintingStatus.Distributed -> {
                // Now that the song is Distributed, upload the song assets to arweave
                try {
                    val song = songRepository.get(mintingStatusSqsMessage.songId)
                    if (song.nftPolicyId?.isNotBlank() == true && song.nftName?.isNotBlank() == true) {
                        // If we already have a policy id and name manually placed in the db, this song has already
                        // been minted as one of our sample sales. We don't need to do anything else.
                        // Move -> Minted
                        songRepository.updateSongMintingStatus(
                            songId = mintingStatusSqsMessage.songId,
                            mintingStatus = MintingStatus.Minted
                        )
                    } else {
                        // Upload 30-second clip, lyrics.txt, streamtokenagreement.pdf, coverArt to arweave and save those URLs
                        // on the Song record
                        arweaveRepository.uploadSongAssets(song)

                        // Done with arweave. Move -> Pending for minting
                        songRepository.updateSongMintingStatus(
                            songId = mintingStatusSqsMessage.songId,
                            mintingStatus = MintingStatus.Pending
                        )
                    }
                } catch (e: Throwable) {
                    val errorMessage = "Error while uploading song assets to arweave!"
                    log.error(e) { errorMessage }
                    songRepository.updateSongMintingStatus(
                        songId = mintingStatusSqsMessage.songId,
                        mintingStatus = MintingStatus.ArweaveUploadException,
                        errorMessage = "$errorMessage: ${e.message}",
                    )
                    throw DistributeAndMintException(errorMessage, e).also { it.captureToSentry() }
                }
            }

            MintingStatus.Pending -> {
                try {
                    // Create and submit the mint transaction
                    val song = songRepository.get(mintingStatusSqsMessage.songId)
                    val mintInfo = mintingRepository.mint(song)

                    // Update the song record with the minting info
                    songRepository.update(
                        songId = mintingStatusSqsMessage.songId,
                        song =
                            Song(
                                mintingTxId = mintInfo.transactionId,
                                nftPolicyId = mintInfo.policyId,
                                nftName = mintInfo.assetName,
                            )
                    )

                    // Done submitting mint transaction. Move -> Minted
                    songRepository.updateSongMintingStatus(
                        songId = mintingStatusSqsMessage.songId,
                        mintingStatus = MintingStatus.Minted
                    )
                } catch (e: Throwable) {
                    val errorMessage = "Error while minting!"
                    log.error(e) { errorMessage }
                    songRepository.updateSongMintingStatus(
                        songId = mintingStatusSqsMessage.songId,
                        mintingStatus = MintingStatus.MintingException,
                        errorMessage = "$errorMessage: ${e.message}",
                    )
                    throw DistributeAndMintException(errorMessage, e).also { it.captureToSentry() }
                }
            }

            MintingStatus.Minted -> {
                // Now that the song is minted. Wait until it is Released on spotify.
                try {
                    // Schedule a job to check the stream platform release status every 12 hours
                    val song = songRepository.get(mintingStatusSqsMessage.songId)

                    val jobKey = JobKey("OutletReleaseStatusJob-${song.id}", "OutletReleaseStatusJobGroup")
                    if (quartzSchedulerDaemon.jobExists(jobKey)) {
                        log.warn { "Job $jobKey is already scheduled" }
                        return
                    }
                    val jobDetail =
                        newJob(OutletReleaseStatusJob::class.java)
                            .withIdentity(jobKey)
                            .usingJobData("songId", song.id.toString())
                            .requestRecovery(true)
                            .build()
                    val trigger =
                        newTrigger()
                            .forJob(jobDetail)
                            .withSchedule(
                                simpleSchedule()
                                    .withIntervalInMinutes(
                                        configRepository.getInt(
                                            CONFIG_KEY_OUTLET_STATUS_CHECK_MINUTES
                                        )
                                    ).repeatForever()
                            ).build()

                    quartzSchedulerDaemon.scheduleJob(jobDetail, trigger)
                } catch (e: Throwable) {
                    val errorMessage = "Error while creating OutletReleaseStatusJob!"
                    log.error(e) { errorMessage }
                    songRepository.updateSongMintingStatus(
                        songId = mintingStatusSqsMessage.songId,
                        mintingStatus = MintingStatus.ReleaseCheckException,
                        errorMessage = "$errorMessage: ${e.message}",
                    )
                    throw DistributeAndMintException(errorMessage, e).also { it.captureToSentry() }
                }
            }

            else -> {
                throw IllegalStateException("No SQS message expected for MintingStatus: ${mintingStatusSqsMessage.mintingStatus}!")
            }
        }
    }
}
