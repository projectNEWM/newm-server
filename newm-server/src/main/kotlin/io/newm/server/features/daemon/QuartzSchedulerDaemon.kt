package io.newm.server.features.daemon

import io.github.oshai.kotlinlogging.KotlinLogging
import io.newm.server.config.repo.ConfigRepository
import io.newm.server.config.repo.ConfigRepository.Companion.CONFIG_KEY_SCHEDULER_EVEARA_SYNC
import io.newm.server.features.scheduler.ArweaveCheckAndFundJob
import io.newm.server.features.scheduler.EvearaSyncJob
import io.newm.shared.daemon.Daemon
import io.newm.shared.koin.inject
import io.newm.shared.ktx.propertiesFromResource
import java.util.Date
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.quartz.CronScheduleBuilder.cronSchedule
import org.quartz.JobBuilder.newJob
import org.quartz.JobDetail
import org.quartz.JobKey
import org.quartz.Scheduler
import org.quartz.SimpleScheduleBuilder.simpleSchedule
import org.quartz.Trigger
import org.quartz.TriggerBuilder.newTrigger
import org.quartz.TriggerKey
import org.quartz.impl.StdSchedulerFactory

class QuartzSchedulerDaemon : Daemon {
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
        startEvearaSync(scheduler)
        startArweaveCheckAndFund(scheduler)
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
        val props = propertiesFromResource("quartz.properties")
        log.info { "Initializing Quartz Scheduler...: $props" }

        // DEBUG junk
        // props["org.quartz.plugin.triggHistory.class"] = "org.quartz.plugins.history.LoggingTriggerHistoryPlugin"
        // props["org.quartz.plugin.triggHistory.triggerFiredMessage"] =
        //     """Trigger {1}.{0} fired job {6}.{5} at: {4, date, HH:mm:ss MM/dd/yyyy}"""
        // props["org.quartz.plugin.triggHistory.triggerCompleteMessage"] =
        //     """Trigger {1}.{0} completed firing job {6}.{5} at {4, date, HH:mm:ss MM/dd/yyyy}"""

        return StdSchedulerFactory(props).scheduler
    }

    private fun startEvearaSync(scheduler: Scheduler) {
        launch {
            val jobKey = JobKey(EVEARA_SYNC_QUARTZ_JOB_KEY, EVEARA_SYNC_QUARTZ_GROUP)
            while (true) {
                try {
                    log.debug { "Validate Scheduled jobs..." }
                    val evearaCronSyncSchedule = configRepository.getString(CONFIG_KEY_SCHEDULER_EVEARA_SYNC)
                    val triggerKey = TriggerKey("${jobKey.name}_$evearaCronSyncSchedule", EVEARA_SYNC_QUARTZ_GROUP)
                    if (!scheduler.checkExists(jobKey)) {
                        log.info { "Creating new job for $EVEARA_SYNC_QUARTZ_JOB_KEY" }
                        val jobDetail =
                            newJob(EvearaSyncJob::class.java)
                                .withIdentity(jobKey)
                                .build()
                        val trigger =
                            newTrigger()
                                .withIdentity(triggerKey)
                                .withSchedule(cronSchedule(evearaCronSyncSchedule))
                                .build()
                        scheduler.scheduleJob(jobDetail, trigger)
                        log.info { "Scheduled job $jobKey with trigger: $trigger" }
                    } else {
                        val jobDetail = scheduler.getJobDetail(jobKey)
                        val currentTriggers = scheduler.getTriggersOfJob(jobKey)
                        val checkTriggerExists = currentTriggers.any { trigger -> trigger.key == triggerKey }
                        if (!checkTriggerExists) {
                            // correct trigger doesn't exist for this job. remove all existing triggers and re-schedule it
                            scheduler.unscheduleJobs(currentTriggers.map { it.key })
                            // reschedule
                            val trigger =
                                newTrigger()
                                    .withIdentity(triggerKey)
                                    .withSchedule(cronSchedule(evearaCronSyncSchedule))
                                    .build()

                            scheduler.scheduleJob(jobDetail, trigger)
                            log.info { "Rescheduled job $jobKey with trigger: $trigger" }
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Throwable) {
                    log.error(e) { "Error validating scheduled jobs!" }
                }
                delay(TimeUnit.MINUTES.toMillis(1))
            }
        }
    }

    private fun startArweaveCheckAndFund(scheduler: Scheduler) {
        try {
            val jobKey = JobKey(ARWEAVE_CHECK_AND_FUND_JOB_KEY, ARWEAVE_CHECK_AND_FUND_JOB_GROUP)
            if (scheduler.checkExists(jobKey)) {
                log.info { "Found existing job for $jobKey" }
                return
            }
            val triggerKey = TriggerKey("${jobKey.name}_trigger", ARWEAVE_CHECK_AND_FUND_JOB_GROUP)
            log.info { "Creating new job for $jobKey" }
            val jobDetail =
                newJob(ArweaveCheckAndFundJob::class.java)
                    .withIdentity(jobKey)
                    .build()
            val trigger =
                newTrigger()
                    .withIdentity(triggerKey)
                    .startAt(Date(System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(30)))
                    .withSchedule(simpleSchedule().withIntervalInHours(24).repeatForever())
                    .build()
            scheduler.scheduleJob(jobDetail, trigger)
            log.info { "Scheduled job $jobKey with trigger: $trigger" }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            log.error(e) { "Error validating scheduled jobs!" }
        }
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

    companion object {
        private const val EVEARA_SYNC_QUARTZ_JOB_KEY = "eveara_sync_job"
        private const val EVEARA_SYNC_QUARTZ_GROUP = "eveara_sync_group"
        private const val ARWEAVE_CHECK_AND_FUND_JOB_KEY = "arweave_check_and_fund_job"
        private const val ARWEAVE_CHECK_AND_FUND_JOB_GROUP = "arweave_check_and_fund_group"
    }
}
