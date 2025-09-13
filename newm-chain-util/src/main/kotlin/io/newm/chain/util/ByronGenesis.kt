package io.newm.chain.util

data class ByronGenesis(
    val startTime: Long,
    val protocolConsts: ProtocolConsts,
    val blockVersionData: BlockVersionData,
)

data class ProtocolConsts(
    val k: Long
)

data class BlockVersionData(
    val slotDuration: Long,
)
