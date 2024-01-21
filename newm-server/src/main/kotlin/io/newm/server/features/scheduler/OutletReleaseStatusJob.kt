package io.newm.server.features.scheduler

import io.newm.server.features.release.repo.OutletReleaseRepository
import io.newm.server.features.song.model.MintingStatus
import io.newm.server.features.song.repo.SongRepository
import io.newm.shared.koin.inject
import io.newm.shared.ktx.info
import io.newm.shared.ktx.toUUID
import kotlinx.coroutines.runBlocking
import org.koin.core.parameter.parametersOf
import org.quartz.DisallowConcurrentExecution
import org.quartz.Job
import org.quartz.JobExecutionContext
import org.quartz.JobKey
import org.slf4j.Logger
import java.time.LocalDateTime
import java.time.ZoneOffset

@DisallowConcurrentExecution
class OutletReleaseStatusJob : Job {
    private val log: Logger by inject { parametersOf(javaClass.simpleName) }
    private val songRepository: SongRepository by inject()
    private val outletReleaseRepository: OutletReleaseRepository by inject()

    override fun execute(context: JobExecutionContext) {
        log.info {
            "OutletReleaseStatusJob key: ${context.jobDetail.key.name} executed at ${
                LocalDateTime.ofInstant(
                    context.fireTime.toInstant(),
                    ZoneOffset.UTC
                )
            }"
        }
        runBlocking {
            val songId = context.mergedJobDataMap.getString("songId").toUUID()
            try {
                val song = songRepository.get(songId)
                if (outletReleaseRepository.isSongReleased(songId) || song.forceDistributed == true) {
                    songRepository.updateSongMintingStatus(songId, MintingStatus.Released)

                    // Cancel this job's future executions
                    context.scheduler.deleteJob(
                        JobKey.jobKey(
                            context.jobDetail.key.name,
                            context.jobDetail.key.group
                        )
                    )
                } else {
                    log.info {
                        "Outlet release status for $songId is false, so we will check again at ${
                            LocalDateTime.ofInstant(
                                context.nextFireTime.toInstant(),
                                ZoneOffset.UTC
                            )
                        }"
                    }
                }
            } catch (e: Exception) {
                val errorMessage = "Error in OutletReleaseStatusJob: ${context.mergedJobDataMap}"
                log.error(errorMessage, e)
                songRepository.updateSongMintingStatus(
                    songId = songId,
                    mintingStatus = MintingStatus.ReleaseCheckException,
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
        }
    }
}
