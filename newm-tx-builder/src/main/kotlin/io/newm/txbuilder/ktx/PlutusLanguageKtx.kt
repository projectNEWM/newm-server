package io.newm.txbuilder.ktx

import com.google.iot.cbor.CborArray
import com.google.iot.cbor.CborInteger
import com.google.iot.cbor.CborMap
import com.google.iot.cbor.CborObject
import java.math.BigInteger

/**
 * Convert Kogmios object into a cbor object that can be used as part of the scriptDataHash calculation.
 */
fun List<BigInteger>.toCborObject(plutusLanguageKey: PlutusLanguageKey): CborObject =
    CborMap.create(
        mapOf(
            plutusLanguageKey.key to
                CborArray.create(
                    this.map { CborInteger.create(it) }
                )
        )
    )

enum class PlutusLanguageKey(
    val key: CborInteger
) {
    PLUTUSV1(CborInteger.create(0)),
    PLUTUSV2(CborInteger.create(1)),
    PLUTUSV3(CborInteger.create(2))
}
