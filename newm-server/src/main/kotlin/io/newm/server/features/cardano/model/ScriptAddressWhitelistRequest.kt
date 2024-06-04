package io.newm.server.features.cardano.model

import kotlinx.serialization.Serializable

@Serializable
data class ScriptAddressWhitelistRequest(
    val scriptAddress: String,
)
