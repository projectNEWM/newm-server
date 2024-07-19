package io.newm.server.features.earnings.repo

import io.github.oshai.kotlinlogging.KotlinLogging
import io.newm.server.config.repo.ConfigRepository
import io.newm.server.config.repo.ConfigRepository.Companion.CONFIG_KEY_EARNINGS_CLAIM_ORDER_FEE
import io.newm.server.features.cardano.database.KeyTable
import io.newm.server.features.cardano.model.Key
import io.newm.server.features.cardano.repo.CardanoRepository
import io.newm.server.features.earnings.daemon.MonitorClaimOrderJob
import io.newm.server.features.earnings.daemon.MonitorClaimOrderSchedulerDaemon
import io.newm.server.features.earnings.database.ClaimOrderEntity
import io.newm.server.features.earnings.database.ClaimOrdersTable
import io.newm.server.features.earnings.database.EarningEntity
import io.newm.server.features.earnings.database.EarningsTable
import io.newm.server.features.earnings.model.AddSongRoyaltyRequest
import io.newm.server.features.earnings.model.ClaimOrder
import io.newm.server.features.earnings.model.ClaimOrder.Companion.ACTIVE_STATUSES
import io.newm.server.features.earnings.model.ClaimOrderStatus
import io.newm.server.features.earnings.model.Earning
import io.newm.server.features.song.database.SongTable
import io.newm.server.features.song.repo.SongRepository
import io.newm.server.features.user.repo.UserRepository
import io.newm.server.typealiases.SongId
import io.newm.shared.ktx.toDate
import java.sql.Connection.TRANSACTION_SERIALIZABLE
import java.time.Instant
import java.time.LocalDateTime
import java.util.UUID
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.quartz.JobBuilder.newJob
import org.quartz.JobKey
import org.quartz.SimpleScheduleBuilder.simpleSchedule
import org.quartz.TriggerBuilder.newTrigger

class EarningsRepositoryImpl(
    private val userRepository: UserRepository,
    private val songRepository: SongRepository,
    private val cardanoRepository: CardanoRepository,
    private val configRepository: ConfigRepository,
    private val schedulerDaemon: MonitorClaimOrderSchedulerDaemon,
) : EarningsRepository {
    private val log = KotlinLogging.logger {}

    override suspend fun add(earning: Earning): UUID =
        transaction {
            EarningEntity
                .new {
                    this.songId = earning.songId?.let { EntityID(it, SongTable) }
                    this.stakeAddress = earning.stakeAddress
                    this.amount = earning.amount
                    this.memo = earning.memo
                    this.startDate = earning.startDate
                    this.endDate = earning.endDate
                    this.claimed = earning.claimed
                    this.claimedAt = earning.claimedAt
                    this.claimOrderId = earning.claimOrderId?.let { EntityID(it, ClaimOrdersTable) }
                    this.createdAt = earning.createdAt
                }.id.value
        }

    override suspend fun addAll(earnings: List<Earning>) {
        transaction {
            EarningsTable.batchInsert(earnings) { earning ->
                this[EarningsTable.songId] = earning.songId?.let { EntityID(it, SongTable) }
                this[EarningsTable.stakeAddress] = earning.stakeAddress
                this[EarningsTable.amount] = earning.amount
                this[EarningsTable.memo] = earning.memo
                this[EarningsTable.startDate] = earning.startDate
                this[EarningsTable.endDate] = earning.endDate
                this[EarningsTable.claimed] = earning.claimed
                this[EarningsTable.claimedAt] = earning.claimedAt
                this[EarningsTable.claimedOrderId] = earning.claimOrderId?.let { EntityID(it, ClaimOrdersTable) }
                this[EarningsTable.createdAt] = earning.createdAt
            }
        }
    }

    override suspend fun addRoyaltySplits(
        songId: SongId,
        royaltyRequest: AddSongRoyaltyRequest
    ) {
        require((royaltyRequest.newmAmount != null) xor (royaltyRequest.usdAmount != null)) {
            "Either newmAmount or usdAmount must be provided, but not both."
        }

        val song = songRepository.get(songId)
        val user = userRepository.get(song.ownerId!!)

        val snapshotResponse = cardanoRepository.snapshotToken(policyId = song.nftPolicyId!!, name = song.nftName!!)
        require(snapshotResponse.snapshotEntriesCount > 0) {
            "No snapshot entries found for song."
        }

        val snapshotMap = snapshotResponse.snapshotEntriesList.associate { it.stakeAddress to it.amount.toBigDecimal() }

        // should be 100m stream tokens
        val totalSupply = snapshotMap["total_supply"] ?: error("No total supply found in snapshot.")
        require(totalSupply == 100_000_000.toBigDecimal()) {
            "Total supply of stream tokens must be 100m."
        }

        val now = LocalDateTime.now()
        var exchangeRate = ""
        val totalNewmAmount =
            royaltyRequest.newmAmount?.toBigDecimal() ?: run {
                val usdAmount = royaltyRequest.usdAmount!!
                val newmUsdPrice = cardanoRepository.queryNEWMUSDPrice()
                val newmAmount = (newmUsdPrice.toBigInteger() * usdAmount).toBigDecimal()
                exchangeRate = " @ 1 NEWM = ${newmUsdPrice.toBigDecimal().movePointLeft(6).toPlainString()} USD"
                newmAmount
            }

        val earnings =
            snapshotMap.mapNotNull { (stakeAddress, streamTokenAmount) ->
                if (stakeAddress == "total_supply") return@mapNotNull null

                // calculate the amount of royalties for this stake address
                // wait 24 hours before starting the royalties in case somebody
                // put in a number wrong.
                Earning(
                    songId = songId,
                    stakeAddress = stakeAddress,
                    amount = (totalNewmAmount * (streamTokenAmount.setScale(6) / totalSupply.setScale(6))).toLong(),
                    memo = "Royalty for: ${song.title} - ${user.stageOrFullName}$exchangeRate",
                    createdAt = now,
                    startDate = now.plusHours(24),
                )
            }
        addAll(earnings)
    }

    override suspend fun add(claimOrder: ClaimOrder): UUID =
        transaction {
            ClaimOrderEntity
                .new {
                    this.stakeAddress = claimOrder.stakeAddress
                    this.keyId = EntityID(claimOrder.keyId, KeyTable)
                    this.paymentAddress = claimOrder.paymentAddress
                    this.paymentAmount = claimOrder.paymentAmount
                    this.status = claimOrder.status.name
                    this.earningsIds = claimOrder.earningsIds.toTypedArray()
                    this.failedEarningsIds = claimOrder.failedEarningsIds?.toTypedArray()
                    this.transactionId = claimOrder.transactionId
                    this.createdAt = claimOrder.createdAt
                    this.errorMessage = claimOrder.errorMessage
                }.id.value
        }

    override suspend fun update(claimOrder: ClaimOrder) {
        transaction {
            val entity = ClaimOrderEntity[claimOrder.id]
            entity.stakeAddress = claimOrder.stakeAddress
            entity.keyId = EntityID(claimOrder.keyId, KeyTable)
            entity.paymentAddress = claimOrder.paymentAddress
            entity.paymentAmount = claimOrder.paymentAmount
            entity.status = claimOrder.status.name
            entity.earningsIds = claimOrder.earningsIds.toTypedArray()
            entity.failedEarningsIds = claimOrder.failedEarningsIds?.toTypedArray()
            entity.transactionId = claimOrder.transactionId
            entity.errorMessage = claimOrder.errorMessage
        }
    }

    override suspend fun claimed(
        earningsIds: List<UUID>,
        claimOrderId: UUID
    ) {
        transaction {
            val claimedAt = LocalDateTime.now()
            EarningEntity.forIds(earningsIds).forUpdate().forEach {
                it.claimed = true
                it.claimOrderId = EntityID(claimOrderId, ClaimOrdersTable)
                it.claimedAt = claimedAt
            }
        }
    }

    override suspend fun createClaimOrder(stakeAddress: String): ClaimOrder? {
        // create any claim orders one at a time to ensure a flood attack can't create a bunch of dup claim orders
        val claimOrder =
            newSuspendedTransaction(transactionIsolation = TRANSACTION_SERIALIZABLE) {
                // check for existing open claim record first
                getActiveClaimOrderByStakeAddress(stakeAddress) ?: run {
                    // create a new claim record
                    val unclaimedEarnings = getAllUnclaimedByStakeAddress(stakeAddress)
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

                    add(claimOrder)
                    claimOrder
                }
            }

        return claimOrder?.also { monitor(it) }
    }

    override suspend fun getAll(): List<Earning> =
        transaction {
            EarningEntity.all().map { it.toModel() }
        }

    override suspend fun getAllBySongId(songId: SongId): List<Earning> =
        transaction {
            EarningEntity
                .wrapRows(
                    EarningEntity.searchQuery(EarningsTable.songId eq songId).orderBy(EarningsTable.createdAt, SortOrder.DESC)
                ).map { it.toModel() }
        }

    override suspend fun getAllByStakeAddress(stakeAddress: String): List<Earning> =
        transaction {
            EarningEntity
                .wrapRows(
                    EarningEntity.searchQuery(EarningsTable.stakeAddress eq stakeAddress).orderBy(EarningsTable.createdAt, SortOrder.DESC)
                ).map { it.toModel() }
        }

    override suspend fun getAllUnclaimedByStakeAddress(stakeAddress: String): List<Earning> =
        transaction {
            EarningEntity
                .wrapRows(
                    EarningEntity
                        .searchQuery(
                            (EarningsTable.stakeAddress eq stakeAddress) and (EarningsTable.claimed eq false)
                        ).orderBy(EarningsTable.createdAt, SortOrder.DESC)
                ).map { it.toModel() }
        }

    override suspend fun getAllByIds(earningsIds: List<UUID>): List<Earning> =
        transaction {
            EarningEntity
                .wrapRows(
                    EarningEntity
                        .searchQuery(
                            (EarningsTable.id inList earningsIds) and (EarningsTable.claimed eq false)
                        ).orderBy(EarningsTable.createdAt, SortOrder.DESC)
                ).map { it.toModel() }
        }

    override suspend fun getActiveClaimOrderByStakeAddress(stakeAddress: String): ClaimOrder? =
        transaction {
            ClaimOrderEntity
                .wrapRows(
                    ClaimOrderEntity.searchQuery(
                        (ClaimOrdersTable.stakeAddress eq stakeAddress) and (ClaimOrdersTable.status inList ACTIVE_STATUSES)
                    )
                ).let { list ->
                    if (list.count() > 1) {
                        throw IllegalStateException("More than one active claim order found for stake address: $stakeAddress")
                    }
                    list.firstOrNull()
                }?.toModel()
        }

    override suspend fun getByClaimOrderId(claimOrderId: UUID): ClaimOrder? =
        transaction {
            ClaimOrderEntity.findById(claimOrderId)?.toModel()
        }

    private fun monitor(claimOrder: ClaimOrder) {
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
}
