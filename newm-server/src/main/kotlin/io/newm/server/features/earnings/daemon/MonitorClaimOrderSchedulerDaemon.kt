package io.newm.server.features.earnings.daemon

import io.github.oshai.kotlinlogging.KotlinLogging
import io.newm.server.config.repo.ConfigRepository
import io.newm.shared.daemon.Daemon
import io.newm.shared.koin.inject
import io.newm.shared.ktx.propertiesFromResource
import java.util.Date
import kotlinx.coroutines.cancelChildren
import org.quartz.JobDetail
import org.quartz.JobKey
import org.quartz.Scheduler
import org.quartz.Trigger
import org.quartz.impl.StdSchedulerFactory

class MonitorClaimOrderSchedulerDaemon : Daemon {
    override val log = KotlinLogging.logger {}
    private val configRepository: ConfigRepository by inject()

    private lateinit var scheduler: Scheduler

    override fun start() {
        log.info { "starting..." }
        scheduler =
            try {
                initializeQuartzScheduler().apply { start() }
            } catch (e: Throwable) {
                log.error(e) { "Error initializing Quartz!" }
                return
            }
        log.info { "startup complete." }
    }

    override fun shutdown() {
        log.info { "begin shutdown..." }
        if (::scheduler.isInitialized) {
            scheduler.shutdown(true)
        }
        coroutineContext.cancelChildren()
        log.info { "shutdown complete." }
    }

    private fun initializeQuartzScheduler(): Scheduler {
        val props = propertiesFromResource("monitor_orders_quartz.properties")
        log.info { "Initializing Quartz Scheduler...: $props" }

        // DEBUG junk
        // props["org.quartz.plugin.triggHistory.class"] = "org.quartz.plugins.history.LoggingTriggerHistoryPlugin"
        // props["org.quartz.plugin.triggHistory.triggerFiredMessage"] =
        //     """Trigger {1}.{0} fired job {6}.{5} at: {4, date, HH:mm:ss MM/dd/yyyy}"""
        // props["org.quartz.plugin.triggHistory.triggerCompleteMessage"] =
        //     """Trigger {1}.{0} completed firing job {6}.{5} at {4, date, HH:mm:ss MM/dd/yyyy}"""

        return StdSchedulerFactory(props).scheduler
    }

    fun scheduleJob(
        jobDetail: JobDetail,
        trigger: Trigger
    ): Date {
        val date = scheduler.scheduleJob(jobDetail, trigger)
        log.warn { "Scheduled job ${jobDetail.key} with trigger: $trigger" }
        return date
    }

    fun jobExists(jobKey: JobKey): Boolean = scheduler.checkExists(jobKey)
}
