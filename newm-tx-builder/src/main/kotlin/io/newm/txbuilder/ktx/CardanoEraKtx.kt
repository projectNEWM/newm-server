package io.newm.txbuilder.ktx

import com.google.iot.cbor.CborTag
import io.newm.kogmios.protocols.model.CardanoEra

fun CardanoEra.toSetTag(): Int =
    when (this) {
        CardanoEra.CONWAY -> 258
        else -> CborTag.UNTAGGED
    }
