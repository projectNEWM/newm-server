package io.newm.txbuilder.ktx

import com.google.iot.cbor.CborArray
import com.google.iot.cbor.CborByteString
import com.google.iot.cbor.CborInteger
import com.google.iot.cbor.CborMap
import com.google.iot.cbor.CborObject
import com.google.iot.cbor.CborTag
import io.newm.chain.grpc.NativeAsset
import io.newm.chain.grpc.OutputUtxo
import io.newm.chain.grpc.Utxo
import io.newm.chain.util.Bech32
import io.newm.chain.util.hexToByteArray
import io.newm.kogmios.protocols.model.CardanoEra
import io.newm.txbuilder.TransactionBuilder
import java.math.BigInteger

/**
 * Convert input utxos into cbor so it can be included in a transaction.
 */
fun Set<Utxo>.toCborObject(cardanoEra: CardanoEra): CborObject? {
    if (isEmpty()) {
        return null
    }
    return CborArray.create(
        map { sourceUtxo ->
            CborArray.create(
                listOf(
                    CborByteString.create(sourceUtxo.hash.hexToByteArray()),
                    CborInteger.create(sourceUtxo.ix),
                ),
                when (cardanoEra) {
                    CardanoEra.CONWAY -> 258
                    else -> CborTag.UNTAGGED
                }
            )
        }
    )
}

fun List<Utxo>.selectUtxos(
    @Suppress("UNUSED_PARAMETER")
    requiredLovelace: BigInteger,
    @Suppress("UNUSED_PARAMETER")
    requiredNativeAssets: List<NativeAsset> = emptyList()
): List<Utxo> {
    // FIXME: implement
    return emptyList()
}

/**
 * calculate the minUtxo based on bytes
 */
fun OutputUtxo.withMinUtxo(utxoCostPerByte: Long): OutputUtxo {
    // The constant overhead of 160 bytes accounts for the transaction input
    // and the entry in the UTxO map data structure (20 words * 8 bytes). CIP-0055
    val overheadConstant = 160L
    val cborSerializedLength =
        this.toBuilder().setLovelace(
            // dummy value of 5 ada to get the byte length correct
            "5000000"
        ).build().toCborObject().toCborByteArray().size
    val minUtxoLovelace = (overheadConstant + cborSerializedLength) * utxoCostPerByte

    val userDefinedLovelace = this.lovelace.takeUnless { it.isNullOrBlank() }?.toLong() ?: 0L
    return if (minUtxoLovelace > userDefinedLovelace) {
        this.toBuilder().setLovelace(minUtxoLovelace.toString()).build()
    } else {
        this
    }
}

/**
 * Convert an OutputUtxo to cbor for including in a transaction.
 */
fun OutputUtxo.toCborObject(): CborObject {
    return CborMap.create(
        mapOf(
            TransactionBuilder.UTXO_OUTPUT_KEY_ADDRESS to CborByteString.create(Bech32.decode(address).bytes),
            TransactionBuilder.UTXO_OUTPUT_KEY_AMOUNT to
                if (nativeAssetsList?.isNotEmpty() == true) {
                    CborArray.create(
                        listOf(
                            CborInteger.create(lovelace.toBigInteger()),
                            nativeAssetsList.toNativeAssetCborMap(),
                        )
                    )
                } else {
                    CborInteger.create(lovelace.toBigInteger())
                },
            TransactionBuilder.UTXO_OUTPUT_KEY_DATUM to
                if (datumHash.isNotBlank()) {
                    CborArray.create(
                        listOf(
                            TransactionBuilder.DATUM_KEY_HASH,
                            CborByteString.create(datumHash.hexToByteArray()),
                        )
                    )
                } else if (datum.isNotBlank()) {
                    val datumBytes = datum.hexToByteArray()
                    CborArray.create(
                        listOf(
                            TransactionBuilder.DATUM_KEY_INLINE,
                            CborByteString.create(datumBytes, 0, datumBytes.size, TransactionBuilder.INLINE_DATUM_TAG),
                        )
                    )
                } else {
                    null
                },
            TransactionBuilder.UTXO_OUTPUT_KEY_SCRIPTREF to
                if (scriptRef.isNotBlank()) {
                    val scriptBytes = scriptRef.hexToByteArray()
                    CborByteString.create(scriptBytes, 0, scriptBytes.size, TransactionBuilder.SCRIPT_REF_TAG)
                } else {
                    null
                }
        ).filterValues { it != null }
    )
}
