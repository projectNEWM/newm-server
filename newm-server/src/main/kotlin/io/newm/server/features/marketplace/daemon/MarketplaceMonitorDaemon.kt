package io.newm.server.features.marketplace.daemon

import io.github.oshai.kotlinlogging.KotlinLogging
import io.grpc.Status
import io.grpc.StatusException
import io.newm.chain.grpc.MonitorAddressResponse
import io.newm.server.config.repo.ConfigRepository
import io.newm.server.config.repo.ConfigRepository.Companion.CONFIG_KEY_MARKETPLACE_MONITORING_MULTI_MODE_ENABLED
import io.newm.server.config.repo.ConfigRepository.Companion.CONFIG_KEY_MARKETPLACE_MONITORING_RETRY_DELAY
import io.newm.server.config.repo.ConfigRepository.Companion.CONFIG_KEY_MARKETPLACE_QUEUE_CONTRACT_ADDRESS
import io.newm.server.config.repo.ConfigRepository.Companion.CONFIG_KEY_MARKETPLACE_SALE_CONTRACT_ADDRESS
import io.newm.server.features.cardano.repo.CardanoRepository
import io.newm.server.features.marketplace.repo.MarketplaceRepository
import io.newm.shared.daemon.Daemon
import io.newm.shared.koin.inject
import io.newm.shared.ktx.coLazy
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.apache.curator.framework.recipes.leader.LeaderLatch
import org.apache.curator.framework.recipes.leader.LeaderLatchListener
import org.koin.core.parameter.parametersOf

private const val LEADER_LATCH_PATH = "/leader-latches/marketplace-monitor"

class MarketplaceMonitorDaemon(
    private val configRepository: ConfigRepository,
    private val cardanoRepository: CardanoRepository,
    private val marketplaceRepository: MarketplaceRepository
) : Daemon,
    LeaderLatchListener {
    override val log = KotlinLogging.logger {}
    private val leaderLatch: LeaderLatch by inject { parametersOf(LEADER_LATCH_PATH) }
    private val isMultiModeEnabled: Boolean by coLazy {
        configRepository.getBoolean(CONFIG_KEY_MARKETPLACE_MONITORING_MULTI_MODE_ENABLED)
    }

    override fun start() {
        if (isMultiModeEnabled) {
            log.info { "Starting in multi-mode..." }
            leaderLatch.addListener(this)
            leaderLatch.start()
        } else {
            log.info { "Starting in single-mode..." }
            startMonitoring()
        }
    }

    override fun shutdown() {
        log.info { "Shutting down..." }
        coroutineContext.cancelChildren()
        if (isMultiModeEnabled) {
            leaderLatch.close()
        }
    }

    override fun isLeader() {
        // only the leader runs
        log.warn { "This instance is: LEADER" }
        startMonitoring()
    }

    override fun notLeader() {
        // we may become the leader later, but only if the current leader shutdowns or crashes
        log.warn { "This instance is: NOT LEADER" }
        coroutineContext.cancelChildren()
    }

    private fun startMonitoring() {
        launch {
            monitorContractAddress(
                addressKey = CONFIG_KEY_MARKETPLACE_SALE_CONTRACT_ADDRESS,
                getTip = marketplaceRepository::getSaleTransactionTip,
                processTransaction = marketplaceRepository::processSaleTransaction
            )
        }
        launch {
            monitorContractAddress(
                addressKey = CONFIG_KEY_MARKETPLACE_QUEUE_CONTRACT_ADDRESS,
                getTip = marketplaceRepository::getQueueTransactionTip,
                processTransaction = marketplaceRepository::processQueueTransaction
            )
        }
    }

    private suspend fun monitorContractAddress(
        addressKey: String,
        getTip: suspend () -> String?,
        processTransaction: suspend (MonitorAddressResponse) -> Unit
    ) {
        val address = configRepository.getString(addressKey)
        val retryDelay = configRepository.getLong(CONFIG_KEY_MARKETPLACE_MONITORING_RETRY_DELAY)
        var logStart = true
        while (true) {
            try {
                val tip = getTip()
                if (logStart) {
                    log.info { "Starting monitoring $address after TxId $tip" }
                    logStart = false
                }
                cardanoRepository.monitorAddress(address, tip).collect(processTransaction)
            } catch (e: CancellationException) {
                log.info { "Ending monitoring $address" }
                throw e
            } catch (e: StatusException) {
                if (e.status.code == Status.Code.UNAVAILABLE || e.status.code == Status.Code.OK) {
                    // Probably just a connection that reached max age and disconnected cleanly.
                    // Retry immediately without any scary log.
                    log.debug { "Disconnected monitoring $address - retry immediately" }
                    delay(1.seconds)
                } else {
                    log.error(e) { "Failed monitoring $address - will retry in $retryDelay seconds" }
                    delay(retryDelay.seconds)
                    logStart = true
                }
            } catch (t: Throwable) {
                log.error(t) { "Failed monitoring $address - will retry in $retryDelay seconds" }
                delay(retryDelay.seconds)
                logStart = true
            }
        }
    }
}
