package io.newm.server.features.marketplace.parser

import com.google.protobuf.ByteString
import io.newm.chain.grpc.NativeAsset
import io.newm.chain.grpc.PlutusDataList
import io.newm.chain.grpc.Utxo
import io.newm.chain.util.hashToPaymentAddress
import io.newm.server.features.marketplace.model.QueueTransaction
import io.newm.server.features.marketplace.model.SaleTransaction
import io.newm.server.features.marketplace.model.Token
import io.newm.shared.ktx.toHexString

internal fun Utxo.parseSale(isMainnet: Boolean): SaleTransaction {
    val items = datum.list.listItemList
    return SaleTransaction(
        tokens = nativeAssetsList.map(NativeAsset::toToken),
        ownerAddress = items[0].list.toPaymentAddress(isMainnet),
        bundle = items[1].list.toToken(),
        cost = items[2].list.toToken(),
        maxBundleSize = items[3].int
    )
}

internal fun Utxo.parseQueue(isMainnet: Boolean): QueueTransaction {
    val items = datum.list.listItemList
    return QueueTransaction(
        tokens = nativeAssetsList.map(NativeAsset::toToken),
        ownerAddress = items[0].list.toPaymentAddress(isMainnet),
        numberOfBundles = items[1].int,
        incentive = items[2].list.toToken(),
        pointerAssetName = items[3].bytes.toHexString()
    )
}

private fun NativeAsset.toToken(): Token =
    Token(
        policyId = policy,
        assetName = name,
        amount = amount.toLong()
    )

private fun PlutusDataList.toToken(): Token =
    Token(
        policyId = listItemList[0].bytes.toHexString(),
        assetName = listItemList[1].bytes.toHexString(),
        amount = listItemList[2].int
    )

private fun PlutusDataList.toPaymentAddress(isMainnet: Boolean): String =
    (
        listItemList[0].bytes.toByteArray() +
            listItemList[1].bytes.toByteArray()
    ).hashToPaymentAddress(isMainnet)

private fun ByteString.toHexString() = toByteArray().toHexString()
