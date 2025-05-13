package io.newm.server.features.scheduler

import io.github.oshai.kotlinlogging.KotlinLogging
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

    override fun execute(context: JobExecutionContext) {
        log.info {
            "ReferralHeroSyncJob key: ${context.jobDetail.key.name} executed at ${
                LocalDateTime.ofInstant(context.fireTime.toInstant(), ZoneOffset.UTC)
            }"
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
                val referralCode = runBlocking { referralHeroRepository.addSubscriber(user.email) }
                transaction { user.referralCode = referralCode }
                log.info { "Successfully synced user `${user.email}`" }
            } catch (e: Exception) {
                log.error(e) { "Failed syncing user `${user.email}`" }
            }
        }
    }
}
