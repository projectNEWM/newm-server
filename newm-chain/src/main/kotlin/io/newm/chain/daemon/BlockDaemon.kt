package io.newm.chain.daemon

import io.ktor.server.application.ApplicationEnvironment
import io.newm.chain.cardano.getEpochForSlot
import io.newm.chain.config.Config
import io.newm.chain.database.repository.ChainRepository
import io.newm.chain.database.repository.LedgerRepository
import io.newm.chain.logging.captureToSentry
import io.newm.chain.model.NativeAssetMetadata
import io.newm.chain.util.b64ToByteArray
import io.newm.chain.util.toAssetMetadataList
import io.newm.chain.util.toChainBlock
import io.newm.chain.util.toCreatedUtxoSet
import io.newm.chain.util.toHexString
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
import io.newm.kogmios.protocols.model.PointDetail
import io.newm.server.di.inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.core.parameter.parametersOf
import org.slf4j.Logger
import java.io.IOException
import java.math.BigInteger
import java.time.Instant
import kotlin.math.max
import kotlin.system.measureTimeMillis

class BlockDaemon(
    private val environment: ApplicationEnvironment
) : Daemon {
    override val log: Logger by inject { parametersOf("BlockDaemon") }

    private val server by lazy { environment.config.property("ogmios.server").getString() }
    private val port by lazy { environment.config.property("ogmios.port").getString().toInt() }
    private val secure by lazy { environment.config.property("ogmios.secure").getString().toBoolean() }
    private val syncRawTxns by lazy { environment.config.property("newmchain.syncRawTxns").getString().toBoolean() }

    private val blockBuffer: MutableList<Block> = mutableListOf()

    private var tipBlockHeight = 0L

    override fun start() {
        log.info("starting...")
        startChainSync()
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
                    e.captureToSentry()
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
        val intersectPoints = ChainRepository.getFindIntersectPairs().toMutableList()
        intersectPoints.add(
            PointDetail(
                slot = environment.config.property("ogmios.startSlot").getString().toLong(),
                hash = environment.config.property("ogmios.startHash").getString()
            )
        )
        val msgFindIntersectResponse = client.findIntersect(intersectPoints)

        if (msgFindIntersectResponse.result !is IntersectionFound) {
            throw IllegalStateException("Error finding blockchain intersect!")
        }
    }

    private suspend fun requestBlocks(client: ChainSyncClient) {
        var lastLogged = Instant.EPOCH
        var isTip = false
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

    private fun processBlock(block: Block, isTip: Boolean) {
        blockBuffer.add(block)
        if (blockBuffer.size == BLOCK_BUFFER_SIZE || isTip) {
            // Create a copy of the list
            val blocksToCommit = blockBuffer.toMutableList()
            blockBuffer.clear()
            commitBlocks(blocksToCommit, isTip)
        }
    }

    private var lastLoggedCommit = Instant.EPOCH
    private fun commitBlocks(blocksToCommit: List<Block>, isTip: Boolean) {
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
            transaction {
//            warnLongQueriesDuration = 200L
                blocksToCommit.forEach { block ->
//                    if (block.header.blockHeight <= 60) {
//                        log.warn("block: $block")
//                    }
                    // Mark same block number as rolled back
                    rollbackTime += measureTimeMillis {
                        LedgerRepository.doRollback(block.header.blockHeight)
                    }

                    chainTime += measureTimeMillis {
                        ChainRepository.insert(block.toChainBlock())
                    }

                    // Load any Native asset metadata
                    nativeAssetTime += measureTimeMillis {
                        // Store just name/image/description
                        val nativeAssetMetadataSet =
                            LedgerRepository.upcertNativeAssets(extractBlockTokenMetadataSet(block))

                        // Store ALL nested metadata items
                        LedgerRepository.insertLedgerAssetMetadataList(block.toAssetMetadataList(nativeAssetMetadataSet))
                    }

                    // Insert unspent utxos and stake delegations/de-registrations
                    createTime += measureTimeMillis {
                        val slotNumber = block.header.slot
                        val blockNumber = block.header.blockHeight
                        val createdUtxos = block.toCreatedUtxoSet()
                        LedgerRepository.createUtxos(slotNumber, blockNumber, createdUtxos)
                        val stakeRegistrations = block.toStakeRegistrationList()
                        LedgerRepository.createStakeRegistrations(stakeRegistrations)
                        val epoch = getEpochForSlot(block.header.slot)
                        val stakeDelegations = block.toStakeDelegationList(epoch)
                        LedgerRepository.createStakeDelegations(stakeDelegations)
                        if (syncRawTxns) {
                            val rawTransactions = block.toRawTransactionList()
                            LedgerRepository.createRawTransactions(rawTransactions)
                        }
                    }

                    // Mark spent utxos as spent
                    spendTime += measureTimeMillis {
                        LedgerRepository.spendUtxos(
                            slotNumber = block.header.slot,
                            blockNumber = block.header.blockHeight,
                            spentUtxos = block.toSpentUtxoSet()
                        )
                    }
                }

                // Prune any old spent utxos we don't need any longer
                pruneTime = measureTimeMillis {
                    LedgerRepository.pruneSpent(blocksToCommit.last().header.slot)
                }
            }
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

    companion object {
        private const val RETRY_DELAY_MILLIS = 10_000L
        private const val BLOCK_BUFFER_SIZE = 100
        private const val FILLED_BLOCK_BUFFER_CAPACITY = 500
        private const val COMMIT_BLOCKS_WARN_LEVEL_MILLIS = 1_000L
        private val NATIVE_ASSET_METADATA_TAG = 721.toBigInteger()
        private val ASSET_NAME_METADATA_TAG = MetadataString("name")
        private val ASSET_IMAGE_METADATA_TAG = MetadataString("image")
        private val ASSET_DESCRIPTION_METADATA_TAG = MetadataString("description")
    }
}
