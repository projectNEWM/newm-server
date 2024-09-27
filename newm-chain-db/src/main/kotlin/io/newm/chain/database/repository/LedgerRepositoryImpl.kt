package io.newm.chain.database.repository

import com.github.benmanes.caffeine.cache.Caffeine
import com.google.iot.cbor.CborArray
import com.google.iot.cbor.CborByteString
import com.google.iot.cbor.CborInteger
import com.google.iot.cbor.CborMap
import com.google.iot.cbor.CborReader
import io.newm.chain.config.Config
import io.newm.chain.database.entity.LedgerAsset
import io.newm.chain.database.entity.LedgerAssetMetadata
import io.newm.chain.database.entity.LedgerUtxoHistory
import io.newm.chain.database.entity.RawTransaction
import io.newm.chain.database.entity.StakeDelegation
import io.newm.chain.database.entity.StakeRegistration
import io.newm.chain.database.repository.LedgerRepository.Companion.STABILITY_WINDOW
import io.newm.chain.database.table.AddressTxLogTable
import io.newm.chain.database.table.ChainTable
import io.newm.chain.database.table.LedgerAssetMetadataTable
import io.newm.chain.database.table.LedgerAssetsTable
import io.newm.chain.database.table.LedgerTable
import io.newm.chain.database.table.LedgerUtxoAssetsTable
import io.newm.chain.database.table.LedgerUtxosHistoryTable
import io.newm.chain.database.table.LedgerUtxosTable
import io.newm.chain.database.table.NativeAssetMonitorLogTable
import io.newm.chain.database.table.RawTransactionsTable
import io.newm.chain.database.table.StakeDelegationsTable
import io.newm.chain.database.table.StakeRegistrationsTable
import io.newm.chain.model.CreatedUtxo
import io.newm.chain.model.NativeAsset
import io.newm.chain.model.SpentUtxo
import io.newm.chain.model.Utxo
import io.newm.chain.util.Bech32
import io.newm.chain.util.Blake2b
import io.newm.chain.util.Constants.TX_DEST_UTXOS_INDEX
import io.newm.chain.util.Constants.TX_SPENT_UTXOS_INDEX
import io.newm.chain.util.Constants.UTXO_ADDRESS_INDEX
import io.newm.chain.util.Constants.UTXO_AMOUNT_INDEX
import io.newm.chain.util.Constants.UTXO_DATUM_INDEX
import io.newm.chain.util.Constants.UTXO_SCRIPT_REF_INDEX
import io.newm.chain.util.elementToByteArray
import io.newm.chain.util.elementToInt
import io.newm.chain.util.extractCredentials
import io.newm.chain.util.hexToByteArray
import io.newm.chain.util.toHexString
import java.math.BigInteger
import java.time.Duration
import kotlin.math.max
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jetbrains.exposed.sql.LongColumnType
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.SqlExpressionBuilder.isNull
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.castTo
import org.jetbrains.exposed.sql.count
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.innerJoin
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.max
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.sum
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.slf4j.LoggerFactory

class LedgerRepositoryImpl : LedgerRepository {
    private val log by lazy { LoggerFactory.getLogger("LedgerRepository") }
    private val utxoMutex = Mutex()

    /**
     * Set of the Utxos that are "spent", but not yet in a block. These should be removed once observed to be
     * used up in a block.
     */
    private val spentUtxoSet = mutableSetOf<SpentUtxo>()

    /**
     * Map the address to a list of utxos that have been created, but not yet made it into a block. These should be
     * removed once they are observed to be created in a block.
     */
    private val liveUtxoMap = mutableMapOf<String, Set<Utxo>>()

    override fun queryUtxos(address: String): Set<Utxo> =
        transaction {
            warnLongQueriesDuration = 1000L

            LedgerUtxosTable
                .innerJoin(LedgerTable, { ledgerId }, { LedgerTable.id }, { LedgerTable.address eq address })
                .select(
                    LedgerTable.address,
                    LedgerUtxosTable.id,
                    LedgerUtxosTable.txId,
                    LedgerUtxosTable.txIx,
                    LedgerUtxosTable.lovelace,
                    LedgerUtxosTable.datumHash,
                    LedgerUtxosTable.datum,
                    LedgerUtxosTable.scriptRef,
                    LedgerUtxosTable.scriptRefVersion,
                    LedgerUtxosTable.blockSpent,
                ).mapNotNull { row ->
                    if (row[LedgerUtxosTable.blockSpent] == null) {
                        val ledgerUtxoId = row[LedgerUtxosTable.id].value

                        val nativeAssets =
                            LedgerUtxoAssetsTable
                                .innerJoin(
                                    LedgerAssetsTable,
                                    { ledgerAssetId },
                                    { LedgerAssetsTable.id },
                                    { LedgerUtxoAssetsTable.ledgerUtxoId eq ledgerUtxoId }
                                ).select(
                                    LedgerAssetsTable.name,
                                    LedgerAssetsTable.policy,
                                    LedgerUtxoAssetsTable.amount
                                ).map { naRow ->
                                    NativeAsset(
                                        name = naRow[LedgerAssetsTable.name],
                                        policy = naRow[LedgerAssetsTable.policy],
                                        amount = BigInteger(naRow[LedgerUtxoAssetsTable.amount])
                                    )
                                }

                        Utxo(
                            address = row[LedgerTable.address],
                            hash = row[LedgerUtxosTable.txId],
                            ix = row[LedgerUtxosTable.txIx].toLong(),
                            lovelace = BigInteger(row[LedgerUtxosTable.lovelace]),
                            nativeAssets = nativeAssets,
                            datumHash = row[LedgerUtxosTable.datumHash],
                            datum = row[LedgerUtxosTable.datum],
                            scriptRef = row[LedgerUtxosTable.scriptRef],
                            scriptRefVersion = row[LedgerUtxosTable.scriptRefVersion],
                        )
                    } else {
                        null
                    }
                }.toHashSet()
        }

    override fun queryUtxosByStakeAddress(address: String): Set<Utxo> =
        transaction {
            warnLongQueriesDuration = 1000L

            LedgerUtxosTable
                .innerJoin(
                    LedgerTable,
                    { ledgerId },
                    { LedgerTable.id },
                    { LedgerTable.stakeAddress eq address }
                ).select(
                    LedgerTable.address,
                    LedgerUtxosTable.id,
                    LedgerUtxosTable.txId,
                    LedgerUtxosTable.txIx,
                    LedgerUtxosTable.lovelace,
                    LedgerUtxosTable.datumHash,
                    LedgerUtxosTable.datum,
                    LedgerUtxosTable.scriptRef,
                    LedgerUtxosTable.scriptRefVersion,
                    LedgerUtxosTable.blockSpent,
                ).mapNotNull { row ->
                    if (row[LedgerUtxosTable.blockSpent] == null) {
                        val ledgerUtxoId = row[LedgerUtxosTable.id].value

                        val nativeAssets =
                            LedgerUtxoAssetsTable
                                .innerJoin(
                                    LedgerAssetsTable,
                                    { ledgerAssetId },
                                    { LedgerAssetsTable.id },
                                    { LedgerUtxoAssetsTable.ledgerUtxoId eq ledgerUtxoId }
                                ).select(
                                    LedgerAssetsTable.name,
                                    LedgerAssetsTable.policy,
                                    LedgerUtxoAssetsTable.amount
                                ).map { naRow ->
                                    NativeAsset(
                                        name = naRow[LedgerAssetsTable.name],
                                        policy = naRow[LedgerAssetsTable.policy],
                                        amount = BigInteger(naRow[LedgerUtxoAssetsTable.amount])
                                    )
                                }

                        Utxo(
                            address = row[LedgerTable.address],
                            hash = row[LedgerUtxosTable.txId],
                            ix = row[LedgerUtxosTable.txIx].toLong(),
                            lovelace = BigInteger(row[LedgerUtxosTable.lovelace]),
                            nativeAssets = nativeAssets,
                            datumHash = row[LedgerUtxosTable.datumHash],
                            datum = row[LedgerUtxosTable.datum],
                            scriptRef = row[LedgerUtxosTable.scriptRef],
                            scriptRefVersion = row[LedgerUtxosTable.scriptRefVersion],
                        )
                    } else {
                        null
                    }
                }.toHashSet()
        }

    override fun queryUtxoByNativeAsset(
        name: String,
        policy: String
    ): Utxo? =
        transaction {
            LedgerTable
                .innerJoin(
                    LedgerUtxosTable,
                    { LedgerTable.id },
                    { ledgerId },
                ).innerJoin(
                    LedgerUtxoAssetsTable,
                    { LedgerUtxosTable.id },
                    { ledgerUtxoId },
                ).innerJoin(
                    LedgerAssetsTable,
                    { LedgerUtxoAssetsTable.ledgerAssetId },
                    { LedgerAssetsTable.id },
                ).selectAll()
                .where {
                    LedgerUtxosTable.blockSpent.isNull() and
                        (LedgerAssetsTable.name eq name) and (LedgerAssetsTable.policy eq policy)
                }.orderBy(LedgerUtxosTable.blockCreated, SortOrder.DESC)
                .firstOrNull()
                ?.let { row ->
                    val ledgerUtxoId = row[LedgerUtxosTable.id].value
                    val nativeAssets =
                        LedgerUtxoAssetsTable
                            .innerJoin(
                                LedgerAssetsTable,
                                { ledgerAssetId },
                                { LedgerAssetsTable.id },
                                { LedgerUtxoAssetsTable.ledgerUtxoId eq ledgerUtxoId }
                            ).selectAll()
                            .map { naRow ->
                                NativeAsset(
                                    name = naRow[LedgerAssetsTable.name],
                                    policy = naRow[LedgerAssetsTable.policy],
                                    amount = BigInteger(naRow[LedgerUtxoAssetsTable.amount])
                                )
                            }
                    Utxo(
                        address = row[LedgerTable.address],
                        hash = row[LedgerUtxosTable.txId],
                        ix = row[LedgerUtxosTable.txIx].toLong(),
                        lovelace = BigInteger(row[LedgerUtxosTable.lovelace]),
                        nativeAssets = nativeAssets,
                        datumHash = row[LedgerUtxosTable.datumHash],
                        datum = row[LedgerUtxosTable.datum],
                        scriptRef = row[LedgerUtxosTable.scriptRef],
                        scriptRefVersion = row[LedgerUtxosTable.scriptRefVersion],
                    )
                }
        }

    override fun queryPublicKeyHashByOutputRef(
        hash: String,
        ix: Int
    ): String? =
        transaction {
            LedgerUtxosTable
                .innerJoin(LedgerTable, { ledgerId }, { LedgerTable.id })
                .selectAll()
                .where {
                    (LedgerUtxosTable.txId eq hash) and
                        (LedgerUtxosTable.txIx eq ix) and
                        LedgerUtxosTable.blockSpent.isNull()
                }.map { row ->
                    val address = row[LedgerTable.address]
                    if (address.startsWith("addr")) {
                        Bech32
                            .decode(address)
                            .bytes
                            .drop(1)
                            .take(28)
                            .toByteArray()
                            .toHexString()
                    } else {
                        null
                    }
                }.firstOrNull()
        }

    override fun queryUtxosByOutputRef(
        hash: String,
        ix: Int
    ): Set<Utxo> =
        transaction {
            LedgerUtxosTable
                .innerJoin(LedgerTable, { ledgerId }, { LedgerTable.id })
                .selectAll()
                .where {
                    (LedgerUtxosTable.txId eq hash) and
                        (LedgerUtxosTable.txIx eq ix) and
                        LedgerUtxosTable.blockSpent.isNull()
                }.map { row ->
                    val ledgerUtxoId = row[LedgerUtxosTable.id].value

                    val nativeAssets =
                        LedgerUtxoAssetsTable
                            .innerJoin(
                                LedgerAssetsTable,
                                { ledgerAssetId },
                                { LedgerAssetsTable.id },
                                { LedgerUtxoAssetsTable.ledgerUtxoId eq ledgerUtxoId }
                            ).selectAll()
                            .map { naRow ->
                                NativeAsset(
                                    name = naRow[LedgerAssetsTable.name],
                                    policy = naRow[LedgerAssetsTable.policy],
                                    amount = BigInteger(naRow[LedgerUtxoAssetsTable.amount])
                                )
                            }

                    Utxo(
                        address = row[LedgerTable.address],
                        hash = row[LedgerUtxosTable.txId],
                        ix = row[LedgerUtxosTable.txIx].toLong(),
                        lovelace = BigInteger(row[LedgerUtxosTable.lovelace]),
                        nativeAssets = nativeAssets,
                        datumHash = row[LedgerUtxosTable.datumHash],
                        datum = row[LedgerUtxosTable.datum],
                        scriptRef = row[LedgerUtxosTable.scriptRef],
                        scriptRefVersion = row[LedgerUtxosTable.scriptRefVersion],
                    )
                }.toHashSet()
        }

    override suspend fun queryLiveUtxos(address: String): Set<Utxo> {
        val chainUtxos = queryUtxos(address)
        return chainUtxosToLiveUtxos(address, chainUtxos as MutableSet)
    }

    private suspend fun chainUtxosToLiveUtxos(
        address: String,
        chainUtxos: MutableSet<Utxo>
    ): Set<Utxo> {
        utxoMutex.withLock {
            return chainUtxos
                .apply {
                    // add in any liveUtxos from pending transactions
                    liveUtxoMap[address]?.let {
                        addAll(it)
                    }
                }.filterNot { chainUtxo ->
                    // remove any chain utxos that are already "spent" in a pending transaction
                    spentUtxoSet.contains(SpentUtxo("dummy", chainUtxo.hash, chainUtxo.ix))
                }.toSet()
        }
    }

    override suspend fun updateLiveLedgerState(
        transactionId: String,
        cborByteArray: ByteArray
    ) {
        utxoMutex.withLock {
            val tx = CborReader.createFromByteArray(cborByteArray).readDataItem() as CborArray
            val txBody = tx.elementAt(0) as CborMap
            val utxosInArray = txBody[TX_SPENT_UTXOS_INDEX] as CborArray
            utxosInArray.forEach { utxo ->
                var hash = ""
                var ix = 0L
                (utxo as CborArray).forEach { utxoElement ->
                    when (utxoElement) {
                        is CborByteString -> hash = utxoElement.byteArrayValue().toHexString()
                        else -> ix = (utxoElement as CborInteger).longValue()
                    }
                }
                // Mark this utxo as spent even though it's not in a block yet.
                processSpentUtxoFromSubmitTx(
                    SpentUtxo(hash = hash, ix = ix, transactionSpent = transactionId).also {
                        if (log.isDebugEnabled) {
                            log.debug("SpentUtxo: $it")
                        }
                    }
                )
            }
            val addressOutArray = txBody[TX_DEST_UTXOS_INDEX] as CborArray
            addressOutArray.forEachIndexed { ix, txOutput ->
                val address: String
                var lovelace = BigInteger.ZERO
                val nativeAssets = mutableListOf<NativeAsset>()
                var datumHash: String? = null
                var datum: String? = null
                var scriptRef: String? = null
                when (txOutput) {
                    is CborArray -> {
                        // alonzo and earlier
                        address =
                            Bech32.encode(
                                if (Config.isMainnet) {
                                    "addr"
                                } else {
                                    "addr_test"
                                },
                                txOutput.elementToByteArray(0)
                            )
                        when (val assetsOutput = txOutput.elementAt(1)) {
                            is CborInteger -> lovelace = assetsOutput.bigIntegerValue()
                            is CborArray -> {
                                assetsOutput.forEach { subItem ->
                                    when (subItem) {
                                        is CborInteger -> lovelace = subItem.bigIntegerValue()
                                        is CborMap -> {
                                            subItem.keySet().forEach { policyId ->
                                                val policy =
                                                    (policyId as CborByteString).byteArrayValue().toHexString()
                                                val token = subItem[policyId] as CborMap
                                                token.keySet().forEach { tokenName ->
                                                    val name =
                                                        (tokenName as CborByteString).byteArrayValue().toHexString()
                                                    val amount = (token[tokenName] as CborInteger).bigIntegerValue()
                                                    nativeAssets.add(
                                                        NativeAsset(
                                                            name = name,
                                                            policy = policy,
                                                            amount = amount
                                                        )
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        if (txOutput.size() > 2) {
                            datumHash = txOutput.elementToByteArray(2).toHexString()
                        }
                    }

                    is CborMap -> {
                        // babbage
                        val addressBytes = (txOutput[UTXO_ADDRESS_INDEX] as CborByteString).byteArrayValue()[0]
                        address =
                            if (Config.isMainnet) {
                                Bech32.encode("addr", addressBytes)
                            } else {
                                Bech32.encode("addr_test", addressBytes)
                            }
                        when (val amountCborObject = txOutput[UTXO_AMOUNT_INDEX]) {
                            is CborInteger -> {
                                lovelace = amountCborObject.bigIntegerValue()
                            }

                            is CborArray -> {
                                amountCborObject.forEach { amountItemCborObject ->
                                    when (amountItemCborObject) {
                                        is CborInteger -> {
                                            lovelace = amountItemCborObject.bigIntegerValue()
                                        }

                                        is CborMap -> {
                                            amountItemCborObject.keySet().forEach { policyId ->
                                                val policy =
                                                    (policyId as CborByteString).byteArrayValue().toHexString()
                                                val token = amountItemCborObject[policyId] as CborMap
                                                token.keySet().forEach { tokenName ->
                                                    val name =
                                                        (tokenName as CborByteString).byteArrayValue().toHexString()
                                                    val amount = (token[tokenName] as CborInteger).bigIntegerValue()
                                                    nativeAssets.add(
                                                        NativeAsset(
                                                            name = name,
                                                            policy = policy,
                                                            amount = amount
                                                        )
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        (txOutput[UTXO_DATUM_INDEX] as? CborArray)?.let { datumHashArray ->
                            when (datumHashArray.elementToInt(0)) {
                                0 -> {
                                    // datum hash
                                    datumHash = datumHashArray.elementToByteArray(1).toHexString()
                                }

                                1 -> {
                                    // inline datum
                                    datum = datumHashArray.elementToByteArray(1).toHexString()
                                }

                                else -> throw IllegalStateException("datum is not a hash or inline datum!")
                            }
                        }
                        (txOutput[UTXO_SCRIPT_REF_INDEX] as? CborByteString)?.let { scriptRefCborObject ->
                            scriptRef = scriptRefCborObject.byteArrayValue().toHexString()
                        }
                    }

                    else -> throw IllegalStateException("txOutput is not Array or Map!")
                }
                processLiveUtxoFromSubmitTx(
                    Utxo(
                        address = address,
                        hash = transactionId,
                        ix = ix.toLong(),
                        lovelace = lovelace,
                        nativeAssets = nativeAssets,
                        datumHash = datumHash,
                        datum = datum,
                        scriptRef = scriptRef,
                        scriptRefVersion = null,
                    ).also {
                        if (log.isDebugEnabled) {
                            log.debug("LiveUtxo: address: $address, $it")
                        }
                    }
                )
            }
        }
    }

    private fun processSpentUtxoFromSubmitTx(spentUtxo: SpentUtxo) {
        if (log.isDebugEnabled) {
            log.debug("processSpentUtxoFromSubmitTx: adding: $spentUtxo")
        }
        spentUtxoSet.add(spentUtxo)

        // remove any spent utxos from the list of live utxos
        val newEntries: MutableList<Pair<String, Set<Utxo>>> = mutableListOf()
        liveUtxoMap.forEach { (address, utxoSet) ->
            val newUtxoList = utxoSet.filterNot { utxo -> utxo.hash == spentUtxo.hash && utxo.ix == spentUtxo.ix }
            if (newUtxoList.size < utxoSet.size) {
                newEntries.add(Pair(address, newUtxoList.toSet()))
            }
        }
        newEntries.forEach { entry -> liveUtxoMap[entry.first] = entry.second }
    }

    private suspend fun processSpentUtxoFromBlock(spentUtxos: Set<SpentUtxo>) {
        utxoMutex.withLock {
            if (spentUtxoSet.removeAll(spentUtxos)) {
                if (log.isDebugEnabled) {
                    log.debug("processSpentUtxoFromBlock: removing: $spentUtxos")
                }
            }
        }
    }

    // Wait until 10 blocks have passed to make sure the blocks are immutable before removing them from live utxo map
    private val blockQueue = LinkedHashMap<Long, Set<CreatedUtxo>>(11)

    private suspend fun processLiveUtxoFromBlock(
        blockNumber: Long,
        createdUtxos: Set<CreatedUtxo>
    ) {
        blockQueue[blockNumber] = createdUtxos
        if (blockQueue.size > 10) {
            val oldestBlockNumber = blockQueue.keys.minOf { it }
            blockQueue.remove(oldestBlockNumber)?.let { immutableUtxos ->
                // now that these utxos are locked on the chain, we can remove them from our "live" list.
                utxoMutex.withLock {
                    if (log.isDebugEnabled) {
                        log.debug("processLiveUtxoFromBlock: $oldestBlockNumber, liveUtxoMap.size: ${liveUtxoMap.size}")
                    }
                    immutableUtxos.forEach { immutableUtxo ->
                        if (liveUtxoMap.containsKey(immutableUtxo.address)) {
                            val utxoSet = liveUtxoMap[immutableUtxo.address]!!
                            val newUtxoSet =
                                utxoSet
                                    .filterNot { utxo -> utxo.hash == immutableUtxo.hash && utxo.ix == immutableUtxo.ix }
                                    .toSet()
                            if (newUtxoSet.isEmpty()) {
                                liveUtxoMap.remove(immutableUtxo.address)?.let {
                                    if (log.isDebugEnabled) {
                                        log.debug("processLiveUtxoFromBlock: address: ${immutableUtxo.address}, removing: $it")
                                    }
                                }
                            } else {
                                liveUtxoMap.put(immutableUtxo.address, newUtxoSet)?.let {
                                    if (log.isDebugEnabled) {
                                        log.debug(
                                            "processLiveUtxoFromBlock: address: ${immutableUtxo.address}, removing: $it, saving: $newUtxoSet"
                                        )
                                    }
                                }
                            }
                        }
                    }
                    if (log.isDebugEnabled) {
                        log.debug("processLiveUtxoFromBlock: done, liveUtxoMap.size: ${liveUtxoMap.size}")
                    }
                }
            }
        }
    }

    private fun processLiveUtxoFromSubmitTx(liveUtxo: Utxo) {
        val newUtxoSet = liveUtxoMap[liveUtxo.address]?.toMutableSet()?.apply { add(liveUtxo) } ?: setOf(liveUtxo)
        if (log.isDebugEnabled) {
            log.debug("processLiveUtxoFromSubmitTx: address: ${liveUtxo.address}, adding: $newUtxoSet")
        }
        liveUtxoMap[liveUtxo.address] = newUtxoSet
    }

    override fun doRollback(blockNumber: Long) {
        LedgerUtxosTable.deleteWhere { blockCreated greaterEq blockNumber }
        LedgerUtxosTable.update({ LedgerUtxosTable.blockSpent greaterEq blockNumber }) {
            it[blockSpent] = null
            it[transactionSpent] = null
        }
        StakeDelegationsTable.deleteWhere { StakeDelegationsTable.blockNumber greaterEq blockNumber }
        RawTransactionsTable.deleteWhere { RawTransactionsTable.blockNumber greaterEq blockNumber }
    }

    override fun queryLedgerAsset(
        policyId: String,
        hexName: String
    ): LedgerAsset? =
        LedgerAssetsTable
            .selectAll()
            .where {
                (LedgerAssetsTable.policy eq policyId) and
                    (LedgerAssetsTable.name eq hexName)
            }.firstOrNull()
            ?.let { row ->
                LedgerAsset(
                    id = row[LedgerAssetsTable.id].value,
                    policy = row[LedgerAssetsTable.policy],
                    name = row[LedgerAssetsTable.name],
                    supply = row[LedgerAssetsTable.supply].toBigInteger()
                )
            }

    override fun queryLedgerAssets(ledgerAssetList: List<LedgerAsset>): List<LedgerAsset> =
        LedgerAssetsTable
            .selectAll()
            .where {
                ledgerAssetList
                    .map { ledgerAsset ->
                        (LedgerAssetsTable.policy eq ledgerAsset.policy) and
                            (LedgerAssetsTable.name eq ledgerAsset.name)
                    }.reduce { acc, expression -> acc or expression }
            }.map { row ->
                val policy = row[LedgerAssetsTable.policy]
                val name = row[LedgerAssetsTable.name]
                LedgerAsset(
                    id = row[LedgerAssetsTable.id].value,
                    policy = policy,
                    name = name,
                    supply = row[LedgerAssetsTable.supply].toBigInteger(),
                    txId = ledgerAssetList.first { it.policy == policy && it.name == name }.txId,
                )
            }

    override fun upcertLedgerAssets(ledgerAssets: List<LedgerAsset>): List<LedgerAsset> {
        val mintedLedgerAssets = mutableMapOf<Long, LedgerAsset>()
        ledgerAssets.map { ledgerAsset ->
            LedgerAssetsTable
                .selectAll()
                .where {
                    (LedgerAssetsTable.policy eq ledgerAsset.policy) and
                        (LedgerAssetsTable.name eq ledgerAsset.name)
                }.firstOrNull()
                ?.let { existingRow ->
                    val id: Long = existingRow[LedgerAssetsTable.id].value
                    val newSupply = existingRow[LedgerAssetsTable.supply].toBigInteger() + ledgerAsset.supply
                    // Do update
                    LedgerAssetsTable.update({ LedgerAssetsTable.id eq id }) { row ->
                        row[policy] = ledgerAsset.policy
                        row[name] = ledgerAsset.name
                        row[supply] = newSupply.toString()
                    }
                    if (ledgerAsset.supply > BigInteger.ZERO) {
                        mintedLedgerAssets[id] = ledgerAsset.copy(id = id, supply = newSupply)
                    }
                } ?: run {
                // Do insert
                val id =
                    LedgerAssetsTable
                        .insertAndGetId { row ->
                            row[policy] = ledgerAsset.policy
                            row[name] = ledgerAsset.name
                            row[supply] = ledgerAsset.supply.toString()
                        }.value

                if (ledgerAsset.supply > BigInteger.ZERO) {
                    mintedLedgerAssets[id] = ledgerAsset.copy(id = id)
                }
            }
        }
        return mintedLedgerAssets.values.toList()
    }

    override fun insertLedgerAssetMetadataList(assetMetadataList: List<LedgerAssetMetadata>) {
        // clean old data
        val assetIds = assetMetadataList.map { it.assetId }.distinct()
        LedgerAssetMetadataTable.deleteWhere { assetId inList assetIds }

        assetMetadataList.forEach { insertLedgerAssetMetadata(it, null) }
    }

    private fun insertLedgerAssetMetadata(
        ledgerAssetMetadata: LedgerAssetMetadata,
        parentId: Long?
    ) {
        val id =
            LedgerAssetMetadataTable
                .insertAndGetId {
                    it[assetId] = ledgerAssetMetadata.assetId
                    it[keyType] = ledgerAssetMetadata.keyType
                    it[key] = ledgerAssetMetadata.key.replace("\u0000", "\\u0000")
                    it[valueType] = ledgerAssetMetadata.valueType
                    it[value] = ledgerAssetMetadata.value.replace("\u0000", "\\u0000")
                    it[nestLevel] = ledgerAssetMetadata.nestLevel
                    it[LedgerAssetMetadataTable.parentId] = parentId
                }.value

        ledgerAssetMetadata.children.forEach { insertLedgerAssetMetadata(it, id) }
    }

    override fun queryLedgerAssetMetadataList(
        assetId: Long,
        parentId: Long?
    ): List<LedgerAssetMetadata> {
        val parentIdExpression =
            parentId?.let { (LedgerAssetMetadataTable.parentId eq it) }
                ?: LedgerAssetMetadataTable.parentId.isNull()
        return LedgerAssetMetadataTable
            .selectAll()
            .where {
                (LedgerAssetMetadataTable.assetId eq assetId) and parentIdExpression
            }.orderBy(LedgerAssetMetadataTable.id)
            .map { row ->
                val id = row[LedgerAssetMetadataTable.id].value
                LedgerAssetMetadata(
                    id = id,
                    assetId = row[LedgerAssetMetadataTable.assetId],
                    keyType = row[LedgerAssetMetadataTable.keyType],
                    key = row[LedgerAssetMetadataTable.key],
                    valueType = row[LedgerAssetMetadataTable.valueType],
                    value = row[LedgerAssetMetadataTable.value],
                    nestLevel = row[LedgerAssetMetadataTable.nestLevel],
                    children = queryLedgerAssetMetadataList(assetId, id),
                )
            }
    }

    override fun queryLedgerAssetMetadataListByNativeAsset(
        name: String,
        policy: String
    ): List<LedgerAssetMetadata> {
        val referenceTokenName =
            if (CIP68_USER_TOKEN_REGEX.matches(name)) {
                CIP68_REFERENCE_TOKEN_PREFIX + name.substring(8)
            } else {
                name
            }
        return transaction {
            LedgerAssetsTable
                .selectAll()
                .where {
                    (LedgerAssetsTable.name eq referenceTokenName) and
                        (LedgerAssetsTable.policy eq policy)
                }.firstOrNull()
                ?.let { row ->
                    val assetId = row[LedgerAssetsTable.id].value
                    queryLedgerAssetMetadataList(assetId, null)
                }.orEmpty()
        }
    }

    override fun pruneSpent(blockNumber: Long) {
        LedgerUtxosTable.deleteWhere { blockSpent less (blockNumber - 10_000L) } // older than 10k blocks ago

        // TODO: this takes forever to complete
        // LedgerTable.deleteWhere { notExists(LedgerUtxosTable.select(LedgerUtxosTable.id).where { LedgerUtxosTable.ledgerId eq LedgerTable.id })  }
    }

    override fun spendUtxos(
        blockNumber: Long,
        spentUtxos: Set<SpentUtxo>
    ) {
        var count = 0
        spentUtxos.forEach { spentUtxo ->
            count +=
                LedgerUtxosTable.update({
                    (LedgerUtxosTable.txId eq spentUtxo.hash) and
                        (LedgerUtxosTable.txIx eq spentUtxo.ix.toInt())
                }) { row ->
                    row[blockSpent] = blockNumber
                    row[transactionSpent] = spentUtxo.transactionSpent
                }
        }
        runBlocking {
            processSpentUtxoFromBlock(spentUtxos)
        }
    }

    override fun createUtxos(
        blockNumber: Long,
        createdUtxos: Set<CreatedUtxo>
    ) {
        createdUtxos.forEach { createdUtxo ->
            val ledgerTableId =
                LedgerTable
                    .select(LedgerTable.id)
                    .where {
                        LedgerTable.address eq createdUtxo.address
                    }.limit(1)
                    .firstOrNull()
                    ?.let { row ->
                        row[LedgerTable.id].value
                    } ?: LedgerTable
                    .insertAndGetId { row ->
                        row[address] = createdUtxo.address
                        row[stakeAddress] = createdUtxo.stakeAddress
                        row[addressType] = createdUtxo.addressType
                    }.value

            val ledgerUtxoTableId =
                LedgerUtxosTable
                    .insertAndGetId { row ->
                        row[ledgerId] = ledgerTableId
                        row[txId] = createdUtxo.hash
                        row[txIx] = createdUtxo.ix.toInt()
                        row[datumHash] = createdUtxo.datumHash ?: createdUtxo.datum
                            ?.hexToByteArray()
                            ?.let { Blake2b.hash256(it).toHexString() }
                        row[datum] = createdUtxo.datum
                        row[scriptRef] = createdUtxo.scriptRef
                        row[scriptRefVersion] = createdUtxo.scriptRefVersion
                        row[lovelace] = createdUtxo.lovelace.toString()
                        row[blockCreated] = blockNumber
                        row[blockSpent] = null
                        row[transactionSpent] = null
                        row[cbor] = createdUtxo.cbor
                        row[paymentCred] = createdUtxo.paymentCred
                        row[stakeCred] = createdUtxo.stakeCred
                    }.value

            createdUtxo.nativeAssets.forEach { nativeAsset ->
                val ledgerAssetTableId =
                    LedgerAssetsTable
                        .selectAll()
                        .where {
                            (LedgerAssetsTable.policy eq nativeAsset.policy) and (LedgerAssetsTable.name eq nativeAsset.name)
                        }.limit(1)
                        .first()[LedgerAssetsTable.id]
                        .value

                LedgerUtxoAssetsTable.insert { row ->
                    row[ledgerUtxoId] = ledgerUtxoTableId
                    row[ledgerAssetId] = ledgerAssetTableId
                    row[amount] = nativeAsset.amount.toString()
                }
            }
        }
        runBlocking {
            processLiveUtxoFromBlock(blockNumber, createdUtxos)
        }
    }

    override fun createStakeRegistrations(stakeRegistrations: List<StakeRegistration>) {
        StakeRegistrationsTable.batchInsert(
            data = stakeRegistrations,
            ignore = true,
            shouldReturnGeneratedValues = false
        ) {
            this[StakeRegistrationsTable.stakeAddress] = it.stakeAddress
            this[StakeRegistrationsTable.slot] = it.slot
            this[StakeRegistrationsTable.txIndex] = it.txIndex
            this[StakeRegistrationsTable.certIndex] = it.certIndex
        }

        stakeRegistrations.forEach { stakeRegistration ->
            stakeRegistrationsCache.invalidate(
                Triple(
                    stakeRegistration.slot,
                    stakeRegistration.txIndex,
                    stakeRegistration.certIndex
                )
            )
        }
    }

    private val stakeRegistrationsCache =
        Caffeine
            .newBuilder()
            .expireAfterWrite(Duration.ofMinutes(10))
            .build<Triple<Long, Int, Int>, StakeRegistration?> { (slot, txIndex, certIndex) ->
                transaction {
                    StakeRegistrationsTable
                        .selectAll()
                        .where {
                            (StakeRegistrationsTable.slot eq slot) and
                                (StakeRegistrationsTable.txIndex eq txIndex) and
                                (StakeRegistrationsTable.certIndex eq certIndex)
                        }.firstOrNull()
                        ?.let { row ->
                            StakeRegistration(
                                id = row[StakeRegistrationsTable.id].value,
                                stakeAddress = row[StakeRegistrationsTable.stakeAddress],
                                slot = row[StakeRegistrationsTable.slot],
                                txIndex = row[StakeRegistrationsTable.txIndex],
                                certIndex = row[StakeRegistrationsTable.certIndex],
                            )
                        }
                }
            }

    override fun findPointerStakeRegistration(
        slot: Long,
        txIndex: Int,
        certIndex: Int
    ): StakeRegistration? = stakeRegistrationsCache[Triple(slot, txIndex, certIndex)]

    override fun createStakeDelegations(stakeDelegations: List<StakeDelegation>) {
        StakeDelegationsTable.batchInsert(data = stakeDelegations, shouldReturnGeneratedValues = false) {
            this[StakeDelegationsTable.blockNumber] = it.blockNumber
            this[StakeDelegationsTable.stakeAddress] = it.stakeAddress
            this[StakeDelegationsTable.epoch] = it.epoch
            this[StakeDelegationsTable.poolId] = it.poolId
        }
    }

    override fun queryPoolLoyalty(
        stakeAddress: String,
        poolId: String,
        currentEpoch: Long
    ): Long =
        transaction {
            StakeDelegationsTable
                .selectAll()
                .where {
                    (StakeDelegationsTable.stakeAddress eq stakeAddress) and
                        (StakeDelegationsTable.epoch lessEq currentEpoch) and
                        (StakeDelegationsTable.poolId eq poolId)
                }.orderBy(
                    Pair(StakeDelegationsTable.blockNumber, SortOrder.DESC),
                    Pair(StakeDelegationsTable.id, SortOrder.DESC)
                ).limit(1)
                .firstOrNull()
                ?.let { row ->
                    currentEpoch - (row[StakeDelegationsTable.epoch] + 2L)
                } ?: 0L
        }

    override fun queryAdaHandle(adaHandleName: String): String? =
        transaction {
            LedgerTable
                .innerJoin(
                    otherTable = LedgerUtxosTable,
                    onColumn = { LedgerTable.id },
                    otherColumn = { ledgerId }
                ).innerJoin(
                    otherTable = LedgerUtxoAssetsTable,
                    onColumn = { LedgerUtxosTable.id },
                    otherColumn = { ledgerUtxoId }
                ).innerJoin(
                    otherTable = LedgerAssetsTable,
                    onColumn = { LedgerUtxoAssetsTable.ledgerAssetId },
                    otherColumn = { LedgerAssetsTable.id }
                ).select(LedgerTable.address)
                .where {
                    (LedgerAssetsTable.policy eq ADA_HANDLES_POLICY) and
                        (LedgerAssetsTable.name eq adaHandleName.toByteArray().toHexString()) and
                        LedgerUtxosTable.blockSpent.isNull()
                }.firstOrNull()
                ?.let { row -> row[LedgerTable.address] }
        }

    private val idCount = LedgerUtxosTable.id.count()
    private val siblingHashCountCache =
        Caffeine
            .newBuilder()
            .expireAfterWrite(Duration.ofMinutes(1))
            .build<String, Long> { hash ->
                transaction {
                    LedgerUtxosTable.select(idCount).where { LedgerUtxosTable.txId eq hash }.first()[idCount]
                }
            }

    override fun siblingHashCount(hash: String): Long = siblingHashCountCache[hash]!!

    override fun queryPayerAddress(receivedUtxo: Utxo): String =
        transaction {
            LedgerTable
                .innerJoin(
                    otherTable = LedgerUtxosTable,
                    onColumn = { LedgerTable.id },
                    otherColumn = { ledgerId }
                ).select(LedgerTable.address)
                .where {
                    LedgerUtxosTable.transactionSpent eq receivedUtxo.hash
                }.limit(1)
                .firstOrNull()
                ?.let { row -> row[LedgerTable.address] }
                ?: throw IllegalArgumentException("Cannot find payer address that funded utxo: ${receivedUtxo.hash}:${receivedUtxo.ix}")
        }

    override fun queryAddressForUtxo(
        hash: String,
        ix: Int
    ): String? =
        transaction {
            LedgerTable
                .innerJoin(
                    otherTable = LedgerUtxosTable,
                    onColumn = { LedgerTable.id },
                    otherColumn = { ledgerId }
                ).select(LedgerTable.address)
                .where {
                    (LedgerUtxosTable.txId eq hash) and
                        (LedgerUtxosTable.txIx eq ix)
                }.limit(1)
                .firstOrNull()
                ?.let { row -> row[LedgerTable.address] }
        }

    override fun queryUtxoHavingAddress(
        address: String,
        hash: String,
        ix: Int
    ): Utxo? =
        transaction {
            LedgerUtxosTable
                .innerJoin(LedgerTable, { ledgerId }, { LedgerTable.id }, { LedgerTable.address eq address })
                .selectAll()
                .where {
                    (LedgerUtxosTable.txId eq hash) and
                        (LedgerUtxosTable.txIx eq ix)
                }.firstOrNull()
                ?.let { row ->
                    val ledgerUtxoId = row[LedgerUtxosTable.id].value

                    val nativeAssets =
                        LedgerUtxoAssetsTable
                            .innerJoin(
                                LedgerAssetsTable,
                                { ledgerAssetId },
                                { LedgerAssetsTable.id },
                                { LedgerUtxoAssetsTable.ledgerUtxoId eq ledgerUtxoId }
                            ).selectAll()
                            .map { naRow ->
                                NativeAsset(
                                    name = naRow[LedgerAssetsTable.name],
                                    policy = naRow[LedgerAssetsTable.policy],
                                    amount = BigInteger(naRow[LedgerUtxoAssetsTable.amount])
                                )
                            }

                    Utxo(
                        address = row[LedgerTable.address],
                        hash = row[LedgerUtxosTable.txId],
                        ix = row[LedgerUtxosTable.txIx].toLong(),
                        lovelace = BigInteger(row[LedgerUtxosTable.lovelace]),
                        nativeAssets = nativeAssets,
                        datumHash = row[LedgerUtxosTable.datumHash],
                        datum = row[LedgerUtxosTable.datum],
                        scriptRef = row[LedgerUtxosTable.scriptRef],
                        scriptRefVersion = row[LedgerUtxosTable.scriptRefVersion],
                    )
                }
        }

    override fun createRawTransactions(rawTransactions: List<RawTransaction>) {
        if (rawTransactions.isNotEmpty()) {
            RawTransactionsTable.batchInsert(data = rawTransactions, shouldReturnGeneratedValues = false) {
                this[RawTransactionsTable.blockNumber] = it.blockNumber
                this[RawTransactionsTable.txId] = it.txId
                this[RawTransactionsTable.tx] = it.tx
                this[RawTransactionsTable.slotNumber] = it.slotNumber
                this[RawTransactionsTable.blockSize] = it.blockSize
                this[RawTransactionsTable.blockBodyHash] = it.blockBodyHash
                this[RawTransactionsTable.protocolVersionMajor] = it.protocolVersionMajor
                this[RawTransactionsTable.protocolVersionMinor] = it.protocolVersionMinor
            }
        }
    }

    override fun createLedgerUtxoHistory(
        createdUtxos: Set<CreatedUtxo>,
        blockNumber: Long
    ) {
        val ledgerUtxoHistorySet =
            createdUtxos.mapNotNull { createdUtxo ->
                // Save off credential to txid mapping
                val credentials =
                    if (createdUtxo.address.startsWith("addr")) {
                        try {
                            createdUtxo.address.extractCredentials()
                        } catch (e: Throwable) {
                            log.warn("Unable to extractCredential from: ${createdUtxo.address}")
                            null
                        }
                    } else {
                        null
                    }
                credentials?.let { creds ->
                    LedgerUtxoHistory(creds.first, creds.second, createdUtxo.hash, blockNumber)
                }
            }

        LedgerUtxosHistoryTable.batchInsert(
            data = ledgerUtxoHistorySet,
            ignore = true,
            shouldReturnGeneratedValues = false
        ) {
            this[LedgerUtxosHistoryTable.paymentCred] = it.paymentCred
            this[LedgerUtxosHistoryTable.stakeCred] = it.stakeCred
            this[LedgerUtxosHistoryTable.txId] = it.txId
            this[LedgerUtxosHistoryTable.block] = it.block
        }
    }

    override fun queryAddressTxLogsAfter(
        address: String,
        afterTxId: String?,
        limit: Int,
        offset: Long
    ): List<ByteArray> =
        transaction {
            val afterId: Long =
                afterTxId?.let {
                    AddressTxLogTable
                        .select(AddressTxLogTable.id)
                        .where {
                            (AddressTxLogTable.address eq address) and (AddressTxLogTable.txId eq it)
                        }.firstOrNull()
                        ?.let { row -> row[AddressTxLogTable.id].value } ?: -1L
                } ?: -1L

            if (afterId == -1L && afterTxId != null) {
                log.warn("Unable to find txId: $afterTxId for address: $address. afterId: $afterId, limit: $limit, offset: $offset")
            }

            val maxBlockNumberExpression = ChainTable.blockNumber.max()
            val maxBlockNumber =
                (
                    ChainTable.select(maxBlockNumberExpression).firstOrNull()?.let {
                        it[maxBlockNumberExpression]
                    } ?: 0L
                ) - STABILITY_WINDOW

            AddressTxLogTable
                .selectAll()
                .where {
                    (AddressTxLogTable.address eq address) and
                        (AddressTxLogTable.id greater afterId) and
                        (AddressTxLogTable.blockNumber lessEq maxBlockNumber)
                }.orderBy(AddressTxLogTable.id)
                .limit(limit, offset)
                .map { row ->
                    row[AddressTxLogTable.monitorAddressResponseBytes]
                }
        }

    override fun queryNativeAssetLogsAfter(
        afterTableId: Long?,
        limit: Int,
        offset: Long
    ): List<Pair<Long, ByteArray>> =
        transaction {
            val afterId: Long =
                afterTableId?.let {
                    NativeAssetMonitorLogTable
                        .select(NativeAssetMonitorLogTable.id)
                        .where {
                            NativeAssetMonitorLogTable.id eq it
                        }.firstOrNull()
                        ?.let { row -> row[NativeAssetMonitorLogTable.id].value } ?: -1L
                } ?: -1L

            val maxBlockNumberExpression = ChainTable.blockNumber.max()
            val maxBlockNumber =
                (
                    ChainTable.select(maxBlockNumberExpression).firstOrNull()?.let {
                        it[maxBlockNumberExpression]
                    } ?: 0L
                ) - STABILITY_WINDOW

            NativeAssetMonitorLogTable
                .selectAll()
                .where {
                    (NativeAssetMonitorLogTable.id greater afterId) and
                        (NativeAssetMonitorLogTable.blockNumber lessEq maxBlockNumber)
                }.orderBy(NativeAssetMonitorLogTable.id)
                .limit(limit, offset)
                .map { row ->
                    row[NativeAssetMonitorLogTable.id].value to row[NativeAssetMonitorLogTable.monitorNativeAssetsResponseBytes]
                }
        }

    override fun queryTransactionConfirmationCounts(txIds: List<String>): Map<String, Long> =
        transaction {
            val tipBlockExpression = LedgerUtxosTable.blockCreated.max()
            val tipBlock: Long =
                LedgerUtxosTable.select(tipBlockExpression).firstOrNull()?.let {
                    it[tipBlockExpression]
                } ?: 0L

            val ledgerMap =
                LedgerUtxosTable
                    .select(LedgerUtxosTable.txId, LedgerUtxosTable.blockCreated)
                    .where { LedgerUtxosTable.txId inList txIds }
                    .withDistinct()
                    .associate { row ->
                        val txId = row[LedgerUtxosTable.txId]
                        val blockCreated = row[LedgerUtxosTable.blockCreated]
                        txId to max(tipBlock - blockCreated, 0L)
                    }

            txIds.associateWith { txId -> (ledgerMap[txId] ?: 0L) }
        }

    override fun queryDatumByHash(datumHashHex: String): String? =
        transaction {
            LedgerUtxosTable
                .selectAll()
                .where { LedgerUtxosTable.datumHash eq datumHashHex }
                .firstOrNull()
                ?.let { row ->
                    row[LedgerUtxosTable.datum]
                }
        }

    override fun snapshotNativeAssets(
        policy: String,
        name: String
    ): Map<String, Long> =
        transaction {
            var totalSupply = 0L
            val stakeAddressToAssetCountMap = mutableMapOf<String, Long>()
            val countExpression = LedgerTable.stakeAddress.count()
            val sumExpression = LedgerUtxoAssetsTable.amount.castTo<Long>(LongColumnType()).sum()
            val isEmpty = name.isBlank()
            val isRegex = name.startsWith('~')
            val isLike = name.contains('%')
            val isNFT = isEmpty || isRegex || isLike
            if (isNFT) {
                // Snapshot NFTs
                LedgerTable
                    .innerJoin(
                        otherTable = LedgerUtxosTable,
                        onColumn = { LedgerTable.id },
                        otherColumn = { ledgerId }
                    ).innerJoin(
                        otherTable = LedgerUtxoAssetsTable,
                        onColumn = { LedgerUtxosTable.id },
                        otherColumn = { ledgerUtxoId }
                    ).innerJoin(
                        otherTable = LedgerAssetsTable,
                        onColumn = { LedgerUtxoAssetsTable.ledgerAssetId },
                        otherColumn = { LedgerAssetsTable.id }
                    ).select(LedgerTable.stakeAddress, countExpression)
                    .where {
                        if (isEmpty) {
                            (LedgerAssetsTable.policy eq policy) and
                                (LedgerAssetsTable.name eq "") and
                                LedgerTable.stakeAddress.isNotNull() and
                                LedgerUtxosTable.blockSpent.isNull()
                        } else if (isRegex) {
                            (LedgerAssetsTable.policy eq policy) and
                                LedgerAssetsTable.name.regexp(name.substring(1)) and
                                LedgerTable.stakeAddress.isNotNull() and
                                LedgerUtxosTable.blockSpent.isNull()
                        } else {
                            (LedgerAssetsTable.policy eq policy) and
                                (LedgerAssetsTable.name like name) and
                                LedgerTable.stakeAddress.isNotNull() and
                                LedgerUtxosTable.blockSpent.isNull()
                        }
                    }.groupBy(LedgerTable.stakeAddress)
                    .forEach { row ->
                        row[LedgerTable.stakeAddress]?.let { stakeAddress ->
                            val currentCount = stakeAddressToAssetCountMap[stakeAddress] ?: 0L
                            stakeAddressToAssetCountMap[stakeAddress] = currentCount + row[countExpression]
                        }
                        totalSupply += row[countExpression]
                    }
            } else {
                // Snapshot FTs
                LedgerTable
                    .innerJoin(
                        otherTable = LedgerUtxosTable,
                        onColumn = { LedgerTable.id },
                        otherColumn = { ledgerId }
                    ).innerJoin(
                        otherTable = LedgerUtxoAssetsTable,
                        onColumn = { LedgerUtxosTable.id },
                        otherColumn = { ledgerUtxoId }
                    ).innerJoin(
                        otherTable = LedgerAssetsTable,
                        onColumn = { LedgerUtxoAssetsTable.ledgerAssetId },
                        otherColumn = { LedgerAssetsTable.id }
                    ).select(LedgerTable.stakeAddress, sumExpression)
                    .where {
                        (LedgerAssetsTable.policy eq policy) and
                            (LedgerAssetsTable.name eq name) and
                            LedgerTable.stakeAddress.isNotNull() and
                            LedgerUtxosTable.blockSpent.isNull()
                    }.groupBy(LedgerTable.stakeAddress)
                    .forEach { row ->
                        row[LedgerTable.stakeAddress]?.let { stakeAddress ->
                            val currentCount = stakeAddressToAssetCountMap[stakeAddress] ?: 0L
                            stakeAddressToAssetCountMap[stakeAddress] = currentCount + row[sumExpression]!!
                        }
                        totalSupply += row[sumExpression]!!
                    }
            }
            if (totalSupply > 0L) {
                // insert total_supply record for this asset_tracking record
                stakeAddressToAssetCountMap["total_supply"] = totalSupply
            }

            stakeAddressToAssetCountMap
        }

    override fun queryUsedAddresses(addresses: List<String>): Set<String> =
        transaction {
            LedgerTable
                .select(LedgerTable.address)
                .where { LedgerTable.address inList addresses }
                .mapNotNull { row -> row[LedgerTable.address] }
                .toHashSet()
        }

    companion object {
        private const val ADA_HANDLES_POLICY = "f0ff48bbb7bbe9d59a40f1ce90e9e9d0ff5002ec48f232b49ca0fb9a"
        private const val CIP68_REFERENCE_TOKEN_PREFIX = "000643b0"
        private val CIP68_USER_TOKEN_REGEX =
            Regex("^00(0de14|14df1|1bc28)0.*$") // (222)TokenName, (333)TokenName, (444)TokenName
    }
}
