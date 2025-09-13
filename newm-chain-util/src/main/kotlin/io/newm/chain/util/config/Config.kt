package io.newm.chain.util.config

import io.newm.chain.util.Constants
import io.newm.kogmios.protocols.model.result.ShelleyGenesisConfigResult

object Config {
    lateinit var genesis: ShelleyGenesisConfigResult
    val isMainnet: Boolean by lazy { genesis.networkMagic == Constants.NETWORK_MAGIC_MAINNET }

    lateinit var shelleyGenesisHash: String
}
