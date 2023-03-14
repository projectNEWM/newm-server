package io.newm.chain.daemon

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.google.iot.cbor.CborReader
import com.google.protobuf.kotlin.toByteString
import io.ktor.server.application.ApplicationEnvironment
import io.newm.chain.cardano.getEpochForSlot
import io.newm.chain.config.Config
import io.newm.chain.database.repository.ChainRepository
import io.newm.chain.database.repository.LedgerRepository
import io.newm.chain.database.table.AddressTxLogTable
import io.newm.chain.grpc.ExUnits
import io.newm.chain.grpc.MonitorAddressResponse
import io.newm.chain.grpc.NativeAsset
import io.newm.chain.grpc.NewmChainService
import io.newm.chain.grpc.Redeemer
import io.newm.chain.grpc.RedeemerTag
import io.newm.chain.grpc.SubmitTransactionRequest
import io.newm.chain.grpc.Utxo
import io.newm.chain.ledger.SubmittedTransactionCache
import io.newm.chain.logging.captureToSentry
import io.newm.chain.model.NativeAssetMetadata
import io.newm.chain.util.b64ToByteArray
import io.newm.chain.util.hexToByteArray
import io.newm.chain.util.toAssetMetadataList
import io.newm.chain.util.toChainBlock
import io.newm.chain.util.toCreatedUtxoMap
import io.newm.chain.util.toCreatedUtxoSet
import io.newm.chain.util.toHexString
import io.newm.chain.util.toRawTransactionList
import io.newm.chain.util.toSpentUtxoMap
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
import io.newm.kogmios.protocols.model.Asset
import io.newm.kogmios.protocols.model.Block
import io.newm.kogmios.protocols.model.BlockAllegra
import io.newm.kogmios.protocols.model.BlockAlonzo
import io.newm.kogmios.protocols.model.BlockBabbage
import io.newm.kogmios.protocols.model.BlockMary
import io.newm.kogmios.protocols.model.BlockShelley
import io.newm.kogmios.protocols.model.CompactGenesis
import io.newm.kogmios.protocols.model.MetadataBytes
import io.newm.kogmios.protocols.model.MetadataList
import io.newm.kogmios.protocols.model.MetadataMap
import io.newm.kogmios.protocols.model.MetadataString
import io.newm.kogmios.protocols.model.MetadataValue
import io.newm.kogmios.protocols.model.OriginString
import io.newm.kogmios.protocols.model.PointDetail
import io.newm.kogmios.protocols.model.PointDetailOrOrigin
import io.newm.shared.ext.debug
import io.newm.shared.ext.getConfigBoolean
import io.newm.shared.ext.getConfigInt
import io.newm.shared.ext.getConfigSplitStrings
import io.newm.shared.ext.getConfigString
import io.newm.shared.koin.inject
import io.newm.txbuilder.ktx.cborHexToPlutusData
import io.newm.txbuilder.ktx.toPlutusData
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.core.parameter.parametersOf
import org.slf4j.Logger
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.math.BigInteger
import java.time.Duration
import java.time.Instant
import java.util.*
import kotlin.math.max
import kotlin.system.measureTimeMillis

class BlockDaemon(
    private val environment: ApplicationEnvironment,
    private val chainRepository: ChainRepository,
    private val ledgerRepository: LedgerRepository,
    private val submittedTransactionCache: SubmittedTransactionCache,
    private val blockFlow: MutableSharedFlow<Block>,
) : Daemon {
    override val log: Logger by inject { parametersOf("BlockDaemon") }

    private val server by lazy { environment.getConfigString("ogmios.server") }
    private val port by lazy { environment.getConfigInt("ogmios.port") }
    private val secure by lazy { environment.getConfigBoolean("ogmios.secure") }
    private val syncRawTxns by lazy { environment.getConfigBoolean("newmchain.syncRawTxns") }
    private val monitorAddresses by lazy {
        environment.getConfigSplitStrings("newmchain.monitorAddresses")
    }

    private val blockBuffer: MutableList<Block> = mutableListOf()

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
        startChainSync()
        startMonitorAddresses()
        log.info("startup complete.")
    }

    override fun shutdown() {
        log.info("shutdown complete.")
    }

    private fun startChainSync() {
        launch {
            // Fetch network genesis information
            createStateQueryClient(
                websocketHost = server,
                websocketPort = port,
                secure = secure,
                ogmiosCompact = true,
            ).use { stateQueryClient ->
                connect(stateQueryClient)
                fetchNetworkInfo(stateQueryClient)
            }

            while (true) {
                val localChainSyncClient = createChainSyncClient(
                    websocketHost = server,
                    websocketPort = port,
                    secure = secure,
                    ogmiosCompact = false,
                )
                try {
                    localChainSyncClient.use {
                        connect(localChainSyncClient)
                        syncBlockchain(localChainSyncClient)
                    }
                } catch (e: Throwable) {
                    log.error("Error syncing blockchain!", e)
                    if (e !is TimeoutCancellationException) {
                        e.captureToSentry()
                    }
                    log.info("Wait 10 seconds to retry...")
                    try {
                        // clear out everything so far since we're going to retry...
                        blockBuffer.clear()
                    } catch (_: Throwable) {
                    }
                    delay(RETRY_DELAY_MILLIS)
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
                        val blockHeight = rollForward.block.header.blockHeight
                        tipBlockHeight = max(blockHeight, rollForward.tip.blockNo)
                        isTip = blockHeight == tipBlockHeight
                        processBlock(rollForward.block, isTip)
                        val now = Instant.now()
                        val tenSecondsAgo = now.minusSeconds(10L)
                        if (isTip || tenSecondsAgo.isAfter(lastLogged)) {
                            val percent = blockHeight.toDouble() / tipBlockHeight.toDouble() * 100.0
                            log.info("RollForward: block $blockHeight of $tipBlockHeight: %.2f%% sync'd".format(percent))
                            lastLogged = now
                        }
                    }
                }
            }
        }
    }

    private suspend fun processBlock(block: Block, isTip: Boolean) {
        blockBuffer.add(block)
        if (blockBuffer.size == BLOCK_BUFFER_SIZE || isTip) {
            // Create a copy of the list
            val blocksToCommit = blockBuffer.toMutableList()
            blockBuffer.clear()
            commitBlocks(blocksToCommit, isTip)
        }
    }

    private var lastLoggedCommit = Instant.EPOCH
    private suspend fun commitBlocks(blocksToCommit: List<Block>, isTip: Boolean) {
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
//            warnLongQueriesDuration = 200L
                blocksToCommit.forEach { block ->
//                    if (block.header.blockHeight <= 60) {
//                        log.warn("block: $block")
//                    }
                    // Mark same block number as rolled back
                    rollbackTime += measureTimeMillis {
                        ledgerRepository.doRollback(block.header.blockHeight)

                        // Handle re-submitting any of our own transactions that got rolled back
                        val tip = isTip && block == blocksToCommit.last()
                        if (tip || blockRollbackCache.getIfPresent(block.header.blockHeight) != null) {
                            val ourTransactionIdsInBlock =
                                block.toCreatedUtxoSet().map { createdUtxo -> createdUtxo.hash }.toSet()
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

                    // Load any Native asset metadata
                    nativeAssetTime += measureTimeMillis {
                        // Store just name/image/description
                        val nativeAssetMetadataSet =
                            ledgerRepository.upcertNativeAssets(extractBlockTokenMetadataSet(block))

                        // Store ALL nested metadata items
                        ledgerRepository.insertLedgerAssetMetadataList(
                            block.toAssetMetadataList(
                                nativeAssetMetadataSet
                            )
                        )
                    }

                    // Insert unspent utxos and stake delegations/de-registrations
                    createTime += measureTimeMillis {
                        val slotNumber = block.header.slot
                        val blockNumber = block.header.blockHeight
                        val createdUtxos = block.toCreatedUtxoSet()
                        ledgerRepository.createUtxos(slotNumber, blockNumber, createdUtxos)
                        val stakeRegistrations = block.toStakeRegistrationList()
                        ledgerRepository.createStakeRegistrations(stakeRegistrations)
                        val epoch = getEpochForSlot(block.header.slot)
                        val stakeDelegations = block.toStakeDelegationList(epoch)
                        ledgerRepository.createStakeDelegations(stakeDelegations)
                        if (syncRawTxns) {
                            val rawTransactions = block.toRawTransactionList()
                            ledgerRepository.createRawTransactions(rawTransactions)
                        }
                    }

                    // Mark spent utxos as spent
                    spendTime += measureTimeMillis {
                        ledgerRepository.spendUtxos(
                            slotNumber = block.header.slot,
                            blockNumber = block.header.blockHeight,
                            spentUtxos = block.toSpentUtxoSet()
                        )
                    }
                }

                val latestBlock = blocksToCommit.last()
                // Prune any old spent utxos we don't need any longer
                pruneTime = measureTimeMillis {
                    // only prune every 1000 blocks
                    if (latestBlock.header.blockHeight % 1000L == 0L) {
                        ledgerRepository.pruneSpent(latestBlock.header.slot)
                    }
                }
            }
            // Emit all these blocks for further processing now that basic ledger entry is done
            blocksToCommit.forEach { blockFlow.emit(it) }
        }.also { totalTime ->
            val now = Instant.now()
            val tenSecondsAgo = now.minusSeconds(10L)
            if (isTip || tenSecondsAgo.isAfter(lastLoggedCommit)) {
                val blockHeight = blocksToCommit.last().header.blockHeight
                val percent = blockHeight.toDouble() / tipBlockHeight.toDouble() * 100.0
                log.info("commitBlock: block $blockHeight of $tipBlockHeight: %.2f%% committed".format(percent))
                lastLoggedCommit = now
            }
            if (isTip && totalTime > COMMIT_BLOCKS_WARN_LEVEL_MILLIS) {
                log.warn("commitBlocks() total: ${totalTime}ms, rollback: ${rollbackTime}ms, nativeAsset: ${nativeAssetTime}ms, create: ${createTime}ms, spend: ${spendTime}ms, prune: ${pruneTime}ms")
            }
        }
    }

    private fun extractBlockTokenMetadataSet(block: Block): Set<NativeAssetMetadata> {
        val sevenTwentyOneMetadataMap = when (block) {
            is BlockShelley -> emptyList()
            is BlockAllegra -> emptyList()
            is BlockMary -> block.mary.body.mapNotNull {
                val assetsMinted = it.body.mint.assets
                (it.metadata?.body?.blob?.get(NATIVE_ASSET_METADATA_TAG) as? MetadataMap)?.let { metadataMap ->
                    Pair(metadataMap, assetsMinted)
                }
            }

            is BlockAlonzo -> block.alonzo.body.mapNotNull {
                val assetsMinted = it.body.mint.assets
                (it.metadata?.body?.blob?.get(NATIVE_ASSET_METADATA_TAG) as? MetadataMap)?.let { metadataMap ->
                    Pair(metadataMap, assetsMinted)
                }
            }

            is BlockBabbage -> block.babbage.body.mapNotNull {
                val assetsMinted = it.body.mint.assets
                (it.metadata?.body?.blob?.get(NATIVE_ASSET_METADATA_TAG) as? MetadataMap)?.let { metadataMap ->
                    Pair(metadataMap, assetsMinted)
                }
            }
        }

        return if (sevenTwentyOneMetadataMap.isNotEmpty()) {
            extractSevenTwentyOneMetadata(sevenTwentyOneMetadataMap, block.header.blockHeight)
        } else {
            emptySet()
        }
    }

    private fun extractSevenTwentyOneMetadata(
        metadataMaps: List<Pair<MetadataMap, List<Asset>>>,
        blockHeight: Long
    ): Set<NativeAssetMetadata> {
        val nativeAssetMetadata: Set<NativeAssetMetadata> = metadataMaps.flatMap { (metadataMap, assetsMinted) ->
            metadataMap.mapNotNull { entry ->
                val assetPolicy = extractNativeAssetPolicy(entry, blockHeight)
                assetPolicy?.let { ap ->
                    (entry.value as? MetadataMap)?.let { policyMetadataMap ->
                        extractNativeAssetsForPolicy(assetsMinted, policyMetadataMap, ap, blockHeight)
                    }
                }
            }
        }.flatten().toSet()

        return nativeAssetMetadata
    }

    private fun extractNativeAssetsForPolicy(
        assetsMinted: List<Asset>,
        policyMetadataMap: MetadataMap,
        ap: String,
        blockHeight: Long,
    ): List<NativeAssetMetadata> {
        return policyMetadataMap.mapNotNull { (key, value) ->
            val assetName = extractNativeAssetName(key, value, blockHeight)
            assetName?.let { an ->
                // We have metadata and we actually minted the asset in this tx
                assetsMinted.find {
                    it.quantity > BigInteger.ZERO && it.policyId == ap &&
                        (it.name == an || it.name.toByteArray().toHexString() == an)
                }?.let {
                    (value as? MetadataMap)?.let { tokenDetailsMap ->
                        extractNativeAssetDetails(tokenDetailsMap, an, ap, blockHeight)
                    } ?: run {
                        log.error("! value is not MetadataMap!: $value")
                        null
                    }
                }
            }
        }
    }

    private fun extractNativeAssetDetails(
        tokenDetailsMap: MetadataMap,
        an: String,
        ap: String,
        blockHeight: Long,
    ): NativeAssetMetadata {
        val metadataName = extractMetadataName(tokenDetailsMap)
        val metadataImage = extractMetadataImage(tokenDetailsMap, blockHeight)
        val metadataDescription = extractMetadataDescription(tokenDetailsMap, blockHeight)
        return NativeAssetMetadata(
            assetName = an,
            assetPolicy = ap,
            metadataName = metadataName,
            metadataImage = metadataImage,
            metadataDescription = metadataDescription,
        )
    }

    private fun extractMetadataName(tokenDetailsMap: MetadataMap): String {
        return (tokenDetailsMap[ASSET_NAME_METADATA_TAG] as? MetadataString)?.string ?: ""
    }

    private fun extractMetadataDescription(tokenDetailsMap: MetadataMap, blockHeight: Long): String? {
        val metadataDescription =
            when (
                val metadataDescriptionValue =
                    tokenDetailsMap[ASSET_DESCRIPTION_METADATA_TAG]
            ) {
                is MetadataString -> metadataDescriptionValue.string
                is MetadataList -> metadataDescriptionValue.joinToString(
                    separator = " "
                ) { metadataDescriptionPart ->
                    (metadataDescriptionPart as? MetadataString)?.string
                        ?: ""
                }

                null -> null
                else -> {
                    log.warn("block: $blockHeight, metadataDescription type was: ${metadataDescriptionValue.javaClass.name}!: $metadataDescriptionValue")
                    null
                }
            }
        return metadataDescription
    }

    private fun extractMetadataImage(tokenDetailsMap: MetadataMap, blockHeight: Long): String {
        return when (val metadataImageValue = tokenDetailsMap[ASSET_IMAGE_METADATA_TAG]) {
            is MetadataString -> metadataImageValue.string
            is MetadataList -> metadataImageValue.joinToString(
                separator = ""
            ) { metadataImagePart ->
                (metadataImagePart as? MetadataString)?.string ?: ""
            }

            null -> null
            else -> {
                log.warn("block: $blockHeight, metadataImage type was: ${metadataImageValue.javaClass.name}!: $metadataImageValue")
                null
            }
        } ?: ""
    }

    private fun extractNativeAssetName(key: MetadataValue, value: MetadataValue, blockHeight: Long): String? {
        return when (key) {
            is MetadataBytes -> key.bytes.b64ToByteArray().toHexString()
            is MetadataString -> key.string.toByteArray().toHexString()
            else -> {
                log.warn("block: $blockHeight, assetName metadata type was: ${key.javaClass.name}!: $key -> $value")
                null
            }
        }
    }

    private fun extractNativeAssetPolicy(entry: Map.Entry<MetadataValue, MetadataValue>, blockHeight: Long): String? {
        return when (val assetPolicyKey = entry.key) {
            is MetadataBytes -> assetPolicyKey.bytes.b64ToByteArray().toHexString()
            is MetadataString -> assetPolicyKey.string.lowercase()
            else -> {
                log.warn("block: $blockHeight, assetPolicy metadata type was: ${assetPolicyKey.javaClass.name}!: $assetPolicyKey -> ${entry.value}")
                null
            }
        }
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

    private fun startMonitorAddresses() {
        launch {
            while (true) {
                try {
                    log.warn("startMonitorAddresses: $monitorAddresses")
                    val blockDelayQueue = LinkedList<Block>()
                    blockFlow.collect { block ->
                        // Prune any rolled back blocks
                        blockDelayQueue.removeIf { bdqBlock -> bdqBlock.header.blockHeight >= block.header.blockHeight }
                        // Add this new block to our queue
                        blockDelayQueue.add(block)
                        // Wait for 3 confirmations before processing
                        if (blockDelayQueue.size > 3) {
                            val oldestBlock = blockDelayQueue.poll()
                            val spentUtxoMap = oldestBlock.toSpentUtxoMap()
                            val createdUtxoMap = oldestBlock.toCreatedUtxoMap()
                            val transactionIds = spentUtxoMap.keys + createdUtxoMap.keys
                            val monitorAddressResponsesMap = mutableMapOf<String, MutableList<MonitorAddressResponse>>()

                            transactionIds.forEach { transactionId ->
                                monitorAddresses.forEach { monitorAddress ->
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
                                                .setBlock(oldestBlock.header.blockHeight)
                                                .setSlot(oldestBlock.header.slot)
                                                .setTxId(transactionId)
                                                .addAllSpentUtxos(spentAddressUtxos)
                                                .addAllCreatedUtxos(createdAddressUtxos)
                                                .apply {
                                                    when (oldestBlock) {
                                                        is BlockAlonzo -> oldestBlock.alonzo.body.first { it.id == transactionId }.witness
                                                        is BlockBabbage -> oldestBlock.babbage.body.first { it.id == transactionId }.witness
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
                            }
                            transaction {
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
                                    this[AddressTxLogTable.address] = monitorAddress
                                    this[AddressTxLogTable.txId] = monitorAddressResponse.txId
                                    val bos = ByteArrayOutputStream()
                                    monitorAddressResponse.writeTo(bos)
                                    this[AddressTxLogTable.monitorAddressResponseBytes] = bos.toByteArray()
                                }
                            }
                        }
                    }
                } catch (e: Throwable) {
                    if (e !is CancellationException) {
                        log.error(e.message, e)
                    }
                }
            }
        }
    }

    companion object {
        private const val RETRY_DELAY_MILLIS = 10_000L
        private const val BLOCK_BUFFER_SIZE = 100
        private const val COMMIT_BLOCKS_WARN_LEVEL_MILLIS = 1_000L
        private val NATIVE_ASSET_METADATA_TAG = 721.toBigInteger()
        private val ASSET_NAME_METADATA_TAG = MetadataString("name")
        private val ASSET_IMAGE_METADATA_TAG = MetadataString("image")
        private val ASSET_DESCRIPTION_METADATA_TAG = MetadataString("description")
    }
}
