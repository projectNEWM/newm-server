package io.newm.server.features.minting

import com.amazonaws.services.sqs.model.Message
import io.newm.chain.grpc.monitorPaymentAddressRequest
import io.newm.server.aws.SqsMessageReceiver
import io.newm.server.config.repo.ConfigRepository
import io.newm.server.config.repo.ConfigRepository.Companion.CONFIG_KEY_EVEARA_STATUS_CHECK_MINUTES
import io.newm.server.config.repo.ConfigRepository.Companion.CONFIG_KEY_MINT_MONITOR_PAYMENT_ADDRESS_TIMEOUT_MIN
import io.newm.server.features.arweave.repo.ArweaveRepository
import io.newm.server.features.cardano.repo.CardanoRepository
import io.newm.server.features.daemon.QuartzSchedulerDaemon
import io.newm.server.features.minting.repo.MintingRepository
import io.newm.server.features.scheduler.EvearaReleaseStatusJob
import io.newm.server.features.song.model.MintingStatus
import io.newm.server.features.song.model.Song
import io.newm.server.features.song.repo.SongRepository
import io.newm.server.logging.captureToSentry
import io.newm.shared.koin.inject
import io.newm.shared.ktx.info
import io.newm.shared.ktx.warn
import kotlinx.serialization.json.Json
import org.koin.core.parameter.parametersOf
import org.quartz.JobBuilder.newJob
import org.quartz.JobKey
import org.quartz.SimpleScheduleBuilder.simpleSchedule
import org.quartz.TriggerBuilder.newTrigger
import org.slf4j.Logger
import kotlin.time.Duration.Companion.minutes

class MintingMessageReceiver : SqsMessageReceiver {
    private val log: Logger by inject { parametersOf(javaClass.simpleName) }
    private val songRepository: SongRepository by inject()
    private val cardanoRepository: CardanoRepository by inject()
    private val arweaveRepository: ArweaveRepository by inject()
    private val mintingRepository: MintingRepository by inject()
    private val configRepository: ConfigRepository by inject()
    private val quartzSchedulerDaemon: QuartzSchedulerDaemon by inject()
    private val json: Json by inject()

    override suspend fun onMessageReceived(message: Message) {
        val mintingStatusSqsMessage: MintingStatusSqsMessage = json.decodeFromString(message.body)
        log.info { "received: $mintingStatusSqsMessage" }
        val dbSong = songRepository.get(mintingStatusSqsMessage.songId)
        if (dbSong.mintingStatus != mintingStatusSqsMessage.mintingStatus) {
            // sometimes, we will send an SQS message manually to re-process a given song. This is fine, but we need to
            // sync the DB with the SQS message.
            log.warn { "DB MintingStatus: ${dbSong.mintingStatus} does not match SQS MintingStatus: ${mintingStatusSqsMessage.mintingStatus}... updating db" }
            songRepository.update(mintingStatusSqsMessage.songId, Song(mintingStatus = mintingStatusSqsMessage.mintingStatus))
        }

        when (mintingStatusSqsMessage.mintingStatus) {
            MintingStatus.MintingPaymentSubmitted -> {
                try {
                    val song = songRepository.get(mintingStatusSqsMessage.songId)
                    val paymentKey = cardanoRepository.getKey(song.paymentKeyId!!)
                    val response = cardanoRepository.awaitPayment(
                        monitorPaymentAddressRequest {
                            address = paymentKey.address
                            lovelace = song.mintCostLovelace!!.toString()
                            timeoutMs =
                                configRepository.getLong(CONFIG_KEY_MINT_MONITOR_PAYMENT_ADDRESS_TIMEOUT_MIN).minutes.inWholeMilliseconds
                        }
                    )
                    if (response.success) {
                        // We got paid!!! Move -> MintingPaymentReceived
                        songRepository.updateSongMintingStatus(
                            songId = mintingStatusSqsMessage.songId,
                            mintingStatus = MintingStatus.MintingPaymentReceived
                        )
                    } else {
                        // We timed out waiting for payment.
                        songRepository.updateSongMintingStatus(
                            songId = mintingStatusSqsMessage.songId,
                            mintingStatus = MintingStatus.MintingPaymentTimeout
                        )
                    }
                } catch (e: Throwable) {
                    val errorMessage = "Error while waiting for payment!"
                    log.error(errorMessage, e)
                    songRepository.updateSongMintingStatus(
                        songId = mintingStatusSqsMessage.songId,
                        mintingStatus = MintingStatus.MintingPaymentException
                    )
                    throw DistributeAndMintException(errorMessage, e).also { it.captureToSentry() }
                }
            }

            MintingStatus.MintingPaymentReceived -> {
                // Payment received. Move -> AwaitingAudioEncoding
                songRepository.updateSongMintingStatus(
                    songId = mintingStatusSqsMessage.songId,
                    mintingStatus = MintingStatus.AwaitingAudioEncoding
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
                    songRepository.distribute(mintingStatusSqsMessage.songId)

                    // Done submitting distributing. Move -> SubmittedForDistribution
                    songRepository.updateSongMintingStatus(
                        songId = mintingStatusSqsMessage.songId,
                        mintingStatus = MintingStatus.SubmittedForDistribution
                    )
                } catch (e: Throwable) {
                    val errorMessage = "Error while distributing!"
                    log.error(errorMessage, e)
                    songRepository.updateSongMintingStatus(
                        songId = mintingStatusSqsMessage.songId,
                        mintingStatus = MintingStatus.DistributionException
                    )
                    throw DistributeAndMintException(errorMessage, e).also { it.captureToSentry() }
                }
            }

            MintingStatus.SubmittedForDistribution -> {
                try {
                    // Schedule a job to check the release status every 24 hours
                    val song = songRepository.get(mintingStatusSqsMessage.songId)

                    val jobKey = JobKey("EvearaReleaseStatusJob-${song.id}", "EvearaReleaseStatusJobGroup")
                    val jobDetail = newJob(EvearaReleaseStatusJob::class.java)
                        .withIdentity(jobKey)
                        .usingJobData("songId", song.id.toString())
                        .usingJobData("userId", song.ownerId.toString())
                        .requestRecovery(true)
                        .build()
                    val trigger = newTrigger()
                        .forJob(jobDetail)
                        .withSchedule(
                            simpleSchedule()
                                .withIntervalInMinutes(configRepository.getInt(CONFIG_KEY_EVEARA_STATUS_CHECK_MINUTES))
                                .repeatForever()
                        )
                        .build()

                    quartzSchedulerDaemon.scheduleJob(jobDetail, trigger)

                    if (!cardanoRepository.isMainnet()) {
                        // If we are on testnet, pretend that the song is already successfully distributed
                        songRepository.update(song.id!!, Song(forceDistributed = true))
                    }
                } catch (e: Throwable) {
                    val errorMessage = "Error while creating distribution check job!"
                    log.error(errorMessage, e)
                    songRepository.updateSongMintingStatus(
                        songId = mintingStatusSqsMessage.songId,
                        mintingStatus = MintingStatus.SubmittedForDistributionException
                    )
                    throw DistributeAndMintException(errorMessage, e).also { it.captureToSentry() }
                }
            }

            MintingStatus.Distributed -> {
                try {
                    // Upload 30-second clip, lyrics.txt, streamtokenagreement.pdf, coverArt to arweave and save those URLs
                    // on the Song record
                    val song = songRepository.get(mintingStatusSqsMessage.songId)
                    arweaveRepository.uploadSongAssets(song)

                    // Done with arweave. Move -> Pending for minting
                    songRepository.updateSongMintingStatus(
                        songId = mintingStatusSqsMessage.songId,
                        mintingStatus = MintingStatus.Pending
                    )
                } catch (e: Throwable) {
                    val errorMessage = "Error while uploading song assets to arweave!"
                    log.error(errorMessage, e)
                    songRepository.updateSongMintingStatus(
                        songId = mintingStatusSqsMessage.songId,
                        mintingStatus = MintingStatus.ArweaveUploadException
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
                        song = Song(
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
                    log.error(errorMessage, e)
                    songRepository.updateSongMintingStatus(
                        songId = mintingStatusSqsMessage.songId,
                        mintingStatus = MintingStatus.MintingException
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
