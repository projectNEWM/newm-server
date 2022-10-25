package io.newm.chain.database.repository

import io.newm.chain.config.Config
import io.newm.chain.database.entity.ChainBlock
import io.newm.chain.database.table.ChainTable
import io.newm.chain.database.table.KeysTable
import io.newm.chain.database.table.PaymentStakeAddressTable
import io.newm.chain.database.table.TransactionDestAddressTable
import io.newm.chain.util.Blake2b
import io.newm.chain.util.hexToByteArray
import io.newm.chain.util.toHexString
import io.newm.kogmios.protocols.model.PointDetail
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object ChainRepository {
    private val log: Logger by lazy { LoggerFactory.getLogger("ChainRepository") }

    /**
     * How many blocks behind tip do we feel is safe?
     */
    private const val STABILITY_WINDOW = 3L

    fun getFindIntersectPairs(): List<PointDetail> {
        return transaction {
            ChainTable.slice(
                ChainTable.slotNumber,
                ChainTable.hash
            ).selectAll()
                .orderBy(ChainTable.slotNumber, SortOrder.DESC)
                .limit(33).filterIndexed { index, _ ->
                    // all powers of 2 including 0th element 0, 2, 4, 8, 16, 32
                    (index == 0) || ((index > 1) && (index and (index - 1) == 0))
                }.map { row ->
                    PointDetail(
                        slot = row[ChainTable.slotNumber],
                        hash = row[ChainTable.hash],
                    )
                }
        }
    }

    fun getPointAfterSlot(slot: Long): PointDetail? {
        return transaction {
            ChainTable.slice(
                ChainTable.slotNumber,
                ChainTable.hash
            ).select { ChainTable.slotNumber greater slot }
                .orderBy(ChainTable.slotNumber, SortOrder.ASC)
                .limit(1).firstOrNull()?.let { row ->
                    PointDetail(
                        slot = row[ChainTable.slotNumber],
                        hash = row[ChainTable.hash],
                    )
                }
        }
    }

    fun getVrfByPoolId(poolId: String): String? {
        return transaction {
            ChainTable.slice(ChainTable.nodeVrfVkey)
                .select { ChainTable.poolId eq poolId }
                .orderBy(ChainTable.id, SortOrder.DESC)
                .limit(1)
                .firstOrNull()?.let { row ->
                    row[ChainTable.nodeVrfVkey]
                }
        }
    }

    fun insertAll(blocks: List<ChainBlock>) {
        transaction {
            blocks.forEach { block ->
                ChainTable.deleteWhere { blockNumber greaterEq block.blockNumber }
                val prevEtaV: String = ChainTable.slice(
                    ChainTable.etaV
                ).select { ChainTable.slotNumber eqSubQuery ChainTable.slice(ChainTable.slotNumber.max()).selectAll() }
                    .singleOrNull()?.get(ChainTable.etaV)
                    ?: Config.shelleyGenesisHash

                val chainId = ChainTable.insertAndGetId { row ->
                    row[blockNumber] = block.blockNumber
                    row[slotNumber] = block.slotNumber
                    row[hash] = block.hash
                    row[prevHash] = block.prevHash
                    row[poolId] = nodeVKeyToPoolId(block.nodeVkey)
                    row[etaV] = calculateEtaV(prevEtaV, block.etaVrf0)
                    row[nodeVkey] = block.nodeVkey
                    row[nodeVrfVkey] = block.nodeVrfVkey
                    row[blockVrf0] = block.blockVrf
                    row[blockVrf1] = block.blockVrfProof
                    row[etaVrf0] = block.etaVrf0
                    row[etaVrf1] = block.etaVrf1
                    row[leaderVrf0] = block.leaderVrf0
                    row[leaderVrf1] = block.leaderVrf1
                    row[blockSize] = block.blockSize
                    row[blockBodyHash] = block.blockBodyHash
                    row[poolOpcert] = block.poolOpcert
                    row[sequenceNumber] = block.sequenceNumber
                    row[kesPeriod] = block.kesPeriod
                    row[sigmaSignature] = block.sigmaSignature
                    row[protocolMajorVersion] = block.protocolMajorVersion
                    row[protocolMinorVersion] = block.protocolMinorVersion
                    row[created] = System.currentTimeMillis()
                    row[processed] = false
                }.value

                if (block.transactionDestAddresses.isNotEmpty()) {
                    TransactionDestAddressTable.batchInsert(
                        block.transactionDestAddresses,
                        shouldReturnGeneratedValues = false
                    ) {
                        this[TransactionDestAddressTable.address] = it
                        this[TransactionDestAddressTable.chainId] = chainId
                        this[TransactionDestAddressTable.processed] = false
                    }
                }

                if (block.stakeDestAddresses.isNotEmpty()) {
                    // Ignore errors as we want to just keep the existing record as-is because it's older
                    PaymentStakeAddressTable.batchInsert(
                        block.stakeDestAddresses,
                        ignore = true,
                        shouldReturnGeneratedValues = false
                    ) {
                        this[PaymentStakeAddressTable.receivingAddress] = it.receivingAddress
                        this[PaymentStakeAddressTable.stakeAddress] = it.stakeAddress
                    }
                }
            }
        }
    }

    fun insert(block: ChainBlock): Long {
        return transaction {
            ChainTable.deleteWhere { blockNumber greaterEq block.blockNumber }
            val prevEtaV: String = ChainTable.slice(
                ChainTable.etaV
            ).select { ChainTable.slotNumber eqSubQuery ChainTable.slice(ChainTable.slotNumber.max()).selectAll() }
                .singleOrNull()?.get(ChainTable.etaV)
                ?: Config.shelleyGenesisHash

            val chainId = ChainTable.insertAndGetId { row ->
                row[blockNumber] = block.blockNumber
                row[slotNumber] = block.slotNumber
                row[hash] = block.hash
                row[prevHash] = block.prevHash
                row[poolId] = nodeVKeyToPoolId(block.nodeVkey)
                row[etaV] = calculateEtaV(prevEtaV, block.etaVrf0)
                row[nodeVkey] = block.nodeVkey
                row[nodeVrfVkey] = block.nodeVrfVkey
                row[blockVrf0] = block.blockVrf
                row[blockVrf1] = block.blockVrfProof
                row[etaVrf0] = block.etaVrf0
                row[etaVrf1] = block.etaVrf1
                row[leaderVrf0] = block.leaderVrf0
                row[leaderVrf1] = block.leaderVrf1
                row[blockSize] = block.blockSize
                row[blockBodyHash] = block.blockBodyHash
                row[poolOpcert] = block.poolOpcert
                row[sequenceNumber] = block.sequenceNumber
                row[kesPeriod] = block.kesPeriod
                row[sigmaSignature] = block.sigmaSignature
                row[protocolMajorVersion] = block.protocolMajorVersion
                row[protocolMinorVersion] = block.protocolMinorVersion
                row[created] = System.currentTimeMillis()
                row[processed] = false
            }.value

            if (block.transactionDestAddresses.isNotEmpty()) {
                TransactionDestAddressTable.batchInsert(block.transactionDestAddresses) {
                    this[TransactionDestAddressTable.address] = it
                    this[TransactionDestAddressTable.chainId] = chainId
                    this[TransactionDestAddressTable.processed] = false
                }
            }

            if (block.stakeDestAddresses.isNotEmpty()) {
                // Ignore errors as we want to just keep the existing record as-is because it's older
                PaymentStakeAddressTable.batchInsert(
                    block.stakeDestAddresses,
                    ignore = true,
                    shouldReturnGeneratedValues = false
                ) {
                    this[PaymentStakeAddressTable.receivingAddress] = it.receivingAddress
                    this[PaymentStakeAddressTable.stakeAddress] = it.stakeAddress
                }
            }

            chainId
        }
    }

    fun markNonPurchaseAddressesAsProcessed() {
        transaction {
            // logDebug(log)
            TransactionDestAddressTable.update({
                TransactionDestAddressTable.processed.eq(false) and
                    notExists(
                        KeysTable.slice(KeysTable.id).select {
                            TransactionDestAddressTable.processed.eq(false) and
                                KeysTable.address.eq(TransactionDestAddressTable.address)
                        }
                    )
            }) {
                it[processed] = true
            }
        }
    }

    /**
     * Check to see if this address we're waiting on has an unprocessed transaction on the blockchain 3 behind tip
     */
    fun shouldProcessAddress(address: String): Boolean {
        return transaction {
            // logDebug(log)
            val updateCount = TransactionDestAddressTable.update({
                TransactionDestAddressTable.address.eq(address) and TransactionDestAddressTable.processed.eq(false) and
                    exists(
                        (ChainTable innerJoin TransactionDestAddressTable).slice(ChainTable.blockNumber).select {
                            (TransactionDestAddressTable.address eq address) and
                                (TransactionDestAddressTable.processed eq false) and
                                (
                                    ChainTable.blockNumber + STABILITY_WINDOW lessEq wrapAsExpression(
                                        ChainTable.slice(ChainTable.blockNumber.max()).selectAll()
                                    )
                                    )
                        }
                    )
            }) {
                it[processed] = true
            }

            updateCount > 0
        }
    }

    private fun nodeVKeyToPoolId(nodeVKey: String): String {
        val vKeyByteArray = nodeVKey.hexToByteArray()
        val hash = Blake2b.hash224(vKeyByteArray)
        return hash.toHexString()
    }

    /**
     * Evolve the etaV nonce value
     */
    private fun calculateEtaV(prevEtaV: String, etaVrf0: String): String {
        val prevEtaVBytes = prevEtaV.hexToByteArray()
        val etaVrf0Bytes = etaVrf0.hexToByteArray()
        val eta = Blake2b.hash256(etaVrf0Bytes)

        val combined = prevEtaVBytes + eta
        val etaV = Blake2b.hash256(combined)

        return etaV.toHexString()
    }
}
