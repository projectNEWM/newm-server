package io.newm.chain.config

import io.newm.chain.util.Constants.NETWORK_MAGIC_MAINNET
import io.newm.kogmios.protocols.model.result.ShelleyGenesisConfigResult

object Config {
    lateinit var genesis: ShelleyGenesisConfigResult
    val isMainnet: Boolean by lazy { genesis.networkMagic == NETWORK_MAGIC_MAINNET }

    lateinit var shelleyGenesisHash: String
}
