package io.newm.txbuilder.ktx

import com.google.iot.cbor.CborArray
import com.google.iot.cbor.CborInteger
import com.google.iot.cbor.CborMap
import com.google.iot.cbor.CborObject
import io.newm.chain.grpc.Redeemer
import io.newm.chain.grpc.TransactionBuilderResponse
import io.newm.chain.grpc.exUnits
import io.newm.chain.grpc.redeemer

private val KEY_FEE: CborInteger = CborInteger.create(2)
private val KEY_TOTAL_COLLATERAL = CborInteger.create(17)
private val KEY_REDEEMERS: CborInteger = CborInteger.create(5)

interface TxFields {
    val fee: Long
    val totalCollateral: Long
    val redeemers: List<Redeemer>
    // TODO: add more fields later as needed
}

fun TransactionBuilderResponse.extractFields(): TxFields {
    val tx = CborObject.createFromCborByteArray(transactionCbor.toByteArray()) as CborArray
    val txBody = tx.elementAt(0) as CborMap
    val witnessSet = tx.elementAt(1) as CborMap
    return object : TxFields {
        override val fee: Long by lazy {
            (txBody[KEY_FEE] as CborInteger).longValue()
        }

        override val totalCollateral: Long by lazy {
            (txBody[KEY_TOTAL_COLLATERAL] as CborInteger).longValue()
        }

        override val redeemers: List<Redeemer> by lazy {
            when (witnessSet[KEY_REDEEMERS]) {
                is CborArray -> {
                    (witnessSet[KEY_REDEEMERS] as CborArray).map {
                        redeemer {
                            val redeemerArray = it as CborArray
                            tagValue = (redeemerArray.elementAt(0) as CborInteger).intValueExact()
                            index = (redeemerArray.elementAt(1) as CborInteger).longValue()
                            data = redeemerArray.elementAt(2).toPlutusData()
                            exUnits = exUnits {
                                val exUnitsArray = redeemerArray.elementAt(3) as CborArray
                                mem = (exUnitsArray.elementAt(0) as CborInteger).longValue()
                                steps = (exUnitsArray.elementAt(1) as CborInteger).longValue()
                            }
                        }
                    }
                }

                is CborMap -> {
                    (witnessSet[KEY_REDEEMERS] as CborMap).entrySet().map { (key, value) ->
                        redeemer {
                            val keyArray = key as CborArray
                            val redeemerArray = value as CborArray
                            tagValue = (keyArray.elementAt(0) as CborInteger).intValueExact()
                            index = (keyArray.elementAt(1) as CborInteger).longValue()
                            data = redeemerArray.elementAt(0).toPlutusData()
                            exUnits = exUnits {
                                val exUnitsArray = redeemerArray.elementAt(1) as CborArray
                                mem = (exUnitsArray.elementAt(0) as CborInteger).longValue()
                                steps = (exUnitsArray.elementAt(1) as CborInteger).longValue()
                            }
                        }
                    }
                }

                else -> throw IllegalStateException("Expected redeemers to be a CborArray or CborMap")
            }
        }

        override fun toString(): String = "TxFields(fee=$fee, totalCollateral=$totalCollateral, redeemers=$redeemers)"
    }
}
