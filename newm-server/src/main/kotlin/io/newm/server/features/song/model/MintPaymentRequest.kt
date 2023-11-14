package io.newm.server.features.song.model

import com.google.iot.cbor.CborArray
import com.google.iot.cbor.CborByteString
import com.google.iot.cbor.CborInteger
import com.google.iot.cbor.CborMap
import com.google.iot.cbor.CborReader
import io.newm.chain.grpc.Utxo
import io.newm.chain.grpc.nativeAsset
import io.newm.chain.grpc.utxo
import io.newm.chain.util.elementToBigInteger
import io.newm.chain.util.elementToHexString
import io.newm.chain.util.elementToInt
import io.newm.chain.util.hexToByteArray
import io.newm.chain.util.toHexString
import kotlinx.serialization.Serializable

@Serializable
data class MintPaymentRequest(
    val changeAddress: String,
    val utxoCborHexList: List<String>? = null,
) {
    val utxos: List<Utxo>
        get() = utxoCborHexList?.map {
            val cborArray = CborReader.createFromByteArray(it.hexToByteArray()).readDataItem() as CborArray
            val utxoArray = cborArray.elementAt(0) as CborArray
            val amountsArray = cborArray.elementAt(1) as CborArray
            utxo {
                hash = utxoArray.elementToHexString(0)
                ix = utxoArray.elementToInt(1).toLong()
                val amounts = amountsArray.elementAt(1)
                lovelace = when (amounts) {
                    is CborInteger -> amounts.bigIntegerValue().toString()
                    is CborArray -> amounts.elementToBigInteger(0).toString()
                    else -> "0"
                }
                if (amounts is CborArray) {
                    val nativeAssetCborMap = amounts.elementAt(1) as CborMap
                    nativeAssets.addAll(
                        nativeAssetCborMap.entrySet().flatMap { (policyCbor, namesCborMap) ->
                            (namesCborMap as CborMap).entrySet().map { (nameCbor, amountCbor) ->
                                val policy = (policyCbor as CborByteString).byteArrayValue().toHexString()
                                val name = (nameCbor as CborByteString).byteArrayValue().toHexString()
                                val amount = (amountCbor as CborInteger).bigIntegerValue().toString()
                                nativeAsset {
                                    this.policy = policy
                                    this.name = name
                                    this.amount = amount
                                }
                            }
                        }
                    )
                }
            }
        }.orEmpty()
}
