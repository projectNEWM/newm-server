package io.newm.txbuilder.ktx

import com.google.iot.cbor.CborArray
import com.google.iot.cbor.CborInteger
import com.google.iot.cbor.CborMap
import com.google.iot.cbor.CborObject
import java.math.BigInteger

/**
 * Convert Kogmios object into a cbor object that can be used as part of the scriptDataHash calculation.
 */
fun List<BigInteger>.toCborObject(): CborObject =
    CborMap.create(
        mapOf(
            LANGUAGE_KEY_PLUTUSV2 to
                CborArray.create(
                    this.map { CborInteger.create(it) }
                )
        )
    )

// PlutusV1 == 0, PlutusV2 == 1
private val LANGUAGE_KEY_PLUTUSV2 by lazy { CborInteger.create(1) }
