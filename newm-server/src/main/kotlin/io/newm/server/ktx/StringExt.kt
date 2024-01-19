package io.newm.server.ktx

import com.google.iot.cbor.CborArray
import com.google.iot.cbor.CborByteString
import com.google.iot.cbor.CborInteger
import com.google.iot.cbor.CborMap
import com.google.iot.cbor.CborObject
import io.newm.chain.grpc.Utxo
import io.newm.chain.grpc.nativeAsset
import io.newm.chain.grpc.utxo
import io.newm.chain.util.Bech32
import io.newm.chain.util.Constants.PAYMENT_ADDRESS_PREFIXES_MAINNET
import io.newm.chain.util.elementToBigInteger
import io.newm.chain.util.elementToByteArray
import io.newm.chain.util.elementToHexString
import io.newm.chain.util.elementToInt
import io.newm.chain.util.hexToByteArray
import io.newm.shared.exception.HttpBadRequestException
import io.newm.shared.exception.HttpUnprocessableEntityException
import io.newm.shared.ktx.isValidEmail
import io.newm.shared.ktx.isValidUrl
import io.newm.shared.ktx.toHexString
import io.newm.txbuilder.ktx.toNativeAssetMap

fun String.checkLength(name: String, max: Int = 64) {
    if (length > max) throw HttpUnprocessableEntityException("Field $name exceeds $max chars limit")
}

fun String?.asValidEmail(): String {
    if (isNullOrBlank()) throw HttpBadRequestException("Missing email")
    if (!isValidEmail()) throw HttpUnprocessableEntityException("Invalid email: $this")
    return this
}

fun String?.asValidUrl(): String {
    if (isNullOrBlank()) throw HttpBadRequestException("Missing url")
    if (!isValidUrl()) throw HttpUnprocessableEntityException("Invalid url: $this")
    return this
}

/**
 * Converts an s3://url into a bucket and key pair.
 */
fun String.toBucketAndKey(): Pair<String, String> {
    val bucket = substringAfter("s3://").substringBefore('/')
    val key = substringAfter(bucket).substringAfter('/')
    return bucket to key
}

fun String.getFileNameWithExtensionFromUrl(): String {
    return substringBefore('?', missingDelimiterValue = this).substringAfterLast('/')
}

fun String.toAudioContentType(): String {
    return when (lowercase().substringAfterLast('.', missingDelimiterValue = this)) {
        "flac" -> "audio/x-flac"
        "mp3" -> "audio/mpeg"
        "wav" -> "audio/wav"
        "ogg" -> "audio/ogg"
        "aiff", "aif" -> "audio/aiff"
        "m4a" -> "audio/mp4"
        else -> "audio/*"
    }
}

fun String.toReferenceUtxo(): Utxo {
    require(this.contains('#')) { "Invalid utxo reference: $this" }
    val parts = this.split('#')
    return utxo {
        hash = parts[0]
        ix = parts[1].toLong()
    }
}

/**
 * Converts a CBOR hex string from a CIP-30 wallet into a Utxo.
 * Example with NativeAssets
 * [
 *     [
 *         h'bc43a0453f0681d841798aafa11be39c49215ed9031d6dc29cb79dfeda05239a',
 *         1,
 *     ],
 *     [
 *         h'003a5cbc099211950e25f9877ab7abd63d056d515bd64823a5de83dc2653daab1c1256d159150ed263bec087e6a6109b465b0150e9e3bb4269',
 *         [
 *             1288690_2,
 *             {
 *                 h'36a4b27112c109a41086900abc145322b16921c522e4ff8fc9dd6978': {
 *                     h'001bc280005c3595c1532c1059535a65ccbba1b893fd3ee58f3d2b69dae23b70': 100000000_2,
 *                 },
 *             },
 *         ],
 *     ],
 * ]
 *
 * Example without NativeAssets
 * [
 *     [
 *         h'a6ff665ae54eefcfc71daf938aa557907af18b33554f2a44b5229cbea80ca411',
 *         1,
 *     ],
 *     [
 *         h'003a5cbc099211950e25f9877ab7abd63d056d515bd64823a5de83dc2653daab1c1256d159150ed263bec087e6a6109b465b0150e9e3bb4269',
 *         278629243_2,
 *     ],
 * ]
 */
fun String.cborHexToUtxo(): Utxo {
    try {
        CborObject.createFromCborByteArray(hexToByteArray()).let { cborObject ->
            return when (cborObject) {
                is CborArray -> {
                    val hashIxArray = cborObject.elementAt(0) as CborArray
                    val value = cborObject.elementAt(1) as CborArray
                    utxo {
                        hash = hashIxArray.elementToHexString(0)
                        ix = hashIxArray.elementToInt(1).toLong()
                        address = value.elementToByteArray(0).let { bytes ->
                            if (bytes[0] in PAYMENT_ADDRESS_PREFIXES_MAINNET) {
                                Bech32.encode("addr", bytes)
                            } else {
                                Bech32.encode("addr_test", bytes)
                            }
                        }
                        val valueAmount = value.elementAt(1)
                        lovelace = when (valueAmount) {
                            is CborInteger -> valueAmount.bigIntegerValue().toString()
                            is CborArray -> valueAmount.elementToBigInteger(0).toString()
                            else -> throw IllegalArgumentException("Invalid CBOR type: ${valueAmount.javaClass.name}, expected CborInteger or CborArray for lovelace!")
                        }
                        if (valueAmount is CborArray) {
                            nativeAssets.addAll(
                                (valueAmount.elementAt(1) as CborMap).mapValue()
                                    .flatMap { (policyCborObject, valueCborObject) ->
                                        val policy =
                                            (policyCborObject as CborByteString).byteArrayValue()[0].toHexString()
                                        val namesCborMap = (valueCborObject as CborMap).mapValue()
                                        namesCborMap.map { (nameCborObject, amountCborObject) ->
                                            val name =
                                                (nameCborObject as CborByteString).byteArrayValue()[0].toHexString()
                                            val amount = (amountCborObject as CborInteger).bigIntegerValue().toString()
                                            nativeAsset {
                                                this.policy = policy
                                                this.name = name
                                                this.amount = amount
                                            }
                                        }
                                    }.toNativeAssetMap().values.flatten()
                            )
                        }
                    }
                }

                else -> throw IllegalArgumentException("Invalid CBOR type: ${cborObject.javaClass.name}, expected CborArray!")
            }
        }
    } catch (e: Exception) {
        throw IllegalArgumentException("Unable to parse cbor utxo: $this", e)
    }
}
