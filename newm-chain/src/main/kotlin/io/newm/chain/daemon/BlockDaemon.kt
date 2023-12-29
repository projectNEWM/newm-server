package io.newm.chain.daemon

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.google.protobuf.kotlin.toByteString
import io.ktor.server.application.ApplicationEnvironment
import io.newm.chain.cardano.getEpochForSlot
import io.newm.chain.cardano.to721Json
import io.newm.chain.cardano.toMetadataMap
import io.newm.chain.config.Config
import io.newm.chain.database.entity.LedgerAsset
import io.newm.chain.database.repository.ChainRepository
import io.newm.chain.database.repository.LedgerRepository
import io.newm.chain.database.table.AddressTxLogTable
import io.newm.chain.database.table.NativeAssetMonitorLogTable
import io.newm.chain.grpc.NewmChainService
import io.newm.chain.grpc.SubmitTransactionRequest
import io.newm.chain.grpc.monitorNativeAssetsResponse
import io.newm.chain.ledger.SubmittedTransactionCache
import io.newm.chain.logging.captureToSentry
import io.newm.chain.model.CreatedUtxo
import io.newm.chain.util.extractAssetMetadata
import io.newm.chain.util.toAssetMetadataList
import io.newm.chain.util.toChainBlock
import io.newm.chain.util.toCreatedUtxoSet
import io.newm.chain.util.toLedgerAssets
import io.newm.chain.util.toRawTransactionList
import io.newm.chain.util.toSpentUtxoSet
import io.newm.chain.util.toStakeDelegationList
import io.newm.chain.util.toStakeRegistrationList
import io.newm.kogmios.ChainSyncClient
import io.newm.kogmios.Client
import io.newm.kogmios.Client.Companion.DEFAULT_REQUEST_TIMEOUT_MS
import io.newm.kogmios.Client.Companion.INFINITE_REQUEST_TIMEOUT_MS
import io.newm.kogmios.StateQueryClient
import io.newm.kogmios.createChainSyncClient
import io.newm.kogmios.createStateQueryClient
import io.newm.kogmios.protocols.messages.IntersectionFound
import io.newm.kogmios.protocols.messages.RollBackward
import io.newm.kogmios.protocols.messages.RollForward
import io.newm.kogmios.protocols.messages.RollForwardData
import io.newm.kogmios.protocols.model.Block
import io.newm.kogmios.protocols.model.CompactGenesis
import io.newm.kogmios.protocols.model.MetadataMap
import io.newm.kogmios.protocols.model.OriginString
import io.newm.kogmios.protocols.model.PointDetail
import io.newm.kogmios.protocols.model.PointDetailOrOrigin
import io.newm.shared.daemon.Daemon
import io.newm.shared.daemon.Daemon.Companion.RETRY_DELAY_MILLIS
import io.newm.shared.koin.inject
import io.newm.shared.ktx.debug
import io.newm.shared.ktx.getConfigBoolean
import io.newm.shared.ktx.getConfigInt
import io.newm.shared.ktx.getConfigString
import io.newm.txbuilder.ktx.cborHexToPlutusData
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.koin.core.parameter.parametersOf
import org.slf4j.Logger
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.math.BigInteger
import java.time.Duration
import java.time.Instant
import kotlin.math.floor
import kotlin.math.max
import kotlin.system.measureTimeMillis

class BlockDaemon(
    private val environment: ApplicationEnvironment,
    private val chainRepository: ChainRepository,
    private val ledgerRepository: LedgerRepository,
    private val submittedTransactionCache: SubmittedTransactionCache,
    private val confirmedBlockFlow: MutableSharedFlow<Block>,
) : Daemon {
    override val log: Logger by inject { parametersOf("BlockDaemon") }

    private val server by lazy { environment.getConfigString("ogmios.server") }
    private val port by lazy { environment.getConfigInt("ogmios.port") }
    private val secure by lazy { environment.getConfigBoolean("ogmios.secure") }
    private val syncRawTxns by lazy { environment.getConfigBoolean("newmchain.syncRawTxns") }
    private val pruneUtxos by lazy { environment.getConfigBoolean("newmchain.pruneUtxos") }

    private val rollForwardFlow = MutableSharedFlow<RollForwardData>(
        replay = 0,
        extraBufferCapacity = 0,
        onBufferOverflow = BufferOverflow.SUSPEND
    )

    val committedRollForwardFlow = MutableSharedFlow<RollForwardData>(
        replay = 0,
        extraBufferCapacity = 20_000,
        onBufferOverflow = BufferOverflow.SUSPEND
    )

    private var blockBufferSize = 1
    private val blockBuffer: MutableList<RollForwardData> = mutableListOf()

    private var tipBlockHeight = 0L

    /**
     * Store the blocknumber mapped to a map of transactionIds so we can re-submit to the mempool
     * in the event of a rollback.
     */
    private val blockRollbackCache: Cache<Long, Set<String>> = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofHours(1))
        .build()

    // We just need the service to re-submit transactions. We won't be going through GRPC to do it so we can just
    // create our own local instance.
    private val newmChainService by lazy { NewmChainService() }

    override fun start() {
        log.info("starting...")
        startProcessBlock()
        startChainSync()
        log.info("startup complete.")
    }

    override fun shutdown() {
        log.info("shutdown complete.")
    }

    private fun startChainSync() {
        launch {
            // Allow other daemons time starting up to connect to our rollForwardFlow
            delay(1000L)

            // Fetch network genesis information
            while (true) {
                try {
                    createStateQueryClient(
                        websocketHost = server,
                        websocketPort = port,
                        secure = secure,
                        ogmiosCompact = true,
                    ).use { stateQueryClient ->
                        connect(stateQueryClient)
                        fetchNetworkInfo(stateQueryClient)
                    }
                    break
                } catch (e: Throwable) {
                    log.warn("Error connecting Kogmios!", e)
                    log.info("Wait 10 seconds to retry...")
                    delay(RETRY_DELAY_MILLIS)
                }
            }

            while (true) {
                try {
                    createChainSyncClient(
                        websocketHost = server,
                        websocketPort = port,
                        secure = secure,
                        ogmiosCompact = false,
                    ).use { localChainSyncClient ->
                        connect(localChainSyncClient)
                        syncBlockchain(localChainSyncClient)
                    }
                } catch (e: Throwable) {
                    log.warn("Error syncing blockchain!", e)
                    if (e !is TimeoutCancellationException) {
                        e.captureToSentry()
                    }
                    log.info("Wait 10 seconds to retry...")
                    try {
                        // clear out everything so far since we're going to retry...
                        blockBuffer.clear()
                    } catch (_: Throwable) {
                    }
                    try {
                        delay(RETRY_DELAY_MILLIS)
                    } catch (_: Throwable) {
                    }
                }
            }
        }
    }

    private suspend fun fetchNetworkInfo(client: StateQueryClient) {
        val genesis = client.genesisConfig().result as CompactGenesis
        Config.genesis = genesis
    }

    private suspend fun connect(client: Client) {
        val connectResult = client.connect()
        if (!connectResult) {
            throw IOException("client.connect() was false!")
        }
        if (!client.isConnected) {
            throw IOException("client.isConnected was false!")
        }
    }

    private suspend fun syncBlockchain(client: ChainSyncClient) {
        findBlockchainIntersect(client)
        requestBlocks(client)
    }

    private suspend fun findBlockchainIntersect(client: ChainSyncClient) {
        val intersectPoints: MutableList<PointDetailOrOrigin> = chainRepository.getFindIntersectPairs().toMutableList()
        val startSlot = environment.config.property("ogmios.startSlot").getString().toLong()
        if (startSlot > -1) {
            intersectPoints.add(
                PointDetail(
                    slot = startSlot,
                    hash = environment.config.property("ogmios.startHash").getString()
                )
            )
        } else {
            intersectPoints.add(
                OriginString()
            )
        }
        val msgFindIntersectResponse = client.findIntersect(intersectPoints)

        if (msgFindIntersectResponse.result !is IntersectionFound) {
            throw IllegalStateException("Error finding blockchain intersect!")
        }
    }

    private suspend fun requestBlocks(client: ChainSyncClient) {
        var lastLogged = Instant.EPOCH
        var isTip = true
        while (true) {
            val response = client.requestNext(
                timeoutMs = if (isTip) {
                    INFINITE_REQUEST_TIMEOUT_MS
                } else {
                    DEFAULT_REQUEST_TIMEOUT_MS
                }
            )
            when (response.result) {
                is RollBackward -> {
                    log.info("RollBackward: ${(response.result as RollBackward).rollBackward.point}")
                }

                is RollForward -> {
                    (response.result as RollForward).rollForward.let { rollForward ->
                        rollForwardFlow.emit(rollForward)
                        val blockHeight = rollForward.block.header.blockHeight
                        tipBlockHeight = max(blockHeight, rollForward.tip.blockNo)
                        isTip = blockHeight == tipBlockHeight
                        val now = Instant.now()
                        val tenSecondsAgo = now.minusSeconds(10L)
                        if (isTip || tenSecondsAgo.isAfter(lastLogged)) {
                            val percent = floor(blockHeight.toDouble() / tipBlockHeight.toDouble() * 10000.0) / 100.0
                            log.info("RollForward: block $blockHeight of $tipBlockHeight: %.2f%% sync'd".format(percent))
                            lastLogged = now
                        }
                    }
                }
            }
        }
    }

    private fun startProcessBlock() {
        launch {
            rollForwardFlow.collect { rollForward ->
                val blockHeight = rollForward.block.header.blockHeight
                tipBlockHeight = max(blockHeight, rollForward.tip.blockNo)
                val isTip = blockHeight == tipBlockHeight
                processBlock(rollForward, isTip)
            }
        }
    }

    private suspend fun processBlock(rollForwardData: RollForwardData, isTip: Boolean) {
        blockBuffer.add(rollForwardData)
        if (blockBuffer.size == blockBufferSize || isTip) {
            // Create a copy of the list
            val blocksToCommit = blockBuffer.toMutableList()
            blockBuffer.clear()
            withContext(NonCancellable) {
                commitBlocks(blocksToCommit, isTip)
            }
        }
    }

    private var lastLoggedCommit = Instant.EPOCH
    private suspend fun commitBlocks(blocksToCommit: List<RollForwardData>, isTip: Boolean) {
//        if (!isTip) {
//            log.warn("starting commitBlocks()...")
//        }
        var rollbackTime = 0L
        var chainTime = 0L
        var nativeAssetTime = 0L
        var spendTime = 0L
        var createTime = 0L
        var pruneTime = 0L
        measureTimeMillis {
            newSuspendedTransaction {
                warnLongQueriesDuration = 1000L
                val firstBlock = blocksToCommit.first().block
                val lastBlock = blocksToCommit.last().block
                rollbackTime += measureTimeMillis {
                    chainRepository.rollback(firstBlock.header.blockHeight)
                    ledgerRepository.doRollback(firstBlock.header.blockHeight)
                    AddressTxLogTable.deleteWhere { blockNumber greaterEq firstBlock.header.blockHeight }
                    NativeAssetMonitorLogTable.deleteWhere { blockNumber greaterEq firstBlock.header.blockHeight }
                }

                blocksToCommit.forEach { rollForwardData ->
                    val block = rollForwardData.block
//                    if (block.header.blockHeight <= 60) {
//                        log.warn("block: $block")
//                    }
                    val createdUtxos = block.toCreatedUtxoSet()
                    val ledgerAssets = block.toLedgerAssets()

                    // Mark same block number as rolled back
                    rollbackTime += measureTimeMillis {
                        // Handle re-submitting any of our own transactions that got rolled back
                        val tip = isTip && block == lastBlock
                        if (tip || blockRollbackCache.getIfPresent(block.header.blockHeight) != null) {
                            val ourTransactionIdsInBlock =
                                createdUtxos.map { createdUtxo -> createdUtxo.hash }.toSet()
                                    .filter { transactionId ->
                                        submittedTransactionCache.get(transactionId)?.also {
                                            log.debug { "Our transaction $transactionId was seen in a block!" }
                                        } != null
                                    }.toSet()
                            checkBlockRollbacks(block.header.blockHeight, ourTransactionIdsInBlock)
                        }
                    }

                    chainTime += measureTimeMillis {
                        chainRepository.insert(block.toChainBlock())
                    }

                    // Which ledger assets do we need to notify grpc subscribers of changes on.
                    val mintedLedgerAssets: List<LedgerAsset>
                    nativeAssetTime += measureTimeMillis {
                        // Handle any assets minted or burned
                        mintedLedgerAssets = ledgerRepository.upcertLedgerAssets(ledgerAssets)
                    }

                    // Insert unspent utxos and stake delegations/de-registrations
                    createTime += measureTimeMillis {
                        val blockNumber = block.header.blockHeight
                        ledgerRepository.createUtxos(blockNumber, createdUtxos)
                        val stakeRegistrations = block.toStakeRegistrationList()
                        ledgerRepository.createStakeRegistrations(stakeRegistrations)
                        val epoch = getEpochForSlot(block.header.slot)
                        val stakeDelegations = block.toStakeDelegationList(epoch)
                        ledgerRepository.createStakeDelegations(stakeDelegations)
                        if (syncRawTxns) {
                            val rawTransactions = block.toRawTransactionList()
                            ledgerRepository.createRawTransactions(rawTransactions)
                            ledgerRepository.createLedgerUtxoHistory(createdUtxos, blockNumber)
                        }
                    }

                    // Mark spent utxos as spent
                    spendTime += measureTimeMillis {
                        ledgerRepository.spendUtxos(
                            blockNumber = block.header.blockHeight,
                            spentUtxos = block.toSpentUtxoSet()
                        )
                    }

                    // Load any Native asset metadata
                    nativeAssetTime += measureTimeMillis {
                        // Save metadata for CIP-68 reference metadata appearing on createdUtxos datum values
                        val nativeAssetMetadataList = cip68UtxoOutputsTo721MetadataMap(createdUtxos)
                        nativeAssetMetadataList.forEach { (metadataMap, assetList) ->
                            try {
                                ledgerRepository.insertLedgerAssetMetadataList(
                                    metadataMap.extractAssetMetadata(assetList)
                                )
                            } catch (e: Throwable) {
                                log.error("metadataError at block ${block.header.blockHeight}, metadataMap: $metadataMap, assetList: $assetList")
                                throw e
                            }
                        }

                        try {
                            // Save metadata for CIP-25 metadata appearing in tx metadata
                            ledgerRepository.insertLedgerAssetMetadataList(
                                block.toAssetMetadataList(mintedLedgerAssets)
                            )
                        } catch (e: Throwable) {
                            log.error("metadataError at block ${block.header.blockHeight}, mintedLedgerAssets: $mintedLedgerAssets")
                            throw e
                        }
                    }

                    // Insert any native asset log responses
                    nativeAssetTime += measureTimeMillis {
                        if (ledgerAssets.isNotEmpty()) {
                            commitNativeAssetLogTransactions(block, ledgerAssets, createdUtxos)
                        }
                    }
                }

                // Prune any old spent utxos we don't need any longer
                if (pruneUtxos) {
                    pruneTime = measureTimeMillis {
                        // only prune every 10000 blocks
                        if (lastBlock.header.blockHeight % 10_000L == 0L) {
                            ledgerRepository.pruneSpent(lastBlock.header.blockHeight)
                        }
                    }
                }
            }
            // Emit the blocks to the flows
            blocksToCommit.forEach { committedRollForwardFlow.emit(it) }
            blocksToCommit.forEach { confirmedBlockFlow.emit(it.block) }
        }.also { totalTime ->
            val now = Instant.now()
            val tenSecondsAgo = now.minusSeconds(10L)
            if (isTip || tenSecondsAgo.isAfter(lastLoggedCommit)) {
                val blockHeight = blocksToCommit.last().block.header.blockHeight
                val percent = floor(blockHeight.toDouble() / tipBlockHeight.toDouble() * 10000.0) / 100.0
                log.info("commitBlock: block $blockHeight of $tipBlockHeight: %.2f%% committed".format(percent))
                lastLoggedCommit = now
            }
            if ((isTip && totalTime > COMMIT_BLOCKS_WARN_LEVEL_MILLIS) || (totalTime > COMMIT_BLOCKS_ERROR_LEVEL_MILLIS)) {
                log.warn("commitBlocks(${blocksToCommit.size}) total: ${totalTime}ms, rollback: ${rollbackTime}ms, nativeAsset: ${nativeAssetTime}ms, create: ${createTime}ms, spend: ${spendTime}ms, prune: ${pruneTime}ms")
            }

            // Adjust blockBufferSize based on how long it took to commit these blocks
            if (totalTime > COMMIT_BLOCKS_ERROR_LEVEL_MILLIS * 2) {
                val averageTimePerBlock = totalTime / blocksToCommit.size
                // get 10 seconds worth of blocks next time
                blockBufferSize = max(1, ((COMMIT_BLOCKS_ERROR_LEVEL_MILLIS * 2) / averageTimePerBlock).toInt())
            } else if (totalTime < COMMIT_BLOCKS_ERROR_LEVEL_MILLIS) {
                blockBufferSize++
            }
        }
    }

    private fun cip68UtxoOutputsTo721MetadataMap(createdUtxos: Set<CreatedUtxo>): List<Pair<MetadataMap, List<LedgerAsset>>> {
        return createdUtxos.filter { createdUtxo ->
            createdUtxo.nativeAssets.any { nativeAsset ->
                nativeAsset.name.matches(CIP68_REFERENCE_TOKEN_REGEX)
            }
        }.mapNotNull { cip68CreatedUtxo ->
            cip68CreatedUtxo.datum?.let { datum ->
                val cip68PlutusData = datum.cborHexToPlutusData()
                if (cip68PlutusData.hasConstr() && cip68PlutusData.constr == 0) {
                    cip68CreatedUtxo.nativeAssets.filter { nativeAsset ->
                        nativeAsset.name.matches(CIP68_REFERENCE_TOKEN_REGEX)
                    }.map { nativeAsset ->
                        val metadataMap = cip68PlutusData.toMetadataMap(nativeAsset.policy, nativeAsset.name)
                        metadataMap to cip68CreatedUtxo.nativeAssets.map { na ->
                            ledgerRepository.queryLedgerAsset(na.policy, na.name)!!
                        }
                    }
                } else {
                    null
                }
            }
        }.flatten()
    }

    private suspend fun checkBlockRollbacks(blockHeight: Long, transactionIdsInBlock: Set<String>) {
        // See if we're overwriting an existing block due to a rollback
        blockRollbackCache.getIfPresent(blockHeight)?.let { rolledBackBlockTransactionList ->
            handleBlockRollback(rolledBackBlockTransactionList, transactionIdsInBlock)
        }
        blockRollbackCache.put(blockHeight, transactionIdsInBlock)

        // Remove any blocks that were pruned due to this rollback.
        var blockNumber = blockHeight + 1
        var rolledBackBlockTransactionIds = blockRollbackCache.getIfPresent(blockNumber)
        while (rolledBackBlockTransactionIds != null) {
            blockRollbackCache.invalidate(blockNumber)
            blockNumber++
            rolledBackBlockTransactionIds = blockRollbackCache.getIfPresent(blockNumber)
        }
    }

    private suspend fun handleBlockRollback(
        rolledBackBlockTransactionList: Set<String>,
        transactionIdsInBlock: Set<String>,
    ) {
        // get the first transactionId in the rolled-back block that isn't in the new block
        rolledBackBlockTransactionList.find { transactionId -> transactionIdsInBlock.none { it == transactionId } }
            ?.let { firstTransactionIdNotInBlock ->
                val keys = submittedTransactionCache.keys
                val startIndex = keys.indexOfFirst { it == firstTransactionIdNotInBlock }.let { index ->
                    // try to start 20 transactions before we need to
                    if (index < 0) {
                        index
                    } else if (index - 20 < 0) {
                        0
                    } else {
                        index - 20
                    }
                }
                val lastIndex = keys.size - 1
                if (startIndex > -1) {
                    keys.forEachIndexed { index, transactionId ->
                        if (index >= startIndex) {
                            submittedTransactionCache.get(transactionId)?.let { cbor ->
                                val request = SubmitTransactionRequest
                                    .newBuilder()
                                    .setCbor(cbor.toByteString())
                                    .build()
                                when (newmChainService.submitTransaction(request).result) {
                                    "MsgAcceptTx" -> {
                                        log.warn("Re-Submit txid to mempool due to rollback: $transactionId, $index/$lastIndex")
                                    }

                                    else -> {
                                        if (index % 10 == 0 || index == lastIndex) {
                                            log.info("Re-Submit txid to mempool exists already: $transactionId, $index/$lastIndex")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } ?: run {
            log.info("No transactions found to re-submit due to rollback.")
        }
    }

    private fun commitNativeAssetLogTransactions(
        block: Block,
        ledgerAssetList: List<LedgerAsset>,
        createdUtxos: Set<CreatedUtxo>
    ) {
        val ledgerAssets = ledgerRepository.queryLedgerAssets(ledgerAssetList)
        // handle supply changes
        val batch = mutableListOf<ByteArray>()
        batch.addAll(
            ledgerAssets.mapNotNull { ledgerAsset ->
                val bos = ByteArrayOutputStream()
                monitorNativeAssetsResponse {
                    policy = ledgerAsset.policy
                    name = ledgerAsset.name
                    nativeAssetSupplyChange = ledgerAsset.supply.toString()
                    slot = block.header.slot
                    this.block = block.header.blockHeight
                    txHash = ledgerAsset.txId
                }.writeTo(bos)
                bos.toByteArray()
            }
        )

        // handle metadata updates for CIP-25 or if CIP-68 is minted without/before reference token
        batch.addAll(
            ledgerAssets.filter { it.supply > BigInteger.ZERO }.map { ledgerAsset ->
                val metadataLedgerAsset = if (ledgerAsset.name.matches(CIP68_USER_TOKEN_REGEX)) {
                    val name = "$CIP68_REFERENCE_TOKEN_PREFIX${ledgerAsset.name.substring(8)}"
                    ledgerRepository.queryLedgerAsset(ledgerAsset.policy, name)
                        ?.copy(txId = ledgerAsset.txId) ?: run {
                        log.warn("No LedgerAsset found for: '${ledgerAsset.policy}.$name' !")
                        ledgerAsset
                    }
                } else {
                    ledgerAsset
                }

                val ledgerAssetMetadataList =
                    ledgerRepository.queryLedgerAssetMetadataList(metadataLedgerAsset.id!!)
                val bos = ByteArrayOutputStream()
                monitorNativeAssetsResponse {
                    policy = ledgerAsset.policy
                    name = ledgerAsset.name
                    nativeAssetMetadataJson = ledgerAssetMetadataList.to721Json(
                        ledgerAsset.policy,
                        ledgerAsset.name,
                    )
                    slot = block.header.slot
                    this.block = block.header.blockHeight
                    txHash = ledgerAsset.txId
                }.writeTo(bos)
                bos.toByteArray()
            }
        )

        // handle metadata updates for CIP-68
        batch.addAll(
            createdUtxos.mapNotNull { createdUtxo ->
                createdUtxo.datum?.let {
                    createdUtxo.nativeAssets.filter { nativeAsset ->
                        nativeAsset.name.matches(CIP68_REFERENCE_TOKEN_REGEX)
                    }
                }
            }.flatten().flatMap { updatedNativeAsset ->
                val metadataBatch = mutableListOf<ByteArray>()
                ledgerRepository.queryLedgerAsset(
                    updatedNativeAsset.policy,
                    updatedNativeAsset.name
                )?.let { nativeAsset ->
                    val ledgerAssetMetadataList =
                        ledgerRepository.queryLedgerAssetMetadataList(nativeAsset.id!!)
                    val bos = ByteArrayOutputStream()
                    // Create a response for the cip68 reference token itself
                    monitorNativeAssetsResponse {
                        policy = updatedNativeAsset.policy
                        name = updatedNativeAsset.name
                        nativeAssetMetadataJson = ledgerAssetMetadataList.to721Json(
                            updatedNativeAsset.policy,
                            updatedNativeAsset.name
                        )
                        slot = block.header.slot
                        this.block = block.header.blockHeight
                        txHash = ledgerAssets.firstOrNull {
                            it.name == updatedNativeAsset.name && it.policy == updatedNativeAsset.policy
                        }?.txId ?: ""
                    }.writeTo(bos)
                    metadataBatch.add(bos.toByteArray())

                    val prefixes = listOf("000de140", "0014df10", "001bc280") // (222), (333), (444)

                    prefixes.forEach { prefix ->
                        val name = prefix + updatedNativeAsset.name.substring(8)
                        ledgerRepository.queryLedgerAsset(updatedNativeAsset.policy, name)
                            ?.let { nativeAsset ->
                                val bos1 = ByteArrayOutputStream()
                                // Create a response for any cip68 user token based on the reference token
                                monitorNativeAssetsResponse {
                                    this.policy = nativeAsset.policy
                                    this.name = nativeAsset.name
                                    nativeAssetMetadataJson = ledgerAssetMetadataList.to721Json(
                                        nativeAsset.policy,
                                        nativeAsset.name
                                    )
                                    slot = block.header.slot
                                    this.block = block.header.blockHeight
                                    txHash = ledgerAssets.firstOrNull {
                                        it.name == nativeAsset.name && it.policy == nativeAsset.policy
                                    }?.txId ?: ""
                                }.writeTo(bos1)
                                metadataBatch.add(bos1.toByteArray())
                            }
                    }
                }
                metadataBatch
            }
        )

        // Update the db for native asset changes
        NativeAssetMonitorLogTable.batchInsert(batch, shouldReturnGeneratedValues = false) {
            this[NativeAssetMonitorLogTable.monitorNativeAssetsResponseBytes] = it
            this[NativeAssetMonitorLogTable.blockNumber] = block.header.blockHeight
        }
    }

    companion object {
        private const val COMMIT_BLOCKS_WARN_LEVEL_MILLIS = 1_000L
        private const val COMMIT_BLOCKS_ERROR_LEVEL_MILLIS = 5_000L
        private const val CIP68_REFERENCE_TOKEN_PREFIX = "000643b0"
        private val CIP68_REFERENCE_TOKEN_REGEX = Regex("^000643b0.*$") // (100)TokenName
        private val CIP68_USER_TOKEN_REGEX =
            Regex("^00(0de14|14df1|1bc28)0.*$") // (222)TokenName, (333)TokenName, (444)TokenName
    }
}
