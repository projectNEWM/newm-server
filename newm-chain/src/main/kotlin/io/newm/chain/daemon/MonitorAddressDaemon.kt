package io.newm.chain.daemon

import com.google.iot.cbor.CborReader
import io.ktor.server.application.ApplicationEnvironment
import io.newm.chain.config.Config
import io.newm.chain.database.entity.MonitoredAddressChain
import io.newm.chain.database.repository.ChainRepository
import io.newm.chain.database.repository.LedgerRepository
import io.newm.chain.database.table.AddressTxLogTable
import io.newm.chain.grpc.ExUnits
import io.newm.chain.grpc.MonitorAddressResponse
import io.newm.chain.grpc.NativeAsset
import io.newm.chain.grpc.Redeemer
import io.newm.chain.grpc.RedeemerTag
import io.newm.chain.grpc.Utxo
import io.newm.chain.logging.captureToSentry
import io.newm.chain.util.hexToByteArray
import io.newm.chain.util.toCreatedUtxoMap
import io.newm.chain.util.toSpentUtxoMap
import io.newm.kogmios.ChainSyncClient
import io.newm.kogmios.Client
import io.newm.kogmios.StateQueryClient
import io.newm.kogmios.createChainSyncClient
import io.newm.kogmios.createStateQueryClient
import io.newm.kogmios.protocols.messages.IntersectionFound
import io.newm.kogmios.protocols.messages.RollBackward
import io.newm.kogmios.protocols.messages.RollForward
import io.newm.kogmios.protocols.messages.RollForwardData
import io.newm.kogmios.protocols.model.Block
import io.newm.kogmios.protocols.model.BlockAllegra
import io.newm.kogmios.protocols.model.BlockAlonzo
import io.newm.kogmios.protocols.model.BlockBabbage
import io.newm.kogmios.protocols.model.BlockMary
import io.newm.kogmios.protocols.model.BlockShelley
import io.newm.kogmios.protocols.model.CompactGenesis
import io.newm.kogmios.protocols.model.OriginString
import io.newm.kogmios.protocols.model.PointDetail
import io.newm.kogmios.protocols.model.PointDetailOrOrigin
import io.newm.shared.daemon.Daemon
import io.newm.shared.daemon.Daemon.Companion.RETRY_DELAY_MILLIS
import io.newm.shared.koin.inject
import io.newm.shared.ktx.getConfigBoolean
import io.newm.shared.ktx.getConfigInt
import io.newm.shared.ktx.getConfigString
import io.newm.txbuilder.ktx.cborHexToPlutusData
import io.newm.txbuilder.ktx.toPlutusData
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.koin.core.parameter.parametersOf
import org.slf4j.Logger
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.time.Instant
import kotlin.math.floor
import kotlin.math.max
import kotlin.system.measureTimeMillis

class MonitorAddressDaemon(
    private val environment: ApplicationEnvironment,
    private val chainRepository: ChainRepository,
    private val ledgerRepository: LedgerRepository,
    private val monitorAddress: String,
) : Daemon {
    override val log: Logger by inject { parametersOf("Monitor(${monitorAddress.take(15)}...)") }
    private val server by lazy { environment.getConfigString("ogmios.server") }
    private val port by lazy { environment.getConfigInt("ogmios.port") }
    private val secure by lazy { environment.getConfigBoolean("ogmios.secure") }
    private val blockDaemon by inject<BlockDaemon>()
    private val rollForwardFlow = MutableSharedFlow<RollForwardData>(
        replay = 0,
        extraBufferCapacity = 0,
        onBufferOverflow = BufferOverflow.SUSPEND
    )
    private var blockBufferSize = 1
    private val blockBuffer: MutableList<Block> = mutableListOf()

    private var tipBlockHeight = 0L

    private var isTipReached = false

    override fun start() {
        log.info("starting...")
        startCheckSyncMode()
        startProcessBlock()
        startChainSync()
        log.info("startup complete.")
    }

    override fun shutdown() {
        log.info("shutdown complete.")
    }

    private fun startCheckSyncMode() {
        val pointDetailList = chainRepository.getFindIntersectPairsAddressChain(monitorAddress)
        if (pointDetailList.size == 1 && pointDetailList[0].slot < 0L) {
            // we're already on the tip for this particular address, so we don't need to sync from a different point
            // than the normal blockdaemon.
            log.info("Use BlockDaemon to sync blockchain.")
            isTipReached = true
        }
    }

    private fun startChainSync() {
        if (isTipReached) {
            return
        }
        launch {
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

            while (!isTipReached) {
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
            // tip reached. mark it in the database
            log.info("Tip reached. ChainSync Exiting...")
            chainRepository.markTipMonitoredAddressChain(monitorAddress)
        }
    }

    private fun startProcessBlock() {
        var blockDaemonHeight = Long.MAX_VALUE
        launch {
            // Collect from blockDaemon
            blockDaemon.committedRollForwardFlow.collect { rollForward ->
                blockDaemonHeight = rollForward.block.header.blockHeight
                if (!isTipReached) {
                    return@collect
                }
                tipBlockHeight = max(blockDaemonHeight, rollForward.tip.blockNo)
                val isTip = blockDaemonHeight == tipBlockHeight
                processBlock(rollForward.block, isTip)
            }
        }
        if (isTipReached) {
            return
        }
        launch {
            // Collect from local sync until we reach the tip
            rollForwardFlow.takeWhile { !isTipReached }.collect { rollForward ->
                val blockHeight = rollForward.block.header.blockHeight
                tipBlockHeight = max(blockHeight, rollForward.tip.blockNo)
                val isTip = blockHeight == tipBlockHeight
                isTipReached = isTip || blockHeight >= blockDaemonHeight
                processBlock(rollForward.block, isTip)
            }
        }
    }

    private suspend fun processBlock(block: Block, isTip: Boolean) {
        blockBuffer.add(block)
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
    private suspend fun commitBlocks(blocksToCommit: List<Block>, isTip: Boolean) {
//        if (!isTip) {
//            log.warn("starting commitBlocks()...")
//        }
        var rollbackTime = 0L
        var createTime = 0L
        var pruneTime = 0L
        val firstBlock = blocksToCommit.first()
        val latestBlock = blocksToCommit.last()
        measureTimeMillis {
            newSuspendedTransaction {
                warnLongQueriesDuration = 1000L
                rollbackTime += measureTimeMillis {
                    chainRepository.rollbackMonitoredAddressChain(monitorAddress, firstBlock.header.blockHeight)
                    AddressTxLogTable.deleteWhere {
                        (address eq monitorAddress) and (blockNumber greaterEq firstBlock.header.blockHeight)
                    }
                }

                blocksToCommit.forEach { block ->
                    // Insert any monitor address tx log responses
                    createTime += measureTimeMillis {
                        commitMonitorAddressTransactions(block)
                    }
                }

                if (!isTipReached) {
                    createTime += measureTimeMillis {
                        // Insert the last processed block into the database
                        chainRepository.insertMonitoredAddressChain(
                            MonitoredAddressChain(
                                address = monitorAddress,
                                height = latestBlock.header.blockHeight,
                                slot = latestBlock.header.slot,
                                hash = when (latestBlock) {
                                    is BlockShelley -> latestBlock.shelley.headerHash
                                    is BlockAllegra -> latestBlock.allegra.headerHash
                                    is BlockMary -> latestBlock.mary.headerHash
                                    is BlockAlonzo -> latestBlock.alonzo.headerHash
                                    is BlockBabbage -> latestBlock.babbage.headerHash
                                },
                            )
                        )
                    }
                }

                // Prune any old stuff we don't need any longer
                pruneTime = measureTimeMillis {
                    // only prune every 10000 blocks
                    if (latestBlock.header.blockHeight % 10_000L == 0L) {
                        chainRepository.pruneMonitoredAddressChainHistory(
                            monitorAddress,
                            latestBlock.header.blockHeight
                        )
                    }
                }
            }
        }.also { totalTime ->
            val now = Instant.now()
            val tenSecondsAgo = now.minusSeconds(10L)
            if (isTip || tenSecondsAgo.isAfter(lastLoggedCommit)) {
                val blockHeight = latestBlock.header.blockHeight
                val percent = floor(blockHeight.toDouble() / tipBlockHeight.toDouble() * 10000.0) / 100.0
                if (percent < 100.0) {
                    log.info("commitBlock: block $blockHeight of $tipBlockHeight: %.2f%% committed".format(percent))
                }
                lastLoggedCommit = now
            }
            if ((isTip && totalTime > COMMIT_BLOCKS_WARN_LEVEL_MILLIS) || (totalTime > COMMIT_BLOCKS_ERROR_LEVEL_MILLIS)) {
                log.warn("commitBlocks(${blocksToCommit.size}) total: ${totalTime}ms, rollback: ${rollbackTime}ms, create: ${createTime}ms, prune: ${pruneTime}ms")
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

    private fun commitMonitorAddressTransactions(block: Block) {
        val spentUtxoMap = block.toSpentUtxoMap()
        val createdUtxoMap = block.toCreatedUtxoMap()
        val transactionIds = spentUtxoMap.keys + createdUtxoMap.keys
        val monitorAddressResponsesMap = mutableMapOf<String, MutableList<MonitorAddressResponse>>()

        transactionIds.forEach { transactionId ->
            val spentAddressUtxos: List<Utxo> =
                spentUtxoMap[transactionId]?.mapNotNull { spentUtxo ->
                    ledgerRepository.queryUtxoHavingAddress(
                        monitorAddress,
                        spentUtxo.hash,
                        spentUtxo.ix.toInt()
                    )
                }?.map { utxo ->
                    Utxo.newBuilder().apply {
                        hash = utxo.hash
                        ix = utxo.ix
                        lovelace = utxo.lovelace.toString()
                        utxo.datumHash?.let { datumHash = it }
                        utxo.datum?.let {
                            datum = it.cborHexToPlutusData()
                        }
                        utxo.nativeAssets.forEach { nativeAsset ->
                            addNativeAssets(
                                NativeAsset.newBuilder().apply {
                                    policy = nativeAsset.policy
                                    name = nativeAsset.name
                                    amount = nativeAsset.amount.toString()
                                }
                            )
                        }
                    }.build()
                }.orEmpty()

            val createdAddressUtxos = createdUtxoMap[transactionId]?.filter { createdUtxo ->
                createdUtxo.address == monitorAddress
            }?.map { utxo ->
                Utxo.newBuilder().apply {
                    hash = utxo.hash
                    ix = utxo.ix
                    lovelace = utxo.lovelace.toString()
                    utxo.datumHash?.let { datumHash = it }
                    utxo.datum?.let {
                        datum = it.cborHexToPlutusData()
                    }
                    utxo.nativeAssets.forEach { nativeAsset ->
                        addNativeAssets(
                            NativeAsset.newBuilder().apply {
                                policy = nativeAsset.policy
                                name = nativeAsset.name
                                amount = nativeAsset.amount.toString()
                            }
                        )
                    }
                }.build()
            }.orEmpty()

            if (spentAddressUtxos.isNotEmpty() || createdAddressUtxos.isNotEmpty()) {
                val monitorAddressResponses = monitorAddressResponsesMap[monitorAddress]
                    ?: mutableListOf<MonitorAddressResponse>().also {
                        monitorAddressResponsesMap[monitorAddress] = it
                    }
                monitorAddressResponses.add(
                    MonitorAddressResponse
                        .newBuilder()
                        .setBlock(block.header.blockHeight)
                        .setSlot(block.header.slot)
                        .setTxId(transactionId)
                        .addAllSpentUtxos(spentAddressUtxos)
                        .addAllCreatedUtxos(createdAddressUtxos)
                        .apply {
                            when (block) {
                                is BlockAlonzo -> block.alonzo.body.first { it.id == transactionId }.witness
                                is BlockBabbage -> block.babbage.body.first { it.id == transactionId }.witness
                                else -> null
                            }?.let { witness ->
                                witness.datums?.let { datums ->
                                    putAllDatums(
                                        datums.entries.associate { entry ->
                                            Pair(
                                                entry.key,
                                                entry.value.cborHexToPlutusData(),
                                            )
                                        }
                                    )
                                }
                                witness.redeemers?.let { redeemers ->
                                    putAllRedeemers(
                                        redeemers.entries.associate { (key, txRedeemer) ->
                                            // log.debug("key, txRedeemer.redeemer: $key, ${txRedeemer.redeemer}")
                                            val txRedeemerCbor = CborReader
                                                .createFromByteArray(txRedeemer.redeemer.hexToByteArray())
                                                .readDataItem()
                                            val (redeemerTag, redeemerIndex) = key.split(":")
                                                .let {
                                                    Pair(
                                                        when (it[0]) {
                                                            "spend" -> RedeemerTag.SPEND
                                                            "mint" -> RedeemerTag.MINT
                                                            "certificate" -> RedeemerTag.CERT
                                                            "withdrawal" -> RedeemerTag.REWARD
                                                            else -> throw IllegalArgumentException(
                                                                "Unknown redeemer tag"
                                                            )
                                                        },
                                                        it[1].toLong()
                                                    )
                                                }

                                            val plutusData =
                                                txRedeemerCbor.toPlutusData(txRedeemer.redeemer)
                                            val exUnits = ExUnits.newBuilder()
                                                .setMem(txRedeemer.executionUnits.memory.toLong())
                                                .setSteps(txRedeemer.executionUnits.steps.toLong())
                                                .build()

                                            Pair(
                                                key,
                                                Redeemer.newBuilder()
                                                    .setTag(redeemerTag)
                                                    .setIndex(redeemerIndex)
                                                    .setData(plutusData)
                                                    .setExUnits(exUnits)
                                                    .build()
                                            )
                                        }
                                    )
                                }
                            }
                        }
                        .build()
                )
            }
        }
        val batch =
            monitorAddressResponsesMap.flatMap { (monitorAddress, monitorAddressResponsesList) ->
                monitorAddressResponsesList.map {
                    Pair(monitorAddress, it)
                }
            }

        AddressTxLogTable.batchInsert(
            batch,
            shouldReturnGeneratedValues = false
        ) { (monitorAddress, monitorAddressResponse) ->
            this[AddressTxLogTable.blockNumber] = block.header.blockHeight
            this[AddressTxLogTable.address] = monitorAddress
            this[AddressTxLogTable.txId] = monitorAddressResponse.txId
            val bos = ByteArrayOutputStream()
            monitorAddressResponse.writeTo(bos)
            this[AddressTxLogTable.monitorAddressResponseBytes] = bos.toByteArray()
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
        val intersectPoints: MutableList<PointDetailOrOrigin> =
            chainRepository.getFindIntersectPairsAddressChain(monitorAddress).toMutableList()
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
        var isTip: Boolean
        do {
            val response = client.requestNext(
                timeoutMs = Client.DEFAULT_REQUEST_TIMEOUT_MS
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
        } while (!isTipReached)
    }

    companion object {
        private const val COMMIT_BLOCKS_WARN_LEVEL_MILLIS = 1_000L
        private const val COMMIT_BLOCKS_ERROR_LEVEL_MILLIS = 5_000L
    }
}
