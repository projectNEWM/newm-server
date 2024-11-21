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
import io.newm.shared.ktx.isValidName
import io.newm.shared.ktx.isValidUrl
import io.newm.shared.ktx.toHexString
import io.newm.txbuilder.ktx.toNativeAssetMap

fun String.checkLength(
    name: String,
    max: Int = 64
) {
    if (length > max) throw HttpUnprocessableEntityException("Field $name exceeds $max chars limit")
}

fun String?.asValidName(): String {
    if (isNullOrBlank()) throw HttpBadRequestException("Missing name")
    if (!isValidName()) throw HttpUnprocessableEntityException("Invalid name: $this")
    return this
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

fun String?.asUrlWithHost(host: String): String? {
    if (isNullOrBlank()) return null
    if (!isValidUrl()) throw HttpUnprocessableEntityException("Invalid url: $this")
    val updatedUrl = replace(Regex("^(http://|https://)"), "")
    if (!updatedUrl.startsWith(host, ignoreCase = true)) {
        throw HttpUnprocessableEntityException("Invalid url: $this, expected $host")
    }
    return "https://${updatedUrl.substringBefore('?')}"
}

fun String.arweaveToWebUrl(): String {
    require(startsWith("ar://")) { "Invalid Arweave URL: $this" }
    return "https://arweave.net/${substring(5)}"
}

/**
 * Converts an s3://url into a bucket and key pair.
 */
fun String.toBucketAndKey(): Pair<String, String> {
    val bucket = substringAfter("s3://").substringBefore('/')
    val key = substringAfter(bucket).substringAfter('/')
    return bucket to key
}

fun String.getFileNameWithExtensionFromUrl(): String = substringBefore('?', missingDelimiterValue = this).substringAfterLast('/')

fun String.toAudioContentType(): String =
    when (lowercase().substringAfterLast('.', missingDelimiterValue = this)) {
        "flac" -> "audio/x-flac"
        "mp3" -> "audio/mpeg"
        "wav" -> "audio/wav"
        "ogg" -> "audio/ogg"
        "aiff", "aif" -> "audio/aiff"
        "m4a" -> "audio/mp4"
        else -> "audio/*"
    }

fun String.toReferenceUtxo(): Utxo {
    require(this.contains('#')) { "Invalid utxo reference: $this" }
    val parts = this.split('#')
    return utxo {
        hash = parts[0]
        ix = parts[1].toLong()
    }
}

fun List<String>?.cborHexToUtxos(): List<Utxo> = this?.map { it.cborHexToUtxo() }.orEmpty()

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
 *
 * Example with a map instead of an array
 * [
 *     [
 *         h'c339d93cde910141e601f567fc51e48497140d0fd5272825bd352ad9aba0606f',
 *         0,
 *     ],
 *     {
 *         0: h'01ca91bd1ac67697bb7b719884fb761f629e68dd8880830701da5e74e8e5da90ed1214ed485c488d58a12ad5ca7ca6ddec4d9ffab270a02df8',
 *         1: [
 *             1452470_2,
 *             {
 *                 h'477cec772adb1466b301fb8161f505aa66ed1ee8d69d3e7984256a43': {h'477574656e62657267204269626c65202330323138': 1},
 *             },
 *         ],
 *         2: [
 *             1,
 *             24_0(h'd8799fd8799fd8799f5820a1ef64c4d16b456a67de3b50af86051a1374159c5c28f79d05e8b3e0dd61f0bdff00ffff'),
 *         ],
 *     },
 * ]
 *
 * Example where NativeAssets use an array internally instead of map
 * [
 *     [
 *         h'de03dc84fd819ac5fc1dba2e48fa18d1390b71dd022bed02eb26e276b92cece6',
 *         0,
 *     ],
 *     [
 *         h'002b5a4101c329c6613cd51091509a4f50b6028a8f20c1a161ecbb5dda55e80300b56dd9f4dc643fe0d10d099598c647ea4588c54699e8548a',
 *         [
 *             1176630_2,
 *             {
 *                 h'769c4c6e9bc3ba5406b9b89fb7beb6819e638ff2e2de63f008d5bcff': {h'744e45574d': 10000000000_3},
 *             },
 *         ],
 *     ],
 * ]
 */
fun String.cborHexToUtxo(): Utxo {
    try {
        CborObject.createFromCborByteArray(hexToByteArray()).let { cborObject ->
            return when (cborObject) {
                is CborArray -> {
                    val hashIxArray = cborObject.elementAt(0) as CborArray
                    val value = cborObject.elementAt(1)
                    when (value) {
                        is CborArray -> {
                            utxo {
                                hash = hashIxArray.elementToHexString(0)
                                ix = hashIxArray.elementToInt(1).toLong()
                                address =
                                    value.elementToByteArray(0).let { bytes ->
                                        if (bytes[0] in PAYMENT_ADDRESS_PREFIXES_MAINNET) {
                                            Bech32.encode("addr", bytes)
                                        } else {
                                            Bech32.encode("addr_test", bytes)
                                        }
                                    }
                                val valueAmount = value.elementAt(1)
                                lovelace =
                                    when (valueAmount) {
                                        is CborInteger -> valueAmount.bigIntegerValue().toString()
                                        is CborArray -> valueAmount.elementToBigInteger(0).toString()
                                        else -> throw IllegalArgumentException(
                                            "Invalid CBOR type: ${valueAmount.javaClass.name}, expected CborInteger or CborArray for lovelace!"
                                        )
                                    }
                                if (valueAmount is CborArray) {
                                    nativeAssets.addAll(
                                        (valueAmount.elementAt(1) as CborMap)
                                            .mapValue()
                                            .flatMap { (policyCborObject, valueCborObject) ->
                                                val policy =
                                                    (policyCborObject as CborByteString).byteArrayValue()[0].toHexString()
                                                val namesCborMap = (valueCborObject as CborMap).mapValue()
                                                namesCborMap.map { (nameCborObject, amountCborObject) ->
                                                    val name =
                                                        (nameCborObject as CborByteString)
                                                            .byteArrayValue()
                                                            .getOrElse(0) { ByteArray(0) }
                                                            .toHexString()
                                                    val amount =
                                                        (amountCborObject as CborInteger).bigIntegerValue().toString()
                                                    nativeAsset {
                                                        this.policy = policy
                                                        this.name = name
                                                        this.amount = amount
                                                    }
                                                }
                                            }.toNativeAssetMap()
                                            .values
                                            .flatten()
                                    )
                                }
                            }
                        }

                        is CborMap -> {
                            utxo {
                                hash = hashIxArray.elementToHexString(0)
                                ix = hashIxArray.elementToInt(1).toLong()
                                value.get(CborInteger.create(0))?.let { addressCborObject ->
                                    (addressCborObject as CborByteString).byteArrayValue()[0].let { bytes ->
                                        address =
                                            if (bytes[0] in PAYMENT_ADDRESS_PREFIXES_MAINNET) {
                                                Bech32.encode("addr", bytes)
                                            } else {
                                                Bech32.encode("addr_test", bytes)
                                            }
                                    }
                                }
                                val valueAmount = value.get(CborInteger.create(1))
                                lovelace =
                                    when (valueAmount) {
                                        is CborInteger -> valueAmount.bigIntegerValue().toString()
                                        is CborArray -> valueAmount.elementToBigInteger(0).toString()
                                        else -> throw IllegalArgumentException(
                                            "Invalid CBOR type: ${valueAmount?.javaClass?.name}, expected CborInteger or CborArray for lovelace!"
                                        )
                                    }
                                if (valueAmount is CborArray) {
                                    nativeAssets.addAll(
                                        (valueAmount.elementAt(1) as CborMap)
                                            .mapValue()
                                            .flatMap { (policyCborObject, valueCborObject) ->
                                                val policy =
                                                    (policyCborObject as CborByteString).byteArrayValue()[0].toHexString()
                                                val namesCborMap = (valueCborObject as CborMap).mapValue()
                                                namesCborMap.map { (nameCborObject, amountCborObject) ->
                                                    val name =
                                                        (nameCborObject as CborByteString)
                                                            .byteArrayValue()
                                                            .getOrElse(0) { ByteArray(0) }
                                                            .toHexString()
                                                    val amount =
                                                        (amountCborObject as CborInteger).bigIntegerValue().toString()
                                                    nativeAsset {
                                                        this.policy = policy
                                                        this.name = name
                                                        this.amount = amount
                                                    }
                                                }
                                            }.toNativeAssetMap()
                                            .values
                                            .flatten()
                                    )
                                }
                            }
                        }

                        else -> throw IllegalArgumentException("Invalid CBOR type: ${value.javaClass.name}, expected CborArray or CborMap!")
                    }
                }

                else -> throw IllegalArgumentException("Invalid CBOR type: ${cborObject.javaClass.name}, expected CborArray!")
            }
        }
    } catch (e: Exception) {
        throw IllegalArgumentException("Unable to parse cbor utxo: $this", e)
    }
}
