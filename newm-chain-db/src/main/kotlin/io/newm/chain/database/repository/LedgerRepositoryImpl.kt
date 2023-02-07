package io.newm.chain.database.repository

import com.github.benmanes.caffeine.cache.Caffeine
import io.newm.chain.database.entity.LedgerAssetMetadata
import io.newm.chain.database.entity.RawTransaction
import io.newm.chain.database.entity.StakeDelegation
import io.newm.chain.database.entity.StakeRegistration
import io.newm.chain.database.table.LedgerAssetMetadataTable
import io.newm.chain.database.table.LedgerAssetsTable
import io.newm.chain.database.table.LedgerTable
import io.newm.chain.database.table.LedgerUtxoAssetsTable
import io.newm.chain.database.table.LedgerUtxosTable
import io.newm.chain.database.table.RawTransactionsTable
import io.newm.chain.database.table.StakeDelegationsTable
import io.newm.chain.database.table.StakeRegistrationsTable
import io.newm.chain.model.CreatedUtxo
import io.newm.chain.model.NativeAsset
import io.newm.chain.model.NativeAssetMetadata
import io.newm.chain.model.SpentUtxo
import io.newm.chain.model.Utxo
import io.newm.chain.util.toHexString
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.count
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.innerJoin
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.slf4j.LoggerFactory
import java.math.BigInteger
import java.time.Duration

class LedgerRepositoryImpl : LedgerRepository {
    private val log by lazy { LoggerFactory.getLogger("LedgerRepository") }

    override fun queryUtxos(address: String): List<Utxo> = transaction {
        LedgerUtxosTable.innerJoin(LedgerTable, { ledgerId }, { LedgerTable.id }, { LedgerTable.address eq address })
            .select {
                LedgerUtxosTable.blockSpent.isNull() and LedgerUtxosTable.slotSpent.isNull()
            }.map { row ->
                val ledgerUtxoId = row[LedgerUtxosTable.id].value

                val nativeAssets = LedgerUtxoAssetsTable.innerJoin(
                    LedgerAssetsTable,
                    { ledgerAssetId },
                    { LedgerAssetsTable.id },
                    { LedgerUtxoAssetsTable.ledgerUtxoId eq ledgerUtxoId }
                )
                    .selectAll().map { naRow ->
                        NativeAsset(
                            name = naRow[LedgerAssetsTable.name],
                            policy = naRow[LedgerAssetsTable.policy],
                            amount = BigInteger(naRow[LedgerUtxoAssetsTable.amount])
                        )
                    }

                Utxo(
                    hash = row[LedgerUtxosTable.txId],
                    ix = row[LedgerUtxosTable.txIx].toLong(),
                    lovelace = BigInteger(row[LedgerUtxosTable.lovelace]),
                    datumHash = row[LedgerUtxosTable.datumHash],
                    datum = row[LedgerUtxosTable.datum],
                    nativeAssets = nativeAssets
                )
            }
    }

    override fun doRollback(blockNumber: Long) {
        LedgerUtxosTable.deleteWhere { blockCreated greaterEq blockNumber }
        LedgerUtxosTable.update({ LedgerUtxosTable.blockSpent greaterEq blockNumber }) {
            it[blockSpent] = null
            it[slotSpent] = null
            it[transactionSpent] = null
        }
        StakeDelegationsTable.deleteWhere { StakeDelegationsTable.blockNumber greaterEq blockNumber }
        RawTransactionsTable.deleteWhere { RawTransactionsTable.blockNumber greaterEq blockNumber }
    }

    override fun upcertNativeAssets(nativeAssetsMetadata: Set<NativeAssetMetadata>): Set<NativeAssetMetadata> {
        return nativeAssetsMetadata.map { nativeAssetMetadata ->
            LedgerAssetsTable.select {
                (LedgerAssetsTable.policy eq nativeAssetMetadata.assetPolicy) and
                    (LedgerAssetsTable.name eq nativeAssetMetadata.assetName)
            }.firstOrNull()?.let { existingRow ->
                val id: Long = existingRow[LedgerAssetsTable.id].value
                // Do update
                LedgerAssetsTable.update({ LedgerAssetsTable.id eq id }) { row ->
                    row[policy] = nativeAssetMetadata.assetPolicy
                    row[name] = nativeAssetMetadata.assetName
                    row[image] = nativeAssetMetadata.metadataImage
                    row[description] = nativeAssetMetadata.metadataDescription
                }

                nativeAssetMetadata.copy(id = id)
            } ?: run {
                // Do insert
                val id = LedgerAssetsTable.insertAndGetId { row ->
                    row[policy] = nativeAssetMetadata.assetPolicy
                    row[name] = nativeAssetMetadata.assetName
                    row[image] = nativeAssetMetadata.metadataImage
                    row[description] = nativeAssetMetadata.metadataDescription
                }.value

                nativeAssetMetadata.copy(id = id)
            }
        }.toSet()
    }

    override fun insertLedgerAssetMetadataList(assetMetadataList: List<LedgerAssetMetadata>) {
        // clean old data
        val assetIds = assetMetadataList.map { it.assetId }.distinct()
        LedgerAssetMetadataTable.deleteWhere { assetId inList assetIds }

        assetMetadataList.forEach { insertLedgerAssetMetadata(it, null) }
    }

    private fun insertLedgerAssetMetadata(ledgerAssetMetadata: LedgerAssetMetadata, parentId: Long?) {
        val id = LedgerAssetMetadataTable.insertAndGetId {
            it[assetId] = ledgerAssetMetadata.assetId
            it[keyType] = ledgerAssetMetadata.keyType
            it[key] = ledgerAssetMetadata.key
            it[valueType] = ledgerAssetMetadata.valueType
            it[value] = ledgerAssetMetadata.value
            it[nestLevel] = ledgerAssetMetadata.nestLevel
            it[LedgerAssetMetadataTable.parentId] = parentId
        }.value

        ledgerAssetMetadata.children.forEach { insertLedgerAssetMetadata(it, id) }
    }

    override fun pruneSpent(slotNumber: Long) {
        LedgerUtxosTable.deleteWhere { slotSpent less (slotNumber - 172800L) } // 48 hours

        // TODO: this takes forever to complete
        // LedgerTable.deleteWhere { notExists(LedgerUtxosTable.slice(LedgerUtxosTable.id).select { LedgerUtxosTable.ledgerId eq LedgerTable.id })  }
    }

    override fun spendUtxos(slotNumber: Long, blockNumber: Long, spentUtxos: Set<SpentUtxo>) {
        var count = 0
        spentUtxos.forEach { spentUtxo ->
            count += LedgerUtxosTable.update({
                (LedgerUtxosTable.txId eq spentUtxo.hash) and
                    (LedgerUtxosTable.txIx eq spentUtxo.ix.toInt())
            }) { row ->
                row[blockSpent] = blockNumber
                row[slotSpent] = slotNumber
                row[transactionSpent] = spentUtxo.transactionSpent
            }
        }
//        if (count > 0 && count == spentUtxos.size) {
//            log.warn("spentUtxo update match: $count")
//        }
//        if (count != spentUtxos.size) {
//            log.warn("spentUtxo update mismatch: spentUtxos.size=${spentUtxos.size}, count=${count}")
//            log.warn(spentUtxos.toString())
//        }
    }

    override fun createUtxos(slotNumber: Long, blockNumber: Long, createdUtxos: Set<CreatedUtxo>) {
        createdUtxos.forEach { createdUtxo ->
            val ledgerTableId =
                LedgerTable.slice(LedgerTable.id).select {
                    LedgerTable.address eq createdUtxo.address
                }.limit(1).firstOrNull()
                    ?.let { row ->
                        row[LedgerTable.id].value
                    } ?: LedgerTable.insertAndGetId { row ->
                    row[address] = createdUtxo.address
                    row[stakeAddress] = createdUtxo.stakeAddress
                    row[addressType] = createdUtxo.addressType
                }.value

            val ledgerUtxoTableId = LedgerUtxosTable.insertAndGetId { row ->
                row[ledgerId] = ledgerTableId
                row[txId] = createdUtxo.hash
                row[txIx] = createdUtxo.ix.toInt()
                row[datumHash] = createdUtxo.datumHash
                row[datum] = createdUtxo.datum
                row[lovelace] = createdUtxo.lovelace.toString()
                row[blockCreated] = blockNumber
                row[slotCreated] = slotNumber
                row[blockSpent] = null
                row[slotSpent] = null
                row[transactionSpent] = null
                row[cbor] = createdUtxo.cbor
            }.value

            createdUtxo.nativeAssets.forEach { nativeAsset ->
                val ledgerAssetTableId =
                    LedgerAssetsTable.select {
                        (LedgerAssetsTable.policy eq nativeAsset.policy) and (LedgerAssetsTable.name eq nativeAsset.name)
                    }.limit(1).firstOrNull()?.let { row ->
                        row[LedgerAssetsTable.id].value
                    } ?: LedgerAssetsTable.insertAndGetId { row ->
                        row[policy] = nativeAsset.policy
                        row[name] = nativeAsset.name
                        row[image] = null
                        row[description] = null
                    }.value

                LedgerUtxoAssetsTable.insert { row ->
                    row[ledgerUtxoId] = ledgerUtxoTableId
                    row[ledgerAssetId] = ledgerAssetTableId
                    row[amount] = nativeAsset.amount.toString()
                }
            }
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

    private val stakeRegistrationsCache = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofMinutes(10))
        .build<Triple<Long, Int, Int>, StakeRegistration?> { (slot, txIndex, certIndex) ->
            transaction {
                StakeRegistrationsTable.select {
                    (StakeRegistrationsTable.slot eq slot) and
                        (StakeRegistrationsTable.txIndex eq txIndex) and
                        (StakeRegistrationsTable.certIndex eq certIndex)
                }.firstOrNull()?.let { row ->
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

    override fun findPointerStakeRegistration(slot: Long, txIndex: Int, certIndex: Int): StakeRegistration? =
        stakeRegistrationsCache[Triple(slot, txIndex, certIndex)]

    override fun createStakeDelegations(stakeDelegations: List<StakeDelegation>) {
        StakeDelegationsTable.batchInsert(data = stakeDelegations, shouldReturnGeneratedValues = false) {
            this[StakeDelegationsTable.blockNumber] = it.blockNumber
            this[StakeDelegationsTable.stakeAddress] = it.stakeAddress
            this[StakeDelegationsTable.epoch] = it.epoch
            this[StakeDelegationsTable.poolId] = it.poolId
        }
    }

    override fun queryPoolLoyalty(stakeAddress: String, poolId: String, currentEpoch: Long): Long = transaction {
        StakeDelegationsTable.select {
            (StakeDelegationsTable.stakeAddress eq stakeAddress) and
                (StakeDelegationsTable.epoch lessEq currentEpoch) and
                (StakeDelegationsTable.poolId eq poolId)
        }.orderBy(
            Pair(StakeDelegationsTable.blockNumber, SortOrder.DESC),
            Pair(StakeDelegationsTable.id, SortOrder.DESC)
        ).limit(1).firstOrNull()?.let { row ->
            currentEpoch - (row[StakeDelegationsTable.epoch] + 2L)
        } ?: 0L
    }

    override fun queryAdaHandle(adaHandleName: String): String? = transaction {
        LedgerTable
            .innerJoin(
                otherTable = LedgerUtxosTable,
                onColumn = { LedgerTable.id },
                otherColumn = { ledgerId }
            )
            .innerJoin(
                otherTable = LedgerUtxoAssetsTable,
                onColumn = { LedgerUtxosTable.id },
                otherColumn = { ledgerUtxoId }
            )
            .innerJoin(
                otherTable = LedgerAssetsTable,
                onColumn = { LedgerUtxoAssetsTable.ledgerAssetId },
                otherColumn = { LedgerAssetsTable.id }
            )
            .slice(LedgerTable.address)
            .select {
                (LedgerAssetsTable.policy eq ADA_HANDLES_POLICY) and
                    (LedgerAssetsTable.name eq adaHandleName.toByteArray().toHexString()) and
                    LedgerUtxosTable.blockSpent.isNull()
            }
            .firstOrNull()?.let { row -> row[LedgerTable.address] }
    }

    private val idCount = LedgerUtxosTable.id.count()
    private val siblingHashCountCache = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofMinutes(1))
        .build<String, Long> { hash ->
            transaction {
                LedgerUtxosTable.slice(idCount).select { LedgerUtxosTable.txId eq hash }.first()[idCount]
            }
        }

    override fun siblingHashCount(hash: String): Long = siblingHashCountCache[hash]!!

    override fun queryPayerAddress(receivedUtxo: Utxo): String = transaction {
        LedgerTable.innerJoin(
            otherTable = LedgerUtxosTable,
            onColumn = { LedgerTable.id },
            otherColumn = { ledgerId }
        )
            .slice(LedgerTable.address)
            .select {
                LedgerUtxosTable.transactionSpent eq receivedUtxo.hash
            }.limit(1).firstOrNull()?.let { row -> row[LedgerTable.address] }
            ?: throw IllegalArgumentException("Cannot find payer address that funded utxo: ${receivedUtxo.hash}:${receivedUtxo.ix}")
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

    companion object {
        private const val ADA_HANDLES_POLICY = "f0ff48bbb7bbe9d59a40f1ce90e9e9d0ff5002ec48f232b49ca0fb9a"
    }
}
