package io.newm.server.features.scheduler

import io.newm.server.features.distribution.DistributionRepository
import io.newm.server.features.distribution.model.OutletStatusCode
import io.newm.server.features.song.model.MintingStatus
import io.newm.server.features.song.repo.SongRepository
import io.newm.server.features.user.repo.UserRepository
import io.newm.shared.koin.inject
import io.newm.shared.ktx.info
import io.newm.shared.ktx.toUUID
import kotlinx.coroutines.runBlocking
import org.koin.core.parameter.parametersOf
import org.quartz.Job
import org.quartz.JobExecutionContext
import org.quartz.JobKey
import org.slf4j.Logger
import java.time.LocalDateTime
import java.time.ZoneOffset

class EvearaReleaseStatusJob : Job {
    private val log: Logger by inject { parametersOf(javaClass.simpleName) }
    private val distributionRepository: DistributionRepository by inject()
    private val userRepository: UserRepository by inject()
    private val songRepository: SongRepository by inject()
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
            try {
                val userId = context.mergedJobDataMap.getString("userId").toUUID()
                val songId = context.mergedJobDataMap.getString("songId").toUUID()
                val user = userRepository.get(userId)
                val song = songRepository.get(songId)
                val distributionReleaseStatusResponse =
                    distributionRepository.distributionOutletReleaseStatus(user, song.distributionReleaseId!!)
                val spotifyOutletStatusCode = if (song.forceDistributed == true) {
                    // If the song is force distributed, then we can mark it as distributed
                    OutletStatusCode.DISTRIBUTED
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
                        songRepository.updateSongMintingStatus(songId, MintingStatus.Declined)

                        // Cancel this job's future executions
                        context.scheduler.deleteJob(
                            JobKey.jobKey(
                                context.jobDetail.key.name,
                                context.jobDetail.key.group
                            )
                        )
                    }

                    else -> {
                        log.info {
                            "Spotify release status for $songId is $spotifyOutletStatusCode, so we will check again at ${
                                LocalDateTime.ofInstant(
                                    context.nextFireTime.toInstant(),
                                    ZoneOffset.UTC
                                )
                            }"
                        }
                    }
                }
            } catch (e: Exception) {
                log.error("Error in EvearaReleaseStatusJob: ${context.mergedJobDataMap}", e)
            }
        }
    }
}
