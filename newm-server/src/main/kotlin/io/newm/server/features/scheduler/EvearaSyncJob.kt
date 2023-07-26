package io.newm.server.features.scheduler

import io.newm.shared.koin.inject
import io.newm.shared.ktx.info
import org.koin.core.parameter.parametersOf
import org.quartz.Job
import org.quartz.JobExecutionContext
import org.slf4j.Logger
import java.time.LocalDateTime
import java.time.ZoneOffset

class EvearaSyncJob : Job {
    private val log: Logger by inject { parametersOf(javaClass.simpleName) }
    override fun execute(context: JobExecutionContext) {
        log.info { "EvearaSyncJob executed at ${LocalDateTime.ofInstant(context.fireTime.toInstant(),ZoneOffset.UTC)}" }
        // do nothing for now. In the future, this can sync analytics or something.
    }
}
