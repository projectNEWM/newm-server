package io.newm.chain.util

import com.google.iot.cbor.CborInteger
import com.google.iot.cbor.CborTextString
import java.math.BigInteger

object Constants {
    const val STAKE_ADDRESS_KEY_PREFIX_TESTNET = 0xe0.toByte()
    const val STAKE_ADDRESS_SCRIPT_PREFIX_TESTNET = 0xf0.toByte()
    const val PAYMENT_STAKE_ADDRESS_KEY_KEY_PREFIX_TESTNET = 0x00.toByte()
    const val PAYMENT_STAKE_ADDRESS_SCRIPT_KEY_PREFIX_TESTNET = 0x10.toByte()
    const val PAYMENT_STAKE_ADDRESS_KEY_SCRIPT_PREFIX_TESTNET = 0x20.toByte()
    const val PAYMENT_STAKE_ADDRESS_SCRIPT_SCRIPT_PREFIX_TESTNET = 0x30.toByte()
    const val PAYMENT_STAKE_ADDRESS_KEY_POINTER_PREFIX_TESTNET = 0x40.toByte()
    const val PAYMENT_STAKE_ADDRESS_SCRIPT_POINTER_PREFIX_TESTNET = 0x50.toByte()
    const val PAYMENT_ADDRESS_PREFIX_TESTNET = 0x60.toByte()
    const val PAYMENT_ADDRESS_SCRIPT_PREFIX_TESTNET = 0x70.toByte()

    val PAYMENT_ADDRESS_PREFIXES_TESTNET = listOf(
        PAYMENT_STAKE_ADDRESS_KEY_KEY_PREFIX_TESTNET,
        PAYMENT_STAKE_ADDRESS_SCRIPT_KEY_PREFIX_TESTNET,
        PAYMENT_STAKE_ADDRESS_KEY_SCRIPT_PREFIX_TESTNET,
        PAYMENT_STAKE_ADDRESS_SCRIPT_SCRIPT_PREFIX_TESTNET,
        PAYMENT_STAKE_ADDRESS_KEY_POINTER_PREFIX_TESTNET,
        PAYMENT_STAKE_ADDRESS_SCRIPT_POINTER_PREFIX_TESTNET,
        PAYMENT_ADDRESS_PREFIX_TESTNET,
        PAYMENT_ADDRESS_SCRIPT_PREFIX_TESTNET,
    )

    const val STAKE_ADDRESS_KEY_PREFIX_MAINNET = 0xe1.toByte()
    const val STAKE_ADDRESS_SCRIPT_PREFIX_MAINNET = 0xf1.toByte()
    const val PAYMENT_STAKE_ADDRESS_KEY_KEY_PREFIX_MAINNET = 0x01.toByte()
    const val PAYMENT_STAKE_ADDRESS_SCRIPT_KEY_PREFIX_MAINNET = 0x11.toByte()
    const val PAYMENT_STAKE_ADDRESS_KEY_SCRIPT_PREFIX_MAINNET = 0x21.toByte()
    const val PAYMENT_STAKE_ADDRESS_SCRIPT_SCRIPT_PREFIX_MAINNET = 0x31.toByte()
    const val PAYMENT_STAKE_ADDRESS_KEY_POINTER_PREFIX_MAINNET = 0x41.toByte()
    const val PAYMENT_STAKE_ADDRESS_SCRIPT_POINTER_PREFIX_MAINNET = 0x51.toByte()
    const val PAYMENT_ADDRESS_PREFIX_MAINNET = 0x61.toByte()
    const val PAYMENT_ADDRESS_SCRIPT_PREFIX_MAINNET = 0x71.toByte()

    val PAYMENT_ADDRESS_PREFIXES_MAINNET = listOf(
        PAYMENT_STAKE_ADDRESS_KEY_KEY_PREFIX_MAINNET,
        PAYMENT_STAKE_ADDRESS_SCRIPT_KEY_PREFIX_MAINNET,
        PAYMENT_STAKE_ADDRESS_KEY_SCRIPT_PREFIX_MAINNET,
        PAYMENT_STAKE_ADDRESS_SCRIPT_SCRIPT_PREFIX_MAINNET,
        PAYMENT_STAKE_ADDRESS_KEY_POINTER_PREFIX_MAINNET,
        PAYMENT_STAKE_ADDRESS_SCRIPT_POINTER_PREFIX_MAINNET,
        PAYMENT_ADDRESS_PREFIX_MAINNET,
        PAYMENT_ADDRESS_SCRIPT_PREFIX_MAINNET,
    )

    const val BYRON_ADDRESS_PREFIX = 0x82.toByte()
    const val RECEIVE_ADDRESS_PATTERN = "^addr(_test)?1(?=[qpzry9x8gf2tvdw0s3jn54khce6mua7l]+)(?:.{98})$"
    const val STAKE_ADDRESS_PATTERN = "^stake(_test)?1(?=[qpzry9x8gf2tvdw0s3jn54khce6mua7l]+)(?:.{53})$"
    const val DUMMY_SCRIPT_ENTERPRISE_ADDRESS = "addr1wyqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqql9r5vw"
    const val DUMMY_STAKE_ADDRESS =
        "addr1qyqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqv2t5am"
    const val DUMMY_TOKEN_POLICY_ID = "00000000000000000000000000000000000000000000000000000000"
    const val DUMMY_MAX_TOKEN_NAME = "0000000000000000000000000000000000000000000000000000000000000000"

    val receiveAddressRegex = Regex(RECEIVE_ADDRESS_PATTERN)
    val stakeAddressRegex = Regex(STAKE_ADDRESS_PATTERN)

    val NONCE_VRF_HEADER = ByteArray(1) { 0x4E.toByte() } // 'N'
    val LEADER_VRF_HEADER = ByteArray(1) { 0x4C.toByte() } // 'L'

    val UTXO_ADDRESS_INDEX: CborInteger = CborInteger.create(0)
    val UTXO_AMOUNT_INDEX: CborInteger = CborInteger.create(1)
    val UTXO_DATUM_INDEX: CborInteger = CborInteger.create(2)
    val UTXO_SCRIPT_REF_INDEX: CborInteger = CborInteger.create(3)

    val TX_SPENT_UTXOS_INDEX: CborInteger = CborInteger.create(0)
    val TX_DEST_UTXOS_INDEX: CborInteger = CborInteger.create(1) // destination addresses are at index 1
    val TX_CERTS_INDEX: CborInteger = CborInteger.create(4)
    val TX_MINTS_INDEX: CborInteger = CborInteger.create(9)
    val TX_COLLAT_UTXOS_INDEX: CborInteger = CborInteger.create(13)

    val STAKE_REGISTRATION_CERT_INDEX: BigInteger = BigInteger.valueOf(0L)
    val STAKE_DEREGISTRATION_CERT_INDEX: BigInteger = BigInteger.valueOf(1L)
    val STAKE_DELEGATION_CERT_INDEX: BigInteger = BigInteger.valueOf(2L)
    val POOL_REGISTRATION_CERT_INDEX: BigInteger = BigInteger.valueOf(3L)

    const val NFT_METADATA_KEY = "721"
    val NFT_METADATA_KEY_NAME: CborTextString = CborTextString.create("name")
    val NFT_METADATA_KEY_IMAGE: CborTextString = CborTextString.create("image")
    val NFT_METADATA_KEY_DESC: CborTextString = CborTextString.create("description")
    val FT_METADATA_KEY_DECIMALS: CborTextString = CborTextString.create("decimals")
    val FT_METADATA_KEY_DESC: CborTextString = CborTextString.create("desc")

    const val BYRON_TO_SHELLEY_EPOCHS_MAINNET = 208L
    const val BYRON_TO_SHELLEY_EPOCHS_GUILD = 1L
    const val BYRON_TO_SHELLEY_EPOCHS_PREVIEW = 0L
    const val BYRON_TO_SHELLEY_EPOCHS_PREPROD = 4L

    const val NETWORK_MAGIC_MAINNET = 764824073L
    const val NETWORK_MAGIC_GUILD = 141L
    const val NETWORK_MAGIC_PREVIEW = 2L
    const val NETWORK_MAGIC_PREPROD = 1L

    const val ROLE_PAYMENT = 0u
    const val ROLE_CHANGE = 1u
    const val ROLE_STAKING = 2u

    // FIXME: Remove unused stuff
}
