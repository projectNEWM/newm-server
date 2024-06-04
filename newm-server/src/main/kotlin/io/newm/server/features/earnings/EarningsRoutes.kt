package io.newm.server.features.earnings

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.route
import io.newm.chain.util.extractStakeAddress
import io.newm.server.auth.jwt.AUTH_JWT
import io.newm.server.auth.jwt.AUTH_JWT_ADMIN
import io.newm.server.config.repo.ConfigRepository
import io.newm.server.config.repo.ConfigRepository.Companion.CONFIG_KEY_EARNINGS_CLAIM_ORDER_FEE
import io.newm.server.features.cardano.model.Key
import io.newm.server.features.cardano.repo.CardanoRepository
import io.newm.server.features.earnings.daemon.MonitorClaimOrderJob
import io.newm.server.features.earnings.daemon.MonitorClaimOrderSchedulerDaemon
import io.newm.server.features.earnings.model.AddSongRoyaltyRequest
import io.newm.server.features.earnings.model.ClaimOrder
import io.newm.server.features.earnings.model.ClaimOrderStatus
import io.newm.server.features.earnings.model.Earning
import io.newm.server.features.earnings.repo.EarningsRepository
import io.newm.shared.koin.inject
import io.newm.shared.ktx.get
import io.newm.shared.ktx.post
import io.newm.shared.ktx.toDate
import io.newm.shared.ktx.toUUID
import java.sql.Connection.TRANSACTION_SERIALIZABLE
import java.time.Instant
import java.time.LocalDateTime
import java.util.UUID
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.quartz.JobBuilder.newJob
import org.quartz.JobKey
import org.quartz.SimpleScheduleBuilder.simpleSchedule
import org.quartz.TriggerBuilder.newTrigger

private const val EARNINGS_PATH = "v1/earnings"

fun Routing.createEarningsRoutes() {
    val log = KotlinLogging.logger {}
    val cardanoRepository: CardanoRepository by inject()
    val earningsRepository: EarningsRepository by inject()
    val configRepository: ConfigRepository by inject()
    val schedulerDaemon: MonitorClaimOrderSchedulerDaemon by inject()

    fun monitor(claimOrder: ClaimOrder) {
        val jobKey = JobKey("monitorClaimOrder_${claimOrder.id}", "earnings")
        if (schedulerDaemon.jobExists(jobKey)) {
            log.info { "Job already exists for claim order ${claimOrder.id}" }
            return
        }
        val jobDetail =
            newJob(MonitorClaimOrderJob::class.java)
                .withIdentity(jobKey)
                .usingJobData("claimOrderId", claimOrder.id.toString())
                .requestRecovery(true)
                .build()
        val trigger =
            newTrigger()
                .forJob(jobDetail)
                .startAt(Instant.now().plusSeconds(60).toDate())
                .withSchedule(simpleSchedule().withMisfireHandlingInstructionFireNow())
                .build()

        schedulerDaemon.scheduleJob(jobDetail, trigger)
    }

    authenticate(AUTH_JWT_ADMIN) {
        route(EARNINGS_PATH) {
            get {
                val earnings = earningsRepository.getAll()
                respond(earnings)
            }
            // create earning records
            post {
                val earnings = receive<List<Earning>>()
                earningsRepository.addAll(earnings)
                respond(HttpStatusCode.Created)
            }
        }
        get("$EARNINGS_PATH/{songId}") {
            // get earnings by song id
            val songId = parameters["songId"]!!.toUUID()
            val earnings = earningsRepository.getAllBySongId(songId)
            respond(earnings)
        }
        post("$EARNINGS_PATH/{songId}") {
            // create earning records for a song based on receiving a total amount of royalties.
            val songId = parameters["songId"]!!.toUUID()
            val royaltyRequest: AddSongRoyaltyRequest = receive()
            earningsRepository.addRoyaltySplits(songId, royaltyRequest)
            respond(HttpStatusCode.Created)
        }
    }
    authenticate(AUTH_JWT) {
        route("$EARNINGS_PATH/{walletAddress}") {
            // get earnings
            get {
                val stakeAddress = parameters["walletAddress"]!!.extractStakeAddress(cardanoRepository.isMainnet())
                val earnings = earningsRepository.getAllByStakeAddress(stakeAddress)
                respond(earnings)
            }
            // create a claim for all earnings on this wallet stake address
            post {
                val stakeAddress = parameters["walletAddress"]!!.extractStakeAddress(cardanoRepository.isMainnet())

                // create any claim orders one at a time to ensure a flood attack can't create a bunch of dup claim orders
                val claimOrder =
                    newSuspendedTransaction(transactionIsolation = TRANSACTION_SERIALIZABLE) {
                        // check for existing open claim record first
                        earningsRepository.getActiveClaimOrderByStakeAddress(stakeAddress) ?: run {
                            // create a new claim record
                            val unclaimedEarnings = earningsRepository.getAllUnclaimedByStakeAddress(stakeAddress)
                            if (unclaimedEarnings.isEmpty()) {
                                return@run null
                            }
                            val key = Key.generateNew()
                            val keyId = cardanoRepository.saveKey(key)

                            val claimOrder =
                                ClaimOrder(
                                    id = UUID.randomUUID(),
                                    stakeAddress = stakeAddress,
                                    keyId = keyId,
                                    paymentAddress = key.address,
                                    paymentAmount = configRepository.getLong(CONFIG_KEY_EARNINGS_CLAIM_ORDER_FEE),
                                    status = ClaimOrderStatus.Pending,
                                    earningsIds = unclaimedEarnings.map { it.id!! },
                                    failedEarningsIds = null,
                                    transactionId = null,
                                    createdAt = LocalDateTime.now(),
                                    errorMessage = null,
                                )

                            earningsRepository.add(claimOrder)
                            claimOrder
                        }
                    }
                claimOrder?.let {
                    respond(claimOrder)
                    monitor(claimOrder)
                } ?: run {
                    respond(HttpStatusCode.NotFound, "No unclaimed earnings found for this wallet address.")
                }
            }
        }
    }
}
