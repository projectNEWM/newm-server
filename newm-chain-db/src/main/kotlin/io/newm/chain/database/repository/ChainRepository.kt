package io.newm.chain.database.repository

import io.newm.chain.database.entity.ChainBlock
import io.newm.chain.database.entity.MonitoredAddressChain
import io.newm.kogmios.protocols.model.PointDetail

interface ChainRepository {
    fun getFindIntersectPairs(): List<PointDetail>

    fun getPointAfterSlot(slot: Long): PointDetail?

    fun getVrfByPoolId(poolId: String): String?

    fun insertAll(blocks: List<ChainBlock>)

    fun insert(block: ChainBlock): Long

    fun rollback(blockNumber: Long): Int

    fun rollbackMonitoredAddressChain(address: String, blockNumber: Long): Int

    fun pruneMonitoredAddressChainHistory(address: String, currentBlockNumber: Long): Int

    fun getFindIntersectPairsAddressChain(address: String): List<PointDetail>

    fun insertMonitoredAddressChain(monitoredAddressChain: MonitoredAddressChain): Long

    fun markTipMonitoredAddressChain(address: String): Long
}
