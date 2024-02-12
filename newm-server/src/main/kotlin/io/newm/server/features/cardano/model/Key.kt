package io.newm.server.features.cardano.model

import com.firehose.model.CliKey
import com.firehose.model.CliKeyPair
import com.google.iot.cbor.CborArray
import com.google.iot.cbor.CborByteString
import com.google.iot.cbor.CborInteger
import com.google.iot.cbor.CborReader
import com.google.protobuf.ByteString
import io.newm.chain.grpc.SigningKey
import io.newm.chain.grpc.signingKey
import io.newm.chain.util.Bech32
import io.newm.chain.util.Blake2b
import io.newm.chain.util.Constants.PAYMENT_ADDRESS_PREFIX_MAINNET
import io.newm.chain.util.Constants.PAYMENT_ADDRESS_PREFIX_TESTNET
import io.newm.chain.util.Constants.PAYMENT_ADDRESS_SCRIPT_PREFIX_MAINNET
import io.newm.chain.util.Constants.PAYMENT_ADDRESS_SCRIPT_PREFIX_TESTNET
import io.newm.chain.util.hexToByteArray
import io.newm.chain.util.toHexString
import io.newm.server.features.cardano.repo.CardanoRepository
import io.newm.shared.koin.inject
import io.newm.shared.serialization.LocalDateTimeSerializer
import io.newm.shared.serialization.UUIDSerializer
import kotlinx.serialization.Serializable
import org.bouncycastle.crypto.generators.Ed25519KeyPairGenerator
import org.bouncycastle.crypto.params.Ed25519KeyGenerationParameters
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters
import java.security.SecureRandom
import java.time.LocalDateTime
import java.util.UUID
import kotlin.concurrent.getOrSet

@Serializable
data class Key(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID? = null,
    @Serializable(with = LocalDateTimeSerializer::class)
    val createdAt: LocalDateTime? = null,
    val skey: ByteArray,
    val vkey: ByteArray,
    val address: String,
    val script: String?,
    val scriptAddress: String?,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Key) return false

        if (id != other.id) return false
        if (createdAt != other.createdAt) return false
        if (!skey.contentEquals(other.skey)) return false
        if (!vkey.contentEquals(other.vkey)) return false
        if (address != other.address) return false
        if (script != other.script) return false
        if (scriptAddress != other.scriptAddress) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id?.hashCode() ?: 0
        result = 31 * result + createdAt.hashCode()
        result = 31 * result + skey.contentHashCode()
        result = 31 * result + vkey.contentHashCode()
        result = 31 * result + address.hashCode()
        result = 31 * result + (script?.hashCode() ?: 0)
        result = 31 * result + (scriptAddress?.hashCode() ?: 0)
        return result
    }

    fun requiredSigner(): ByteArray {
        return Bech32.decode(address).bytes.copyOfRange(1, 29)
    }

    fun toCliKeyPair(name: String): CliKeyPair {
        return CliKeyPair(
            name = name,
            skey =
                CliKey(
                    type = "PaymentSigningKeyShelley_ed25519",
                    description = "Payment Signing Key",
                    cborHex = CborByteString.create(skey).toCborByteArray().toHexString(),
                ),
            vkey =
                CliKey(
                    type = "PaymentVerificationKeyShelley_ed25519",
                    description = "Payment Verification Key",
                    cborHex = CborByteString.create(vkey).toCborByteArray().toHexString(),
                )
        )
    }

    fun toSigningKey(): SigningKey {
        return signingKey {
            this.skey = ByteString.copyFrom(this@Key.skey)
            this.vkey = ByteString.copyFrom(this@Key.vkey)
        }
    }

    companion object {
        private val cardanoRepository: CardanoRepository by inject()
        private val scriptPrefixTag = ByteArray(1) { 0x00 }
        private val ed25519KeyPairGenerator = ThreadLocal<Ed25519KeyPairGenerator>()

        /**
         * Generate a new random keypair
         */
        suspend fun generateNew(): Key {
            val keyPairGenerator =
                ed25519KeyPairGenerator.getOrSet {
                    Ed25519KeyPairGenerator().also {
                        it.init(Ed25519KeyGenerationParameters(SecureRandom.getInstanceStrong()))
                    }
                }
            val ed25519KeyPair = keyPairGenerator.generateKeyPair()
            val vkey = (ed25519KeyPair.public as Ed25519PublicKeyParameters).encoded
            val skey = (ed25519KeyPair.private as Ed25519PrivateKeyParameters).encoded
            return Key(
                skey = skey,
                vkey = vkey,
                address = vkeyToAddress(vkey),
                script = null,
                scriptAddress = null,
                createdAt = LocalDateTime.now(),
            )
        }

        suspend fun createFromCliKeys(keyPair: CliKeyPair): Key {
            val skeyBytes =
                keyPair.skey?.let { skey ->
                    (
                        CborReader.createFromByteArray(skey.cborHex.hexToByteArray(), 0, 1)
                            .readDataItem() as CborByteString
                    ).byteArrayValue()[0]
                } ?: ByteArray(0)
            val vkeyBytes =
                (
                    CborReader.createFromByteArray(keyPair.vkey.cborHex.hexToByteArray(), 0, 1)
                        .readDataItem() as CborByteString
                ).byteArrayValue()[0]

            return Key(
                skey = skeyBytes,
                vkey = vkeyBytes,
                address = vkeyToAddress(vkeyBytes),
                script = null,
                scriptAddress = null,
                createdAt = LocalDateTime.now(),
            )
        }

        suspend fun generateNewMultiSig(otherKey: Key): Key {
            val key = generateNew()
            val script = createMultiSigAllOfScript(otherKey, key)
            val address = scriptToAddress(script)
            return key.copy(script = script.toHexString(), scriptAddress = address)
        }

        private fun createMultiSigAllOfScript(vararg keys: Key): ByteArray {
            return CborArray.create(
                listOf(
                    // allOf
                    CborInteger.create(1L),
                    CborArray.create(
                        keys.map { key ->
                            val keyHash = Blake2b.hash224(key.vkey)
                            CborArray.create(
                                listOf(
                                    // sig
                                    CborInteger.create(0L),
                                    CborByteString.create(keyHash)
                                )
                            )
                        }
                    )
                )
            ).toCborByteArray()
        }

        private suspend fun vkeyToAddress(vkey: ByteArray): String {
            val hash = Blake2b.hash224(vkey)

            return if (cardanoRepository.isMainnet()) {
                Bech32.encode("addr", ByteArray(1) { PAYMENT_ADDRESS_PREFIX_MAINNET } + hash)
            } else {
                Bech32.encode("addr_test", ByteArray(1) { PAYMENT_ADDRESS_PREFIX_TESTNET } + hash)
            }
        }

        private suspend fun scriptToAddress(script: ByteArray): String {
            val hash = Blake2b.hash224(scriptPrefixTag + script)
            return if (cardanoRepository.isMainnet()) {
                Bech32.encode("addr", ByteArray(1) { PAYMENT_ADDRESS_SCRIPT_PREFIX_MAINNET } + hash)
            } else {
                Bech32.encode("addr_test", ByteArray(1) { PAYMENT_ADDRESS_SCRIPT_PREFIX_TESTNET } + hash)
            }
        }
    }

    suspend fun addMultiSig(otherKey: Key): Key {
        require(this.script == null)
        val script = createMultiSigAllOfScript(otherKey, this)
        val address = scriptToAddress(script)
        return this.copy(script = script.toHexString(), scriptAddress = address)
    }

    /**
     * Prevent skey from being leaked to logs.
     */
    override fun toString(): String {
        return "Key(id=$id, skey=*******, vkey='${vkey.toHexString()}', address='$address', scriptAddress='$scriptAddress', createdAt=$createdAt)"
    }
}
