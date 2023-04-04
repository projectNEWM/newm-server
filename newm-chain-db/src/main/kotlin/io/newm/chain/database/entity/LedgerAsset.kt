package io.newm.chain.database.entity

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.math.BigInteger

@Serializable
data class LedgerAsset(
    val id: Long? = null,

    // policy id for this asset
    val policy: String,

    // name for this asset
    val name: String,

    // the total supply of this asset
    @Contextual
    val supply: BigInteger,
)
