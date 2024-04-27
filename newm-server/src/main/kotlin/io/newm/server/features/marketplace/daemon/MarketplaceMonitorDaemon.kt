package io.newm.server.features.marketplace.daemon

import io.ktor.server.application.ApplicationEnvironment
import io.newm.chain.grpc.MonitorAddressResponse
import io.newm.server.features.cardano.repo.CardanoRepository
import io.newm.server.features.marketplace.repo.MarketplaceRepository
import io.newm.server.ktx.getSecureConfigString
import io.newm.shared.daemon.Daemon
import io.newm.shared.koin.inject
import io.newm.shared.ktx.error
import io.newm.shared.ktx.getConfigBoolean
import io.newm.shared.ktx.getConfigLong
import io.newm.shared.ktx.info
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.apache.curator.framework.recipes.leader.LeaderLatch
import org.apache.curator.framework.recipes.leader.LeaderLatchListener
import org.koin.core.parameter.parametersOf
import org.slf4j.Logger
import kotlin.time.Duration.Companion.seconds

private const val LEADER_LATCH_PATH = "/leader-latches/marketplace-monitor"

class MarketplaceMonitorDaemon(
    private val environment: ApplicationEnvironment,
    private val cardanoRepository: CardanoRepository,
    private val marketplaceRepository: MarketplaceRepository
) : Daemon, LeaderLatchListener {
    override val log: Logger by inject { parametersOf(javaClass.simpleName) }
    private val leaderLatch: LeaderLatch by inject { parametersOf(LEADER_LATCH_PATH) }
    private val isEnabled = environment.getConfigBoolean("marketplace.enabled")

    override fun start() {
        log.info { "Starting..." }
        if (isEnabled) {
            leaderLatch.addListener(this)
            leaderLatch.start()
        }
    }

    override fun shutdown() {
        log.info { "Shutting down..." }
        coroutineContext.cancelChildren()
        if (isEnabled) {
            leaderLatch.close()
        }
    }

    override fun isLeader() {
        // only the leader runs
        log.info { "This is instance is now the leader" }
        launch {
            run(
                path = "marketplace.sale",
                getTip = marketplaceRepository::getSaleTransactionTip,
                processTransaction = marketplaceRepository::processSaleTransaction
            )
        }
        launch {
            run(
                path = "marketplace.queue",
                getTip = marketplaceRepository::getQueueTransactionTip,
                processTransaction = marketplaceRepository::processQueueTransaction
            )
        }
    }

    override fun notLeader() {
        // we may become the leader later, but only if the current leader shutdowns or crashes
        log.info { "This is instance is not currently the leader" }
    }

    private suspend fun run(
        path: String,
        getTip: suspend () -> String?,
        processTransaction: suspend (MonitorAddressResponse) -> Unit
    ) {
        // TODO: Remove this after the smart contract is up in Mainnet
        if (cardanoRepository.isMainnet()) {
            log.info { "Temporarily disabled in Mainnet" }
            return
        }

        val address = environment.getSecureConfigString("$path.contractAddress")
        val retryDelay = environment.getConfigLong("$path.retryDelay")
        while (true) {
            try {
                val tip = getTip()
                log.info { "Starting monitoring $address after TxId $tip" }
                cardanoRepository.monitorAddress(address, tip).collect(processTransaction)
            } catch (e: CancellationException) {
                log.info { "Ending monitoring $address" }
                throw e
            } catch (t: Throwable) {
                log.error(t) { "Failed monitoring $address - will retry in $retryDelay seconds" }
                delay(retryDelay.seconds)
            }
        }
    }
}
