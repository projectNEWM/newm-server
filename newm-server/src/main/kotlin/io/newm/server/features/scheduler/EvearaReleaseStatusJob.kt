package io.newm.server.features.scheduler

import io.github.oshai.kotlinlogging.KotlinLogging
import io.newm.server.config.repo.ConfigRepository
import io.newm.server.config.repo.ConfigRepository.Companion.CONFIG_KEY_EVEARA_STATUS_CHECK_MINUTES
import io.newm.server.config.repo.ConfigRepository.Companion.CONFIG_KEY_EVEARA_STATUS_CHECK_REFIRE
import io.newm.server.features.cardano.repo.CardanoRepository
import io.newm.server.features.distribution.DistributionRepository
import io.newm.server.features.distribution.model.OutletStatusCode
import io.newm.server.features.song.model.MintingStatus
import io.newm.server.features.song.repo.SongRepository
import io.newm.server.features.user.repo.UserRepository
import io.newm.shared.exception.HttpStatusException
import io.newm.shared.koin.inject
import io.newm.shared.ktx.toUUID
import java.io.EOFException
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlinx.coroutines.runBlocking
import org.quartz.DisallowConcurrentExecution
import org.quartz.Job
import org.quartz.JobExecutionContext
import org.quartz.JobKey

@DisallowConcurrentExecution
class EvearaReleaseStatusJob : Job {
    private val log = KotlinLogging.logger {}
    private val distributionRepository: DistributionRepository by inject()
    private val cardanoRepository: CardanoRepository by inject()
    private val userRepository: UserRepository by inject()
    private val songRepository: SongRepository by inject()
    private val configRepository: ConfigRepository by inject()

    override fun execute(context: JobExecutionContext) {
        log.info {
            "EvearaReleaseStatusJob key: ${context.jobDetail.key.name} executed at ${
                LocalDateTime.ofInstant(
                    context.fireTime.toInstant(),
                    ZoneOffset.UTC
                )
            }"
        }

        runBlocking {
            val userId = context.mergedJobDataMap.getString("userId").toUUID()
            val songId = context.mergedJobDataMap.getString("songId").toUUID()
            try {
                val user = userRepository.get(userId)
                val song = songRepository.get(songId)
                val release = songRepository.getRelease(song.releaseId!!)
                val distributionReleaseStatusResponse =
                    distributionRepository.distributionOutletReleaseStatus(user, release.distributionReleaseId!!)
                val spotifyOutletStatusCode =
                    if (release.forceDistributed == true) {
                        // If the song is force distributed, then we can mark it as disapproved or distributed
                        if (song.title!!.contains("[DistributionFailure]")) {
                            OutletStatusCode.DISAPPROVED
                        } else {
                            OutletStatusCode.DISTRIBUTED
                        }
                    } else {
                        distributionReleaseStatusResponse.outletReleaseStatuses?.find {
                            it.storeName.equals(
                                "Spotify",
                                ignoreCase = true
                            )
                        }?.outletStatus?.statusCode
                    }

                when (spotifyOutletStatusCode) {
                    OutletStatusCode.DISTRIBUTED -> {
                        // At least spotify is distributed, so we can mark the song as distributed
                        songRepository.updateSongMintingStatus(songId, MintingStatus.Distributed)

                        // Cancel this job's future executions
                        context.scheduler.deleteJob(
                            JobKey.jobKey(
                                context.jobDetail.key.name,
                                context.jobDetail.key.group
                            )
                        )
                    }

                    OutletStatusCode.DISAPPROVED -> {
                        // At least spotify is rejected, so we can mark the song as rejected
                        val albumsResponse = distributionRepository.getAlbums(user)
                        if (!albumsResponse.success) {
                            songRepository.updateSongMintingStatus(
                                songId,
                                MintingStatus.Declined,
                                "Failed to get album disapproveMessage: ${albumsResponse.message}"
                            )
                        } else {
                            albumsResponse.albumData.firstOrNull { it.releaseId == release.distributionReleaseId }?.let {
                                songRepository.updateSongMintingStatus(
                                    songId,
                                    MintingStatus.Declined,
                                    it.disapproveMessage
                                )
                            } ?: songRepository.updateSongMintingStatus(
                                songId,
                                MintingStatus.Declined,
                                "Failed to get album disapproveMessage"
                            )
                        }
                        if (context.refireCount *
                            configRepository.getInt(
                                CONFIG_KEY_EVEARA_STATUS_CHECK_MINUTES
                            ) > configRepository.getInt(CONFIG_KEY_EVEARA_STATUS_CHECK_REFIRE)
                        ) {
                            // Cancel this job's future executions
                            context.scheduler.deleteJob(
                                JobKey.jobKey(
                                    context.jobDetail.key.name,
                                    context.jobDetail.key.group
                                )
                            )
                        } else {
                            log.debug { "Keep job ${context.jobDetail.key.name} in the schedule, re-fire count is: ${context.refireCount}" }
                        }
                    }

                    OutletStatusCode.DISTRIBUTE_INITIATED -> {
                        if (!cardanoRepository.isMainnet()) {
                            // We're on testnet, so we can simulate distribution
                            log.info { "Simulating distribution for $songId on testnet" }
                            val response =
                                distributionRepository.simulateDistributeRelease(user, release.distributionReleaseId)
                            log.info { "Simulated distribution response: $response" }
                        }
                        log.info {
                            "Eveara's Spotify distribution status for $songId is $spotifyOutletStatusCode, so we will check again at ${
                                LocalDateTime.ofInstant(
                                    context.nextFireTime.toInstant(),
                                    ZoneOffset.UTC
                                )
                            }"
                        }
                    }

                    else -> {
                        log.info {
                            "Eveara's Spotify distribution status for $songId is $spotifyOutletStatusCode, so we will check again at ${
                                LocalDateTime.ofInstant(
                                    context.nextFireTime.toInstant(),
                                    ZoneOffset.UTC
                                )
                            }"
                        }
                    }
                }
            } catch (e: Exception) {
                val errorMessage = "Error in EvearaReleaseStatusJob: ${context.mergedJobDataMap}"
                log.error(e) { errorMessage }
                if (e !is EOFException && e !is HttpStatusException) {
                    songRepository.updateSongMintingStatus(
                        songId = songId,
                        mintingStatus = MintingStatus.SubmittedForDistributionException,
                        errorMessage = "$errorMessage: ${e.message}",
                    )

                    // Cancel this job's future executions
                    context.scheduler.deleteJob(
                        JobKey.jobKey(
                            context.jobDetail.key.name,
                            context.jobDetail.key.group
                        )
                    )
                }
                Unit
            }
        }
    }
}
