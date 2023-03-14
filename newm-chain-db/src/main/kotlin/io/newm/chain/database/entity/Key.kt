package io.newm.chain.database.entity

import com.google.iot.cbor.CborArray
import com.google.iot.cbor.CborByteString
import com.google.iot.cbor.CborInteger
import com.google.iot.cbor.CborReader
import io.newm.chain.config.Config
import io.newm.chain.model.CliKey
import io.newm.chain.model.CliKeyPair
import io.newm.chain.util.Bech32
import io.newm.chain.util.Blake2b
import io.newm.chain.util.Constants.PAYMENT_ADDRESS_PREFIX_MAINNET
import io.newm.chain.util.Constants.PAYMENT_ADDRESS_PREFIX_TESTNET
import io.newm.chain.util.Constants.PAYMENT_ADDRESS_SCRIPT_PREFIX_MAINNET
import io.newm.chain.util.Constants.PAYMENT_ADDRESS_SCRIPT_PREFIX_TESTNET
import io.newm.chain.util.hexToByteArray
import io.newm.chain.util.toHexString
import org.bouncycastle.crypto.generators.Ed25519KeyPairGenerator
import org.bouncycastle.crypto.params.Ed25519KeyGenerationParameters
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters
import java.security.SecureRandom
import kotlin.concurrent.getOrSet

data class Key(
    val id: Long? = null,
    val skey: ByteArray,
    val vkey: String,
    val address: String,
    val script: String?,
    val scriptAddress: String?,
    val created: Long,
) {

    companion object {

        private val scriptPrefixTag = ByteArray(1) { 0x00 }

        private val ed25519KeyPairGenerator = ThreadLocal<Ed25519KeyPairGenerator>()

        /**
         * Generate a new random keypair
         */
        fun create(): Key {
            val keyPairGenerator = ed25519KeyPairGenerator.getOrSet {
                Ed25519KeyPairGenerator().also {
                    it.init(Ed25519KeyGenerationParameters(SecureRandom.getInstanceStrong()))
                }
            }
            val ed25519KeyPair = keyPairGenerator.generateKeyPair()
            val vkey = (ed25519KeyPair.public as Ed25519PublicKeyParameters).encoded
            val skey = (ed25519KeyPair.private as Ed25519PrivateKeyParameters).encoded
            return Key(
                skey = skey,
                vkey = vkey.toHexString(),
                address = vkeyToAddress(vkey),
                script = null,
                scriptAddress = null,
                created = System.currentTimeMillis(),
            )
        }

        fun createMultiSig(otherKey: Key): Key {
            val key = create()
            val script = createMultiSigAllOfScript(otherKey, key)
            val address = scriptToAddress(script)
            return key.copy(script = script.toHexString(), scriptAddress = address)
        }

        fun createFromCliKeys(skey: CliKey, vkey: CliKey): Key {
            val skeyBytes = (
                CborReader.createFromByteArray(skey.cborHex.hexToByteArray(), 0, 1)
                    .readDataItem() as CborByteString
                ).byteArrayValue()[0]
            val vkeyBytes = (
                CborReader.createFromByteArray(vkey.cborHex.hexToByteArray(), 0, 1)
                    .readDataItem() as CborByteString
                ).byteArrayValue()[0]

            return Key(
                skey = skeyBytes,
                vkey = vkeyBytes.toHexString(),
                address = vkeyToAddress(vkeyBytes),
                script = null,
                scriptAddress = null,
                created = System.currentTimeMillis(),
            )
        }

        private fun createMultiSigAllOfScript(vararg keys: Key): ByteArray {
            return CborArray.create().apply {
                add(CborInteger.create(1L)) // all
                add(
                    CborArray.create().apply {
                        keys.forEach { key ->
                            val keyHash = Blake2b.hash224(key.vkey.hexToByteArray())
                            add(
                                CborArray.create().apply {
                                    add(CborInteger.create(0L)) // sig
                                    add(CborByteString.create(keyHash)) // keyHash
                                }
                            )
                        }
                    }
                )
            }.toCborByteArray()
        }

        private fun vkeyToAddress(vkey: ByteArray): String {
            val hash = Blake2b.hash224(vkey)
            return if (Config.isMainnet) {
                Bech32.encode("addr", ByteArray(1) { PAYMENT_ADDRESS_PREFIX_MAINNET } + hash)
            } else {
                Bech32.encode("addr_test", ByteArray(1) { PAYMENT_ADDRESS_PREFIX_TESTNET } + hash)
            }
        }

        private fun scriptToAddress(script: ByteArray): String {
            val hash = Blake2b.hash224(scriptPrefixTag + script)
            return if (Config.isMainnet) {
                Bech32.encode("addr", ByteArray(1) { PAYMENT_ADDRESS_SCRIPT_PREFIX_MAINNET } + hash)
            } else {
                Bech32.encode("addr_test", ByteArray(1) { PAYMENT_ADDRESS_SCRIPT_PREFIX_TESTNET } + hash)
            }
        }
    }

    fun toCliKeys(): CliKeyPair {
        val skeyCliKey = CliKey(
            type = "PaymentSigningKeyShelley_ed25519",
            description = "Payment Signing Key",
            cborHex = CborByteString.create(skey).toCborByteArray().toHexString(),
        )
        val vkeyCliKey = CliKey(
            type = "PaymentVerificationKeyShelley_ed25519",
            description = "Payment Verification Key",
            cborHex = CborByteString.create(vkey.hexToByteArray()).toCborByteArray().toHexString(),
        )
        return CliKeyPair(skeyCliKey, vkeyCliKey)
    }

    fun addMultiSig(otherKey: Key): Key {
        require(this.script == null)
        val script = createMultiSigAllOfScript(otherKey, this)
        val address = scriptToAddress(script)
        return this.copy(script = script.toHexString(), scriptAddress = address)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Key

        if (id != other.id) return false
        if (!skey.contentEquals(other.skey)) return false
        if (vkey != other.vkey) return false
        if (address != other.address) return false
        if (script != other.script) return false
        if (scriptAddress != other.scriptAddress) return false
        if (created != other.created) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id?.hashCode() ?: 0
        result = 31 * result + skey.contentHashCode()
        result = 31 * result + vkey.hashCode()
        result = 31 * result + address.hashCode()
        result = 31 * result + script.hashCode()
        result = 31 * result + scriptAddress.hashCode()
        result = 31 * result + created.hashCode()
        return result
    }

    override fun toString(): String {
        return "Key(id=$id, skey=*******, vkey='$vkey', address='$address', scriptAddress='$scriptAddress', created=$created)"
    }
}
