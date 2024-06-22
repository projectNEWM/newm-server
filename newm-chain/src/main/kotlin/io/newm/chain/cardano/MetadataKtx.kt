package io.newm.chain.cardano

import com.google.protobuf.ByteString
import com.google.protobuf.kotlin.toByteString
import io.newm.chain.database.entity.LedgerAssetMetadata
import io.newm.chain.grpc.LedgerAssetMetadataItem
import io.newm.chain.grpc.PlutusData
import io.newm.chain.grpc.ledgerAssetMetadataItem
import io.newm.chain.util.hexToByteArray
import io.newm.chain.util.toB64String
import io.newm.kogmios.protocols.model.MetadataBytes
import io.newm.kogmios.protocols.model.MetadataInteger
import io.newm.kogmios.protocols.model.MetadataList
import io.newm.kogmios.protocols.model.MetadataMap
import io.newm.kogmios.protocols.model.MetadataString
import io.newm.kogmios.protocols.model.MetadataValue

private const val NULL_BYTE = 0x00.toByte()

fun ByteString.isValidDbUtf8(): Boolean = isValidUtf8 && !contains(NULL_BYTE)

fun PlutusData.toMetadataMap(
    policy: String,
    nameHex: String
): MetadataMap {
    val cip68PlutusData = this
    return MetadataMap().apply {
        this[MetadataString(policy)] =
            MetadataMap().apply {
                val nameKey =
                    if (nameHex.hexToByteArray().toByteString().isValidDbUtf8()) {
                        MetadataString(String(nameHex.hexToByteArray()))
                    } else {
                        MetadataBytes(nameHex.hexToByteArray().toB64String())
                    }
                this[nameKey] =
                    MetadataMap().apply {
                        if (cip68PlutusData.hasList()) {
                            cip68PlutusData.list.listItemList?.firstOrNull()?.let { plutusMetadata ->
                                if (plutusMetadata.hasMap()) {
                                    plutusMetadata.map.mapItemList.forEach { plutusDataMapItem ->
                                        val key: MetadataValue = plutusDataMapItem.mapItemKey.toMetadataValue()
                                        if (key is MetadataString) {
                                            val value: MetadataValue = plutusDataMapItem.mapItemValue.toMetadataValue()
                                            this[key] = value
                                        }
                                    }
                                }
                            }
                        }
                    }
            }
    }
}

fun PlutusData.toMetadataValue(): MetadataValue =
    when (plutusDataWrapperCase) {
        PlutusData.PlutusDataWrapperCase.MAP ->
            MetadataMap().apply {
                map.mapItemList.forEach { plutusDataMapItem ->
                    val key = plutusDataMapItem.mapItemKey.toMetadataValue()
                    val value = plutusDataMapItem.mapItemValue.toMetadataValue()
                    this[key] = value
                }
            }

        PlutusData.PlutusDataWrapperCase.LIST ->
            MetadataList().apply {
                list.listItemList.forEach { plutusDataListItem ->
                    add(plutusDataListItem.toMetadataValue())
                }
            }

        PlutusData.PlutusDataWrapperCase.INT -> MetadataInteger(int.toBigInteger())
        PlutusData.PlutusDataWrapperCase.BYTES ->
            if (bytes.isValidDbUtf8()) {
                MetadataString(bytes.toStringUtf8())
            } else {
                MetadataBytes(bytes.toByteArray().toB64String())
            }

        else -> throw IllegalStateException("plutusDataWrapper must be set!")
    }

fun LedgerAssetMetadata.toLedgerAssetMetadataItem(): LedgerAssetMetadataItem {
    val ledgerAssetMetadata = this
    return ledgerAssetMetadataItem {
        keyType = ledgerAssetMetadata.keyType
        key = ledgerAssetMetadata.key
        valueType = ledgerAssetMetadata.valueType
        value = ledgerAssetMetadata.value
        nestLevel = ledgerAssetMetadata.nestLevel
        children.addAll(ledgerAssetMetadata.children.map { it.toLedgerAssetMetadataItem() })
    }
}

/**
 * Convert database records to json
 */
fun List<LedgerAssetMetadata>.to721Json(
    policy: String,
    name: String
): String {
    val sb = StringBuilder()
    sb.append("{")
    sb.append("\"721\":{")
    sb.append("\"$policy\":{")
    name.hexToByteArray().toByteString().let { byteString ->
        if (byteString.isValidDbUtf8()) {
            sb.append("\"${byteString.toStringUtf8()}\":{")
        } else {
            sb.append("\"$name\":{")
        }
    }
    this.forEach { ledgerAssetMetadata ->
        sb.append(ledgerAssetMetadata.to721Json())
        sb.append(",")
    }
    sb.setLength(sb.length - 1) // remove last comma
    sb.append("}") // name
    sb.append("}") // policy
    sb.append("}") // 721
    sb.append("}")
    return sb.toString()
}

fun LedgerAssetMetadata.to721Json(isArrayItem: Boolean = false): String {
    val sb = StringBuilder()
    if (!isArrayItem) {
        sb.append("\"$key\":")
    }
    when (this.valueType) {
        "array" -> {
            sb.append("[")
            if (children.isNotEmpty()) {
                children.forEach {
                    sb.append(it.to721Json(true))
                    sb.append(",")
                }
                sb.setLength(sb.length - 1) // remove last comma
            }
            sb.append("]")
        }

        "map" -> {
            sb.append("{")
            if (children.isNotEmpty()) {
                children.forEach {
                    sb.append(it.to721Json())
                    sb.append(",")
                }
                sb.setLength(sb.length - 1) // remove last comma
            }
            sb.append("}")
        }

        "string" -> sb.append("\"$value\"")
        "bytestring" -> sb.append("\"$value\"")
        "integer" -> sb.append(value)
    }
    return sb.toString()
}
