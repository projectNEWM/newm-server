package io.newm.txbuilder.ktx

import com.google.iot.cbor.CborArray
import com.google.iot.cbor.CborByteString
import com.google.iot.cbor.CborInteger
import com.google.iot.cbor.CborObject
import io.newm.chain.grpc.NativeScript

/**
 * Convert NativeScript into cbor so it can be included in a transaction.
 */
fun NativeScript.toCborObject(): CborObject {
    return when (this.nativeScriptWrapperCase) {
        NativeScript.NativeScriptWrapperCase.NATIVE_SCRIPT_PUB_KEY -> {
            CborArray.create(
                listOf(
                    NATIVE_SCRIPT_KEY_PUB_KEY_INDEX,
                    CborByteString.create(this.nativeScriptPubKey.addrKeyHash.toByteArray()),
                )
            )
        }

        NativeScript.NativeScriptWrapperCase.NATIVE_SCRIPT_ALL -> {
            CborArray.create(
                listOf(
                    NATIVE_SCRIPT_ALL_INDEX,
                    CborArray.create(
                        this.nativeScriptAll.allList.map { it.toCborObject() }
                    ),
                )
            )
        }

        NativeScript.NativeScriptWrapperCase.NATIVE_SCRIPT_ANY -> {
            CborArray.create(
                listOf(
                    NATIVE_SCRIPT_ANY_INDEX,
                    CborArray.create(
                        this.nativeScriptAny.anyList.map { it.toCborObject() }
                    ),
                )
            )
        }

        NativeScript.NativeScriptWrapperCase.NATIVE_SCRIPT_N_OF_K -> {
            CborArray.create(
                listOf(
                    NATIVE_SCRIPT_N_OF_K_INDEX,
                    CborInteger.create(this.nativeScriptNOfK.n),
                    CborArray.create(
                        this.nativeScriptNOfK.kList.map { it.toCborObject() }
                    ),
                )
            )
        }

        NativeScript.NativeScriptWrapperCase.NATIVE_SCRIPT_INVALID_BEFORE -> {
            CborArray.create(
                listOf(
                    NATIVE_SCRIPT_INVALID_BEFORE_INDEX,
                    CborInteger.create(this.nativeScriptInvalidBefore.absoluteSlot)
                )
            )
        }

        NativeScript.NativeScriptWrapperCase.NATIVE_SCRIPT_INVALID_HEREAFTER -> {
            CborArray.create(
                listOf(
                    NATIVE_SCRIPT_INVALID_HEREAFTER_INDEX,
                    CborInteger.create(this.nativeScriptInvalidHereafter.absoluteSlot)
                )
            )
        }

        else -> throw IllegalArgumentException("NativeScript must have a body!")
    }
}

private val NATIVE_SCRIPT_KEY_PUB_KEY_INDEX by lazy { CborInteger.create(0) }
private val NATIVE_SCRIPT_ALL_INDEX by lazy { CborInteger.create(1) }
private val NATIVE_SCRIPT_ANY_INDEX by lazy { CborInteger.create(2) }
private val NATIVE_SCRIPT_N_OF_K_INDEX by lazy { CborInteger.create(3) }
private val NATIVE_SCRIPT_INVALID_BEFORE_INDEX by lazy { CborInteger.create(4) }
private val NATIVE_SCRIPT_INVALID_HEREAFTER_INDEX by lazy { CborInteger.create(5) }
