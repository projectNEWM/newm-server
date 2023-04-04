package io.newm.txbuilder.ktx

import com.google.iot.cbor.CborArray
import com.google.iot.cbor.CborByteString
import com.google.iot.cbor.CborInteger
import com.google.iot.cbor.CborMap
import com.google.iot.cbor.CborObject
import com.google.iot.cbor.CborReader
import com.google.iot.cbor.CborTag
import com.google.protobuf.ByteString
import com.google.protobuf.kotlin.toByteString
import io.newm.chain.grpc.PlutusData
import io.newm.chain.grpc.PlutusDataList
import io.newm.chain.grpc.PlutusDataMap
import io.newm.chain.grpc.PlutusDataMapItem
import io.newm.chain.util.hexToByteArray
import io.newm.chain.util.toHexString

fun String.cborHexToPlutusData(): PlutusData =
    CborReader.createFromByteArray(this.hexToByteArray()).readDataItem().toPlutusData(this)

/**
 * Read on-chain data back into PlutusData for monitoring addresses.
 */
fun CborObject.toPlutusData(cborHex: String? = null): PlutusData {
    val fields = this
    return PlutusData
        .newBuilder().apply {
            cborHex?.let {
                this.cborHex = it
            }
            if (fields.tag != CborTag.UNTAGGED) {
                constr = if (fields.tag >= 128) {
                    fields.tag - 1273
                } else {
                    fields.tag - 121
                }
            }

            when (fields) {
                is CborInteger -> int = fields.longValue()
                is CborByteString ->
                    bytes =
                        fields.byteArrayValue().takeUnless { it.isEmpty() }?.get(0)?.toByteString() ?: ByteString.EMPTY

                is CborArray -> list = PlutusDataList.newBuilder().apply {
                    addAllListItem(
                        fields.map { it.toPlutusData() }
                    )
                }.build()

                is CborMap -> map = PlutusDataMap.newBuilder().apply {
                    addAllMapItem(
                        fields.mapValue().entries.map { (k, v) ->
                            PlutusDataMapItem.newBuilder().apply {
                                mapItemKey = k.toPlutusData()
                                mapItemValue = v.toPlutusData()
                            }.build()
                        }
                    )
                }.build()

                else -> throw IllegalArgumentException(
                    "plutus_data fields must be int, bytes, array, or map!: ${
                        fields.toCborByteArray().toHexString()
                    }"
                )
            }
        }
        .build()
}

/**
 * Convert PlutusData into cbor for including in a transaction.
 */
fun PlutusData.toCborObject(): CborObject {
    // cbor hex is already defined. Just return that.
    if (this.hasCborHex() && cborHex.isNotBlank()) {
        return CborObject.createFromCborByteArray(cborHex.hexToByteArray())
    }

    // define the correct cborTag
    val cborTag = if (this.hasConstr()) {
        if (constr >= 7) {
            constr + 1273
        } else {
            constr + 121
        }
    } else {
        CborTag.UNTAGGED
    }

    // build the stuff from objects
    return when (this.plutusDataWrapperCase) {
        PlutusData.PlutusDataWrapperCase.MAP -> {
            CborMap.create(
                this.map.mapItemList.associate { plutusDataMapItem ->
                    Pair(
                        plutusDataMapItem.mapItemKey.toCborObject(),
                        plutusDataMapItem.mapItemValue.toCborObject(),
                    )
                },
                cborTag
            )
        }

        PlutusData.PlutusDataWrapperCase.LIST -> {
            CborArray.create(list.listItemList.map { it.toCborObject() }, cborTag)
        }

        PlutusData.PlutusDataWrapperCase.INT -> CborInteger.create(int, cborTag)
        PlutusData.PlutusDataWrapperCase.BYTES -> CborByteString.create(
            bytes.toByteArray(),
            0,
            bytes.size(),
            cborTag
        )

        else -> throw IllegalArgumentException("PlutusData must contain one of map, list, int, or bytes!")
    }
}
