package io.newm.chain.util

import com.google.iot.cbor.CborArray
import com.google.iot.cbor.CborByteString
import io.newm.chain.util.Constants.PAYMENT_ADDRESS_PREFIX_MAINNET
import io.newm.chain.util.Constants.PAYMENT_ADDRESS_PREFIX_TESTNET
import io.newm.chain.util.Constants.PAYMENT_STAKE_ADDRESS_KEY_KEY_PREFIX_MAINNET
import io.newm.chain.util.Constants.PAYMENT_STAKE_ADDRESS_KEY_KEY_PREFIX_TESTNET
import io.newm.chain.util.Constants.STAKE_ADDRESS_KEY_PREFIX_MAINNET
import io.newm.chain.util.Constants.STAKE_ADDRESS_KEY_PREFIX_TESTNET
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.math.BigInteger
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Base64
import java.util.HexFormat

private val log by lazy { LoggerFactory.getLogger("CardanoKtx") }

fun CborArray.elementToBigInteger(index: Int): BigInteger {
    val obj = elementAt(index).toJavaObject()
    return (obj as? BigInteger) ?: (obj as? Long)?.toBigInteger() ?: (obj as Int).toBigInteger()
}

fun CborArray.elementToInt(index: Int): Int {
    val obj = elementAt(index).toJavaObject()
    return (obj as? Int) ?: (obj as? Long)?.toInt() ?: (obj as BigInteger).toInt()
}

fun CborArray.elementToByteArray(index: Int): ByteArray = (elementAt(index) as CborByteString).byteArrayValue()[0]

fun CborArray.elementToHexString(index: Int): String = elementToByteArray(index).toHexString()

fun ByteArray.toHexString(
    fromIndex: Int = 0,
    toIndex: Int = size
): String = HexFormat.of().formatHex(this, fromIndex, toIndex)

fun Array<ByteArray>.toHexString(): String {
    val builder = StringBuilder()
    this.forEach { bytes ->
        builder.append(HexFormat.of().formatHex(bytes))
    }
    return builder.toString()
}

fun String.hexToByteArray(): ByteArray {
    try {
        return HexFormat.of().parseHex(this)
    } catch (e: Throwable) {
        log.error("Failed to decode hex string: $this", e)
        throw e
    }
}

fun String.b64ToByteArray(): ByteArray {
    try {
        // Check if this is a url-safe base64 string
        return if (this.contains('-') || this.contains('_')) {
            Base64.getUrlDecoder().decode(this)
        } else {
            Base64.getDecoder().decode(this)
        }
    } catch (e: Throwable) {
        log.error("Failed to decode base64 string: $this", e)
        throw e
    }
}

fun ByteArray.toB64String(): String = Base64.getEncoder().encodeToString(this)

private val hexRegex = Regex("([a-f\\d]{2})+")

fun String.assetNameToHexString(): String =
    if (hexRegex.matches(this)) {
        // it's already hex string
        this
    } else {
        // convert to hex string
        this.toByteArray().toHexString()
    }

fun String.hexStringToAssetName(): String =
    if (hexRegex.matches(this)) {
        // convert it
        String(this.hexToByteArray())
    } else {
        // it's already assetName
        this
    }

fun Int.toHexString(): String = Integer.toHexString(this)

fun Long.toHexString(): String = java.lang.Long.toHexString(this)

fun BigInteger.toAda(): BigDecimal = this.toBigDecimal(6)

fun BigInteger.toAdaString() = toAda().toPlainString()

private val MAX_ULONG = BigInteger("ffffffffffffffff", 16)

fun BigInteger.toULong(): ULong = (this and MAX_ULONG).toString(16).toULong(16)

fun ULong.toBigInteger(): BigInteger = BigInteger(this.toString(16), 16)

fun ULong.toLittleEndianBytes(): ByteArray {
    val buffer = ByteBuffer.allocate(8)
    buffer.order(ByteOrder.LITTLE_ENDIAN)
    buffer.putLong(this.toLong())
    return buffer.array()
}

fun Long.lovelaceToAdaString(): String = "₳${this.toBigDecimal().movePointLeft(6)}"

fun String.toHexPoolId(): String =
    try {
        Bech32.decode(this).bytes.toHexString()
    } catch (e: Throwable) {
        this
    }

fun String.extractStakeAddress(isMainnet: Boolean): String {
    val decodedReceiveAddress = Bech32.decode(this)

    // check the length of the address
    require(decodedReceiveAddress.bytes.size == 57) { "Not enough bytes in stake address for: $this" }

    val addressType = decodedReceiveAddress.bytes[0]

    // calculate the encoded stake address
    return if (isMainnet) {
        Bech32.encode(
            "stake",
            decodedReceiveAddress.bytes.sliceArray(28..56).apply {
                set(0, STAKE_ADDRESS_KEY_PREFIX_MAINNET)
            }
        )
    } else {
        Bech32.encode(
            "stake_test",
            decodedReceiveAddress.bytes.sliceArray(28..56).apply {
                set(0, STAKE_ADDRESS_KEY_PREFIX_TESTNET)
            }
        )
    }
}

fun String.extractCredentials(): Pair<String, String?> {
    try {
        val decodedReceiveAddress = Bech32.decode(this)
        val buffer = ByteBuffer.wrap(decodedReceiveAddress.bytes)
        val paymentBuf = ByteArray(28)
        // skip address type byte
        buffer.position(1)
        buffer.get(paymentBuf)
        val stakeBuf =
            if (buffer.remaining() == 28) {
                val stakeBuf = ByteArray(28)
                buffer.get(stakeBuf)
                stakeBuf
            } else {
                null
            }
        return Pair(paymentBuf.toHexString(), stakeBuf?.toHexString())
    } catch (e: Throwable) {
        log.error("Failed to extract credentials: $this", e)
        throw e
    }
}

/**
 * Extract the address type from an address
 */
fun String.addressType(): String =
    if (this.startsWith("addr")) {
        val addressBytes = Bech32.decode(this).bytes
        addressBytes[0].toUByte().toString(16).padStart(2, '0')
    } else {
        // TODO: Fix hardcoding for byron stuff
        "82"
    }

/**
 * flatten a list of maps into a single map
 */
fun <K, V> List<Map<K, V>>.flatten(): Map<K, V> {
    val list = this
    return mutableMapOf<K, V>().apply {
        for (innerMap in list) putAll(innerMap)
    }
}

fun ByteArray.hashToPaymentAddress(isMainnet: Boolean): String {
    require(size == 28 || size == 56) { "Invalid hash size: $size" }
    val prefix: String
    val firstByte: Byte
    if (isMainnet) {
        prefix = "addr"
        firstByte = if (size == 56) PAYMENT_STAKE_ADDRESS_KEY_KEY_PREFIX_MAINNET else PAYMENT_ADDRESS_PREFIX_MAINNET
    } else {
        prefix = "addr_test"
        firstByte = if (size == 56) PAYMENT_STAKE_ADDRESS_KEY_KEY_PREFIX_TESTNET else PAYMENT_ADDRESS_PREFIX_TESTNET
    }
    return Bech32.encode(prefix, byteArrayOf(firstByte) + this)
}

fun String.paymentAddressToHash(): ByteArray {
    require(startsWith("addr")) { "Invalid payment address: $this" }
    return Bech32.decode(this).bytes.run { copyOfRange(1, size) }
}

fun String.extractStakeKeyHex(): String? {
    val bytes = Bech32.decode(this).bytes
    return when {
        startsWith("stake") && bytes.size == 29 -> bytes.toHexString(fromIndex = 1)

        startsWith("addr") && bytes.size == 57 -> bytes.toHexString(fromIndex = 29)

        else -> null
    }
}

/**
 * Computes the Asset fingerprint
 * Based on: https://cips.cardano.org/cip/CIP-14
 */
fun assetFingerprintOf(
    policyId: String,
    assetName: String
): String =
    Bech32.encode(
        prefix = "asset",
        bytes = Blake2b.hash160(policyId.hexToByteArray() + assetName.hexToByteArray())
    )

/**
 * Gives the Asset URL at pool.pm for mainnet, or at preprod.cardanoscan.io for testnet.
 */
fun assetUrlOf(
    isMainnet: Boolean,
    policyId: String,
    assetName: String
): String =
    if (isMainnet) {
        "https://pool.pm/${assetFingerprintOf(policyId, assetName)}"
    } else {
        "https://preprod.cardanoscan.io/token/$policyId$assetName"
    }

fun String.toRequiredSigner(): ByteArray = Bech32.decode(this).bytes.copyOfRange(1, 29)
