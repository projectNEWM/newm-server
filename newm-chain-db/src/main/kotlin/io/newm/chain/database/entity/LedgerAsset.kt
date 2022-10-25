package io.newm.chain.database.entity

import kotlinx.serialization.Serializable

@Serializable
data class LedgerAsset(
    val id: Long? = null,

    // policy id for this asset
    val policy: String,

    // name for this asset
    val name: String,

    // the asset image ipfs link
    val image: String?,

    // the asset image description
    val description: String?,
)
