package io.newm.server.ktx

import io.newm.chain.grpc.LedgerAssetMetadataItem
import io.newm.server.features.cardano.model.LedgerAssetMetadata

fun LedgerAssetMetadataItem.toLedgerAssetMetadata(): LedgerAssetMetadata {
    return LedgerAssetMetadata(
        keyType = this.keyType,
        key = this.key,
        valueType = this.valueType,
        value = this.value,
        nestLevel = this.nestLevel,
        children = this.childrenList.map { it.toLedgerAssetMetadata() }
    )
}
