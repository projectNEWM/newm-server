package io.newm.txbuilder.ktx

import com.google.iot.cbor.CborArray
import com.google.iot.cbor.CborInteger
import com.google.iot.cbor.CborObject
import io.newm.chain.grpc.Redeemer
import io.newm.chain.grpc.RedeemerTag
import io.newm.kogmios.protocols.model.Validator

/**
 * Convert a redeemer object into cbor so it can be included in a transaction.
 */
fun Redeemer.toCborObject(dummyExUnitsMemory: Long, dummyExUnitsSteps: Long): CborObject {
    return CborArray.create(
        listOf(
            // redeemer tag
            CborInteger.create(tag.number),
            // redeemer index
            CborInteger.create(index),
            // plutus_data
            data.toCborObject(),
            // ex_units
            if (hasExUnits()) {
                CborArray.create(
                    listOf(
                        CborInteger.create(exUnits.mem),
                        CborInteger.create(exUnits.steps),
                    )
                )
            } else {
                // Dummy exUnits since we haven't calculated them with newmChainClient.evaluateTx() yet.
                // We need to provide something so it uses some bytes as a placeholder and the fee
                // calculations based on byte size can be correct.
                CborArray.create(
                    listOf(
                        CborInteger.create(dummyExUnitsMemory),
                        CborInteger.create(dummyExUnitsSteps),
                    )
                )
            }
        )
    )
}

/**
 * Convert the redeemer keys coming from Kogmios such as "spend:1" into a Tag and index value.
 */
fun Validator.toRedeemerTagAndIndex(): Pair<RedeemerTag, Long> {
    return Pair(
        when (this.purpose) {
            "spend" -> RedeemerTag.SPEND
            "mint" -> RedeemerTag.MINT
            "certificate" -> RedeemerTag.CERT
            "withdrawal" -> RedeemerTag.REWARD
            else -> throw IllegalArgumentException(
                "Unknown redeemer tag"
            )
        },
        this.index.toLong()
    )
}
