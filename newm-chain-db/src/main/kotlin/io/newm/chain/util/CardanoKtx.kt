package io.newm.chain.util

import com.google.iot.cbor.CborArray
import com.google.iot.cbor.CborByteString
import io.newm.chain.config.Config
import io.newm.chain.database.entity.ChainBlock
import io.newm.chain.database.entity.LedgerAssetMetadata
import io.newm.chain.database.entity.PaymentStakeAddress
import io.newm.chain.database.entity.RawTransaction
import io.newm.chain.database.entity.StakeRegistration
import io.newm.chain.model.CreatedUtxo
import io.newm.chain.model.NativeAsset
import io.newm.chain.model.NativeAssetMetadata
import io.newm.chain.model.SpentUtxo
import io.newm.chain.util.Constants.LEADER_VRF_HEADER
import io.newm.chain.util.Constants.NFT_METADATA_KEY
import io.newm.chain.util.Constants.NONCE_VRF_HEADER
import io.newm.chain.util.Constants.STAKE_ADDRESS_KEY_PREFIX_MAINNET
import io.newm.chain.util.Constants.STAKE_ADDRESS_KEY_PREFIX_TESTNET
import io.newm.chain.util.Constants.receiveAddressRegex
import io.newm.kogmios.protocols.model.Block
import io.newm.kogmios.protocols.model.BlockAllegra
import io.newm.kogmios.protocols.model.BlockAlonzo
import io.newm.kogmios.protocols.model.BlockBabbage
import io.newm.kogmios.protocols.model.BlockMary
import io.newm.kogmios.protocols.model.BlockShelley
import io.newm.kogmios.protocols.model.Certificate
import io.newm.kogmios.protocols.model.DelegationCertificate
import io.newm.kogmios.protocols.model.MetadataBytes
import io.newm.kogmios.protocols.model.MetadataInteger
import io.newm.kogmios.protocols.model.MetadataList
import io.newm.kogmios.protocols.model.MetadataMap
import io.newm.kogmios.protocols.model.MetadataString
import io.newm.kogmios.protocols.model.MetadataValue
import io.newm.kogmios.protocols.model.StakeKeyRegistrationCertificate
import io.newm.kogmios.protocols.model.UtxoOutput
import org.apache.commons.codec.binary.Hex
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.math.BigInteger
import java.util.*

private val log by lazy { LoggerFactory.getLogger("CardanoKtx") }

fun CborArray.elementToBigInteger(index: Int): BigInteger {
    val obj = elementAt(index).toJavaObject()
    return (obj as? BigInteger) ?: (obj as? Long)?.toBigInteger() ?: (obj as Int).toBigInteger()
}

fun CborArray.elementToInt(index: Int): Int {
    val obj = elementAt(index).toJavaObject()
    return (obj as? Int) ?: (obj as? Long)?.toInt() ?: (obj as BigInteger).toInt()
}

fun CborArray.elementToByteArray(index: Int): ByteArray {
    return (elementAt(index) as CborByteString).byteArrayValue()[0]
}

fun CborArray.elementToHexString(index: Int): String {
    return elementToByteArray(index).toHexString()
}

fun ByteArray.toHexString(): String {
    return Hex.encodeHexString(this, true)
}

fun String.hexToByteArray(): ByteArray {
    return Hex.decodeHex(this)
}

fun String.b64ToByteArray(): ByteArray {
    return Base64.getDecoder().decode(this)
}

fun ByteArray.toB64String(): String {
    return Base64.getEncoder().encodeToString(this)
}

private val hexRegex = Regex("([a-f\\d]{2})+")
fun String.assetNameToHexString(): String {
    return if (hexRegex.matches(this)) {
        // it's already hex string
        this
    } else {
        // convert to hex string
        this.toByteArray().toHexString()
    }
}

fun Int.toHexString(): String = Integer.toHexString(this)
fun Long.toHexString(): String = java.lang.Long.toHexString(this)

fun BigInteger.toAda(): BigDecimal = this.toBigDecimal(6)
fun Long.lovelaceToAdaString(): String = "₳${this.toBigDecimal().movePointLeft(6)}"

fun String.toHexPoolId(): String {
    return try {
        Bech32.decode(this).bytes.toHexString()
    } catch (e: Throwable) {
        this
    }
}

fun String.extractStakeAddress(): String {
    val decodedReceiveAddress = Bech32.decode(this)

    // check the length of the address
    require(decodedReceiveAddress.bytes.size == 57) { "Not enough bytes in stake address for: $this" }

    // calculate the encoded stake address
    return if (Config.isMainnet) {
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

fun Block.toChainBlock() = ChainBlock(
    blockNumber = this.header.blockHeight,
    slotNumber = this.header.slot,
    hash = when (this) {
        is BlockShelley -> this.shelley.headerHash
        is BlockAllegra -> this.allegra.headerHash
        is BlockMary -> this.mary.headerHash
        is BlockAlonzo -> this.alonzo.headerHash
        is BlockBabbage -> this.babbage.headerHash
    },
    prevHash = this.header.prevHash,
    nodeVkey = this.header.issuerVk,
    nodeVrfVkey = this.header.issuerVrf.b64ToByteArray().toHexString(),
    blockVrf = this.header.vrfInput?.output ?: "",
    blockVrfProof = this.header.vrfInput?.proof ?: "",
    etaVrf0 = this.header.vrfInput?.output?.let {
        Blake2b.hash256(NONCE_VRF_HEADER + it.b64ToByteArray()).toHexString()
    } ?: this.header.nonce?.output?.b64ToByteArray()?.toHexString() ?: "",
    etaVrf1 = this.header.nonce?.proof?.b64ToByteArray()?.toHexString() ?: "",
    leaderVrf0 = this.header.vrfInput?.output?.let {
        Blake2b.hash256(LEADER_VRF_HEADER + it.b64ToByteArray()).toHexString()
    } ?: this.header.leaderValue?.output?.b64ToByteArray()?.toHexString() ?: "",
    leaderVrf1 = this.header.leaderValue?.proof?.b64ToByteArray()?.toHexString() ?: "",
    blockSize = this.header.blockSize.toInt(),
    blockBodyHash = this.header.blockHash,
    poolOpcert = this.header.opCert?.hotVk?.b64ToByteArray()?.toHexString() ?: "",
    sequenceNumber = this.header.opCert?.count ?: 0,
    kesPeriod = this.header.opCert?.kesPeriod ?: 0,
    sigmaSignature = this.header.opCert?.sigma?.b64ToByteArray()?.toHexString() ?: "",
    protocolMajorVersion = this.header.protocolVersion?.major ?: 0,
    protocolMinorVersion = this.header.protocolVersion?.minor ?: 0,
    transactionDestAddresses = this.toTransactionDestAddressSet(),
    stakeDestAddresses = this.toPaymentStakeAddressSet(),
)

private fun Block.toTransactionDestAddressSet() = when (this) {
    is BlockShelley -> this.shelley.body.flatMap { txShelley ->
        txShelley.body.outputs.map { utxoOutput ->
            utxoOutput.address
        }
    }.toSet()

    is BlockAllegra -> this.allegra.body.flatMap { txShelley ->
        txShelley.body.outputs.map { utxoOutput ->
            utxoOutput.address
        }
    }.toSet()

    is BlockMary -> this.mary.body.flatMap { txShelley ->
        txShelley.body.outputs.map { utxoOutput ->
            utxoOutput.address
        }
    }.toSet()

    is BlockAlonzo -> this.alonzo.body.flatMap { txShelley ->
        txShelley.body.outputs.map { utxoOutput ->
            utxoOutput.address
        }
    }.toSet()

    is BlockBabbage -> this.babbage.body.flatMap { txShelley ->
        txShelley.body.outputs.map { utxoOutput ->
            utxoOutput.address
        }
    }.toSet()
}

private fun Block.toPaymentStakeAddressSet(): Set<PaymentStakeAddress> = when (this) {
    is BlockShelley -> this.shelley.body.flatMap { txShelley ->
        txShelley.body.outputs.toPaymentStakeAddressList()
    }.toSet()

    is BlockAllegra -> this.allegra.body.flatMap { txAllegra ->
        txAllegra.body.outputs.toPaymentStakeAddressList()
    }.toSet()

    is BlockMary -> this.mary.body.flatMap { txMary ->
        txMary.body.outputs.toPaymentStakeAddressList()
    }.toSet()

    is BlockAlonzo -> this.alonzo.body.flatMap { txAlonzo ->
        txAlonzo.body.outputs.toPaymentStakeAddressList()
    }.toSet()

    is BlockBabbage -> this.babbage.body.flatMap { txBabbage ->
        txBabbage.body.outputs.toPaymentStakeAddressList()
    }.toSet()
}

private fun List<UtxoOutput>.toPaymentStakeAddressList() = this.mapNotNull { utxoOutput ->
    if (receiveAddressRegex.matches(utxoOutput.address)) {
        PaymentStakeAddress(
            receivingAddress = utxoOutput.address,
            stakeAddress = utxoOutput.address.extractStakeAddress(),
        )
    } else {
        null
    }
}

private fun UtxoOutput.extractStakeAddressOrNull() = if (receiveAddressRegex.matches(this.address)) {
    this.address.extractStakeAddress()
} else {
    null
}

fun Block.toCreatedUtxoSet(): Set<CreatedUtxo> = when (this) {
    is BlockShelley -> this.shelley.body.flatMap { txShelley ->
        txShelley.body.outputs.toCreatedUtxoList(txShelley.id)
    }.toSet()

    is BlockAllegra -> this.allegra.body.flatMap { txAllegra ->
        txAllegra.body.outputs.toCreatedUtxoList(txAllegra.id)
    }.toSet()

    is BlockMary -> this.mary.body.flatMap { txMary ->
        txMary.body.outputs.toCreatedUtxoList(txMary.id)
    }.toSet()

    is BlockAlonzo -> this.alonzo.body.flatMap { txAlonzo ->
        txAlonzo.body.outputs.toCreatedUtxoList(txAlonzo.id)
    }.toSet()

    is BlockBabbage -> this.babbage.body.flatMap { txBabbage ->
        txBabbage.body.outputs.toCreatedUtxoList(txBabbage.id)
    }.toSet()
}

private fun List<UtxoOutput>.toCreatedUtxoList(txId: String) = this.mapIndexed { index, utxoOutput ->
    CreatedUtxo(
        address = utxoOutput.address,
        addressType = utxoOutput.address.addressType(),
        stakeAddress = utxoOutput.extractStakeAddressOrNull(),
        hash = txId,
        ix = index.toLong(),
        lovelace = utxoOutput.value.coins,
        datumHash = utxoOutput.datumHash,
        datum = utxoOutput.datum,
        nativeAssets = utxoOutput.value.assets?.map { entry ->
            NativeAsset(
                policy = entry.key.substringBefore('.'),
                name = entry.key.substringAfter('.', missingDelimiterValue = ""),
                amount = entry.value,
            )
        } ?: emptyList(),
        cbor = null, // TODO: fix if we ever need it
    )
}

fun Block.toSpentUtxoSet(): Set<SpentUtxo> = when (this) {
    is BlockShelley -> shelley.body.flatMap { transaction ->
        transaction.body.inputs.map { utxoInput ->
            SpentUtxo(
                transactionSpent = transaction.id,
                hash = utxoInput.txId,
                ix = utxoInput.index,
            )
        }
    }.toSet()

    is BlockAllegra -> allegra.body.flatMap { transaction ->
        transaction.body.inputs.map { utxoInput ->
            SpentUtxo(
                transactionSpent = transaction.id,
                hash = utxoInput.txId,
                ix = utxoInput.index,
            )
        }
    }.toSet()

    is BlockMary -> mary.body.flatMap { transaction ->
        transaction.body.inputs.map { utxoInput ->
            SpentUtxo(
                transactionSpent = transaction.id,
                hash = utxoInput.txId,
                ix = utxoInput.index,
            )
        }
    }.toSet()

    is BlockAlonzo -> alonzo.body.flatMap { transaction ->
        if (transaction.inputSource == "inputs") {
            transaction.body.inputs
        } else {
            // tx failed. take the collaterals
            transaction.body.collaterals
        }.map { utxoInput ->
            SpentUtxo(
                transactionSpent = transaction.id,
                hash = utxoInput.txId,
                ix = utxoInput.index,
            )
        }
    }.toSet()

    is BlockBabbage -> babbage.body.flatMap { transaction ->
        if (transaction.inputSource == "inputs") {
            transaction.body.inputs
        } else {
            // tx failed. take the collaterals
            transaction.body.collaterals
        }.map { utxoInput ->
            SpentUtxo(
                transactionSpent = transaction.id,
                hash = utxoInput.txId,
                ix = utxoInput.index,
            )
        }
    }.toSet()
}

fun Block.toStakeRegistrationList(): List<StakeRegistration> = when (this) {
    is BlockShelley -> this.shelley.body.flatMapIndexed { txIndex, txShelley ->
        txShelley.body.certificates.mapIndexedNotNull { certIndex, certificate ->
            certificate.toStakeRegistrationOrNull(this.header.slot, txIndex, certIndex)
        }
    }

    is BlockAllegra -> this.allegra.body.flatMapIndexed { txIndex, txShelley ->
        txShelley.body.certificates.mapIndexedNotNull { certIndex, certificate ->
            certificate.toStakeRegistrationOrNull(this.header.slot, txIndex, certIndex)
        }
    }

    is BlockMary -> this.mary.body.flatMapIndexed { txIndex, txShelley ->
        txShelley.body.certificates.mapIndexedNotNull { certIndex, certificate ->
            certificate.toStakeRegistrationOrNull(this.header.slot, txIndex, certIndex)
        }
    }

    is BlockAlonzo -> this.alonzo.body.flatMapIndexed { txIndex, txShelley ->
        txShelley.body.certificates.mapIndexedNotNull { certIndex, certificate ->
            certificate.toStakeRegistrationOrNull(this.header.slot, txIndex, certIndex)
        }
    }

    is BlockBabbage -> this.babbage.body.flatMapIndexed { txIndex, txShelley ->
        txShelley.body.certificates.mapIndexedNotNull { certIndex, certificate ->
            certificate.toStakeRegistrationOrNull(this.header.slot, txIndex, certIndex)
        }
    }
}

private fun Certificate.toStakeRegistrationOrNull(slot: Long, txIndex: Int, certIndex: Int) = when (this) {
    is StakeKeyRegistrationCertificate -> {
        StakeRegistration(
            stakeAddress = this.stakeKeyRegistration,
            slot = slot,
            txIndex = txIndex,
            certIndex = certIndex,
        )
    }

    else -> null
}

fun Block.toStakeDelegationList(epoch: Long) = when (this) {
    is BlockShelley -> this.shelley.body.flatMap { txShelley ->
        txShelley.body.certificates.mapNotNull { certificate ->
            certificate.toStakeDelegationOrNull(this.header.blockHeight, epoch)
        }
    }

    is BlockAllegra -> this.allegra.body.flatMap { txShelley ->
        txShelley.body.certificates.mapNotNull { certificate ->
            certificate.toStakeDelegationOrNull(this.header.blockHeight, epoch)
        }
    }

    is BlockMary -> this.mary.body.flatMap { txShelley ->
        txShelley.body.certificates.mapNotNull { certificate ->
            certificate.toStakeDelegationOrNull(this.header.blockHeight, epoch)
        }
    }

    is BlockAlonzo -> this.alonzo.body.flatMap { txShelley ->
        txShelley.body.certificates.mapNotNull { certificate ->
            certificate.toStakeDelegationOrNull(this.header.blockHeight, epoch)
        }
    }

    is BlockBabbage -> this.babbage.body.flatMap { txShelley ->
        txShelley.body.certificates.mapNotNull { certificate ->
            certificate.toStakeDelegationOrNull(this.header.blockHeight, epoch)
        }
    }
}

fun Block.toAssetMetadataList(nativeAssetMetadataSet: Set<NativeAssetMetadata>): List<LedgerAssetMetadata> =
    when (this) {
        is BlockShelley -> emptyList()
        is BlockAllegra -> emptyList()
        is BlockMary -> mary.body.flatMap { transaction ->
            val assetMetadatas = transaction.metadata?.body?.blob?.get(NFT_METADATA_KEY) as? MetadataMap
            extractAssetMetadata(nativeAssetMetadataSet, assetMetadatas)
        }

        is BlockAlonzo -> alonzo.body.flatMap { transaction ->
            val assetMetadatas = transaction.metadata?.body?.blob?.get(NFT_METADATA_KEY) as? MetadataMap
            extractAssetMetadata(nativeAssetMetadataSet, assetMetadatas)
        }

        is BlockBabbage -> babbage.body.flatMap { transaction ->
            val assetMetadatas = transaction.metadata?.body?.blob?.get(NFT_METADATA_KEY) as? MetadataMap
            extractAssetMetadata(nativeAssetMetadataSet, assetMetadatas)
        }
    }

private fun extractAssetMetadata(
    nativeAssetMetadataSet: Set<NativeAssetMetadata>,
    assetMetadatas: MetadataMap?
): List<LedgerAssetMetadata> =
    assetMetadatas?.flatMap { (policyIdKey, assetMetadataValue) ->
        val policyId = when (policyIdKey) {
            is MetadataString -> policyIdKey.string
            is MetadataBytes -> policyIdKey.bytes.b64ToByteArray().toHexString()
            else -> null
        }
        policyId?.let {
            (assetMetadataValue as? MetadataMap)?.let { assetMetadataMap ->
                assetMetadataMap.flatMap { (nameMetadataValue, detailsMetadataValue) ->
                    val name = when (nameMetadataValue) {
                        is MetadataString -> nameMetadataValue.string
                        is MetadataBytes -> nameMetadataValue.bytes.b64ToByteArray().toHexString()
                        else -> null
                    }
                    name?.let {
                        val hexName = it.toByteArray().toHexString()
                        val nativeAssetMetadata = nativeAssetMetadataSet.find { asset ->
                            asset.assetPolicy == policyId &&
                                (asset.assetName == hexName || asset.assetName == it)
                        }

                        nativeAssetMetadata?.let {
                            (detailsMetadataValue as? MetadataMap)?.mapNotNull { (keyMetadataValue, valueMetadataValue) ->
                                buildAssetMetadata(it.id!!, keyMetadataValue, valueMetadataValue, 0)
                            }
                        } ?: run {
//                            log.warn("No 721 metadata found for $policyId.$hexName: nativeAssetMetadataSet: $nativeAssetMetadataSet, assetMetadatas: $assetMetadatas")
                            emptyList()
                        }
                    } ?: emptyList()
                }
            } ?: emptyList()
        } ?: emptyList()
    } ?: emptyList()

private fun buildAssetMetadata(
    assetId: Long,
    keyMetadataValue: MetadataValue,
    valueMetadataValue: MetadataValue,
    nestLevel: Int
): LedgerAssetMetadata? {
    val (key, keyType) = when (keyMetadataValue) {
        is MetadataString -> Pair(keyMetadataValue.string, "string")
        is MetadataBytes -> Pair(keyMetadataValue.bytes.b64ToByteArray().toHexString(), "bytestring")
        is MetadataInteger -> Pair(keyMetadataValue.int.toString(), "integer")
        else -> return null // ignore maps or lists for keys
    }

    val (value, valueType) = when (valueMetadataValue) {
        is MetadataString -> Pair(valueMetadataValue.string, "string")
        is MetadataBytes -> Pair(valueMetadataValue.bytes.b64ToByteArray().toHexString(), "bytestring")
        is MetadataInteger -> Pair(valueMetadataValue.int.toString(), "integer")
        is MetadataList -> Pair("", "array")
        is MetadataMap -> Pair("", "map")
    }

    return LedgerAssetMetadata(
        assetId = assetId,
        keyType = keyType,
        key = key,
        valueType = valueType,
        value = value,
        nestLevel = nestLevel,
        children = when (valueType) {
            "array" -> (valueMetadataValue as MetadataList).mapNotNull { subValueMetadata ->
                buildAssetMetadata(assetId, keyMetadataValue, subValueMetadata, nestLevel + 1)
            }

            "map" -> (valueMetadataValue as MetadataMap).mapNotNull { (subKeyMetadata, subValueMetadata) ->
                buildAssetMetadata(assetId, subKeyMetadata, subValueMetadata, nestLevel + 1)
            }

            else -> emptyList()
        }
    )
}

fun Block.toRawTransactionList() = when (this) {
    is BlockShelley -> {
        with(shelley) {
            val blockNumber = header.blockHeight
            val slotNumber = header.slot
            val blockSize = header.blockSize.toInt()
            val blockBodyHash = header.blockHash
            val protocolVersionMajor = header.protocolVersion?.major ?: 0
            val protocolVersionMinor = header.protocolVersion?.minor ?: 0

            body.map { transaction ->
                val tx = transaction.raw!!.b64ToByteArray()

                RawTransaction(
                    blockNumber = blockNumber,
                    slotNumber = slotNumber,
                    blockSize = blockSize,
                    blockBodyHash = blockBodyHash,
                    protocolVersionMajor = protocolVersionMajor,
                    protocolVersionMinor = protocolVersionMinor,
                    txId = transaction.id,
                    tx = tx,
                )
            }
        }
    }

    is BlockAllegra -> {
        with(allegra) {
            val blockNumber = header.blockHeight
            val slotNumber = header.slot
            val blockSize = header.blockSize.toInt()
            val blockBodyHash = header.blockHash
            val protocolVersionMajor = header.protocolVersion?.major ?: 0
            val protocolVersionMinor = header.protocolVersion?.minor ?: 0

            body.map { transaction ->
                val tx = transaction.raw!!.b64ToByteArray()

                RawTransaction(
                    blockNumber = blockNumber,
                    slotNumber = slotNumber,
                    blockSize = blockSize,
                    blockBodyHash = blockBodyHash,
                    protocolVersionMajor = protocolVersionMajor,
                    protocolVersionMinor = protocolVersionMinor,
                    txId = transaction.id,
                    tx = tx,
                )
            }
        }
    }

    is BlockMary -> {
        with(mary) {
            val blockNumber = header.blockHeight
            val slotNumber = header.slot
            val blockSize = header.blockSize.toInt()
            val blockBodyHash = header.blockHash
            val protocolVersionMajor = header.protocolVersion?.major ?: 0
            val protocolVersionMinor = header.protocolVersion?.minor ?: 0

            body.map { transaction ->
                val tx = transaction.raw!!.b64ToByteArray()

                RawTransaction(
                    blockNumber = blockNumber,
                    slotNumber = slotNumber,
                    blockSize = blockSize,
                    blockBodyHash = blockBodyHash,
                    protocolVersionMajor = protocolVersionMajor,
                    protocolVersionMinor = protocolVersionMinor,
                    txId = transaction.id,
                    tx = tx,
                )
            }
        }
    }

    is BlockAlonzo -> {
        with(alonzo) {
            val blockNumber = header.blockHeight
            val slotNumber = header.slot
            val blockSize = header.blockSize.toInt()
            val blockBodyHash = header.blockHash
            val protocolVersionMajor = header.protocolVersion?.major ?: 0
            val protocolVersionMinor = header.protocolVersion?.minor ?: 0

            body.map { transaction ->
                val tx = transaction.raw!!.b64ToByteArray()

                RawTransaction(
                    blockNumber = blockNumber,
                    slotNumber = slotNumber,
                    blockSize = blockSize,
                    blockBodyHash = blockBodyHash,
                    protocolVersionMajor = protocolVersionMajor,
                    protocolVersionMinor = protocolVersionMinor,
                    txId = transaction.id,
                    tx = tx,
                )
            }
        }
    }

    is BlockBabbage -> {
        with(babbage) {
            val blockNumber = header.blockHeight
            val slotNumber = header.slot
            val blockSize = header.blockSize.toInt()
            val blockBodyHash = header.blockHash
            val protocolVersionMajor = header.protocolVersion?.major ?: 0
            val protocolVersionMinor = header.protocolVersion?.minor ?: 0

            body.map { transaction ->
                val tx = transaction.raw!!.b64ToByteArray()

                RawTransaction(
                    blockNumber = blockNumber,
                    slotNumber = slotNumber,
                    blockSize = blockSize,
                    blockBodyHash = blockBodyHash,
                    protocolVersionMajor = protocolVersionMajor,
                    protocolVersionMinor = protocolVersionMinor,
                    txId = transaction.id,
                    tx = tx,
                )
            }
        }
    }
}

private fun Certificate.toStakeDelegationOrNull(blockNumber: Long, epoch: Long) = when (this) {
    is DelegationCertificate -> {
        io.newm.chain.database.entity.StakeDelegation(
            blockNumber = blockNumber,
            stakeAddress = this.stakeDelegation.delegator,
            poolId = this.stakeDelegation.delegatee,
            epoch = epoch,
        )
    }

    else -> null
}

/**
 * Extract the address type from an address
 */
fun String.addressType(): String {
    return if (this.startsWith("addr")) {
        val addressBytes = Bech32.decode(this).bytes
        addressBytes[0].toUByte().toString(16).padStart(2, '0')
    } else {
        // TODO: Fix hardcoding for byron stuff
        "82"
    }
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
