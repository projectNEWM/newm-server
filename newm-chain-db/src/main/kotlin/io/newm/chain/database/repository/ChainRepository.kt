package io.newm.chain.database.repository

import io.newm.chain.database.entity.ChainBlock
import io.newm.kogmios.protocols.model.PointDetail

interface ChainRepository {
    fun getFindIntersectPairs(): List<PointDetail>

    fun getPointAfterSlot(slot: Long): PointDetail?

    fun getVrfByPoolId(poolId: String): String?

    fun insertAll(blocks: List<ChainBlock>)

    fun insert(block: ChainBlock): Long
}
