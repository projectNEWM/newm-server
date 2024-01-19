package io.newm.chain.database.repository

import com.github.benmanes.caffeine.cache.Caffeine
import io.newm.chain.config.Config
import io.newm.chain.database.entity.ChainBlock
import io.newm.chain.database.entity.MonitoredAddressChain
import io.newm.chain.database.table.ChainTable
import io.newm.chain.database.table.MonitoredAddressChainTable
import io.newm.chain.database.table.PaymentStakeAddressTable
import io.newm.chain.util.Blake2b
import io.newm.chain.util.hexToByteArray
import io.newm.chain.util.toHexString
import io.newm.kogmios.protocols.model.PointDetail
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greater
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.max
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class ChainRepositoryImpl : ChainRepository {
    private val log: Logger by lazy { LoggerFactory.getLogger("ChainRepository") }

    override fun getFindIntersectPairs(): List<PointDetail> {
        return transaction {
            ChainTable.select(
                ChainTable.slotNumber,
                ChainTable.hash
            ).orderBy(ChainTable.slotNumber, SortOrder.DESC)
                .limit(33).filterIndexed { index, _ ->
                    // all powers of 2 including 0th element 0, 2, 4, 8, 16, 32
                    (index == 0) || ((index > 1) && (index and (index - 1) == 0))
                }.map { row ->
                    PointDetail(
                        slot = row[ChainTable.slotNumber],
                        id = row[ChainTable.hash],
                    )
                }
        }
    }

    override fun getPointAfterSlot(slot: Long): PointDetail? {
        return transaction {
            ChainTable.select(
                ChainTable.slotNumber,
                ChainTable.hash
            ).where { ChainTable.slotNumber greater slot }
                .orderBy(ChainTable.slotNumber, SortOrder.ASC)
                .limit(1).firstOrNull()?.let { row ->
                    PointDetail(
                        slot = row[ChainTable.slotNumber],
                        id = row[ChainTable.hash],
                    )
                }
        }
    }

    override fun getVrfByPoolId(poolId: String): String? {
        return transaction {
            ChainTable.select(ChainTable.nodeVrfVkey)
                .where { ChainTable.poolId eq poolId }
                .orderBy(ChainTable.id, SortOrder.DESC)
                .limit(1)
                .firstOrNull()?.let { row ->
                    row[ChainTable.nodeVrfVkey]
                }
        }
    }

    override fun insertAll(blocks: List<ChainBlock>) {
        transaction {
            blocks.forEach { block ->
                ChainTable.deleteWhere { blockNumber greaterEq block.blockNumber }
                val prevEtaV: String = ChainTable.select(
                    ChainTable.etaV
                ).where { ChainTable.slotNumber eqSubQuery ChainTable.select(ChainTable.slotNumber.max()) }
                    .singleOrNull()?.get(ChainTable.etaV)
                    ?: Config.shelleyGenesisHash

                ChainTable.insertAndGetId { row ->
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
                }.value

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

    private var prevEtaVCache = Caffeine.newBuilder().maximumSize(100L).build<Long, String>()

    override fun insert(block: ChainBlock): Long {
        return transaction {
            val prevEtaV: String = prevEtaVCache.getIfPresent(block.blockNumber - 1) ?: ChainTable.select(
                ChainTable.etaV
            ).where { ChainTable.blockNumber eq (block.blockNumber - 1) }.singleOrNull()?.get(ChainTable.etaV)
                ?: Config.shelleyGenesisHash

            val newEtaV = calculateEtaV(prevEtaV, block.etaVrf0)
            val chainId = ChainTable.insertAndGetId { row ->
                row[blockNumber] = block.blockNumber
                row[slotNumber] = block.slotNumber
                row[hash] = block.hash
                row[prevHash] = block.prevHash
                row[poolId] = nodeVKeyToPoolId(block.nodeVkey)
                row[etaV] = newEtaV
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
            }.value
            prevEtaVCache.put(block.blockNumber, newEtaV)

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

    override fun rollback(blockNumber: Long) = transaction {
        ChainTable.deleteWhere { ChainTable.blockNumber greaterEq blockNumber }
    }

    override fun rollbackMonitoredAddressChain(address: String, blockNumber: Long): Int = transaction {
        MonitoredAddressChainTable.deleteWhere {
            (MonitoredAddressChainTable.address eq address) and
                (height greaterEq blockNumber)
        }
    }

    override fun pruneMonitoredAddressChainHistory(address: String, currentBlockNumber: Long): Int = transaction {
        MonitoredAddressChainTable.deleteWhere {
            (MonitoredAddressChainTable.address eq address) and
                (height less currentBlockNumber - 1000) and
                (height greater 0)
        }
    }

    override fun getFindIntersectPairsAddressChain(address: String): List<PointDetail> = transaction {
        MonitoredAddressChainTable.select(
            MonitoredAddressChainTable.slot,
            MonitoredAddressChainTable.hash
        ).where { MonitoredAddressChainTable.address eq address }
            .orderBy(MonitoredAddressChainTable.slot, SortOrder.DESC)
            .limit(33).filterIndexed { index, _ ->
                // all powers of 2 including 0th element 0, 2, 4, 8, 16, 32
                (index == 0) || ((index > 1) && (index and (index - 1) == 0))
            }.map { row ->
                PointDetail(
                    slot = row[MonitoredAddressChainTable.slot],
                    id = row[MonitoredAddressChainTable.hash],
                )
            }
    }

    override fun insertMonitoredAddressChain(monitoredAddressChain: MonitoredAddressChain): Long = transaction {
        MonitoredAddressChainTable.insertAndGetId { row ->
            row[address] = monitoredAddressChain.address
            row[height] = monitoredAddressChain.height
            row[slot] = monitoredAddressChain.slot
            row[hash] = monitoredAddressChain.hash
        }.value
    }

    override fun markTipMonitoredAddressChain(address: String): Long = transaction {
        MonitoredAddressChainTable.deleteWhere { MonitoredAddressChainTable.address eq address }
        MonitoredAddressChainTable.insertAndGetId { row ->
            row[MonitoredAddressChainTable.address] = address
            row[height] = -1
            row[slot] = -1
            row[hash] = ""
        }.value
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
