package io.newm.server.features.scheduler

import io.newm.server.features.arweave.repo.ArweaveRepository
import io.newm.shared.koin.inject
import io.newm.shared.ktx.info
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.runBlocking
import org.koin.core.parameter.parametersOf
import org.quartz.DisallowConcurrentExecution
import org.quartz.Job
import org.quartz.JobExecutionContext
import org.slf4j.Logger

@DisallowConcurrentExecution
class ArweaveCheckAndFundJob : Job {
    private val log: Logger by inject { parametersOf(javaClass.simpleName) }
    private val arweaveRepository: ArweaveRepository by inject()

    override fun execute(context: JobExecutionContext) {
        runBlocking {
            try {
                log.info {
                    "ArweaveCheckAndFundJob executed at ${
                        LocalDateTime.ofInstant(
                            context.fireTime.toInstant(),
                            ZoneOffset.UTC
                        )
                    }"
                }
                arweaveRepository.checkAndFundTurboBalance()
            } catch (e: CancellationException) {
                log.info("ArweaveCheckAndFundJob cancelled.")
                throw e
            } catch (e: Throwable) {
                log.error("Error invoking Turbo check-and-fund!", e)
            }
        }
    }
}
