package io.newm.server.features.scheduler

import io.github.oshai.kotlinlogging.KotlinLogging
import io.newm.server.config.repo.ConfigRepository
import io.newm.server.config.repo.ConfigRepository.Companion.CONFIG_KEY_REFERRAL_HERO_ENABLED
import io.newm.server.features.referralhero.repo.ReferralHeroRepository
import io.newm.server.features.user.database.UserEntity
import io.newm.shared.koin.inject
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.transactions.transaction
import org.quartz.DisallowConcurrentExecution
import org.quartz.Job
import org.quartz.JobExecutionContext
import java.time.LocalDateTime
import java.time.ZoneOffset

@DisallowConcurrentExecution
class ReferralHeroSyncJob : Job {
    private val log = KotlinLogging.logger {}
    private val referralHeroRepository: ReferralHeroRepository by inject()
    private val configRepository: ConfigRepository by inject()

    override fun execute(context: JobExecutionContext) {
        log.info {
            "ReferralHeroSyncJob key: ${context.jobDetail.key.name} executed at ${
                LocalDateTime.ofInstant(context.fireTime.toInstant(), ZoneOffset.UTC)
            }"
        }
        if (!runBlocking { configRepository.getBoolean(CONFIG_KEY_REFERRAL_HERO_ENABLED) }) {
            log.info { "Syncing currently disabled" }
            return
        }

        val users = try {
            transaction { UserEntity.allWithoutReferralCode().toList() }
        } catch (e: Exception) {
            log.error(e) { "Failed querying users from db" }
            return
        }

        log.info { "Found ${users.size} users to sync" }
        for (user in users) {
            try {
                val subscriber = runBlocking { referralHeroRepository.getOrCreateSubscriber(user.email) }
                subscriber?.let {
                    transaction {
                        user.referralCode = it.referralCode
                        user.referralStatus = it.referralStatus
                    }
                    log.info { "Successfully synced user `${user.email}`" }
                }
            } catch (e: Exception) {
                log.error(e) { "Failed syncing user `${user.email}`" }
            }
        }
    }
}
