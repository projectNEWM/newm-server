package io.newm.server.features.marketplace.builders

import io.newm.chain.grpc.PlutusData
import io.newm.chain.grpc.plutusData
import io.newm.chain.grpc.plutusDataList
import io.newm.chain.util.hashFromPaymentAddress
import io.newm.chain.util.hexToByteArray
import io.newm.server.features.marketplace.model.Token
import io.newm.txbuilder.ktx.toPlutusData

fun buildSaleDatum(
    ownerAddress: String,
    bundleToken: Token,
    costToken: Token,
    maxBundleSize: Long
): PlutusData =
    plutusData {
        constr = 0
        list =
            plutusDataList {
                listItem.add(ownerAddress.ownerAddressToPlutusData())
                listItem.add(bundleToken.toPlutusData())
                listItem.add(costToken.toPlutusData())
                listItem.add(maxBundleSize.toPlutusData())
            }
    }

fun buildQueueDatum(
    ownerAddress: String,
    numberOfBundles: Long,
    incentiveToken: Token,
    pointerAssetName: String
): PlutusData =
    plutusData {
        constr = 0
        list =
            plutusDataList {
                listItem.add(ownerAddress.ownerAddressToPlutusData())
                listItem.add(numberOfBundles.toPlutusData())
                listItem.add(incentiveToken.toPlutusData())
                listItem.add(pointerAssetName.hexToByteArray().toPlutusData())
            }
    }

private fun String.ownerAddressToPlutusData(): PlutusData =
    plutusData {
        constr = 0
        list =
            plutusDataList {
                val hash = hashFromPaymentAddress(this@ownerAddressToPlutusData)
                if (hash.size == 28) {
                    listItem.add(hash.toPlutusData())
                    listItem.add(byteArrayOf().toPlutusData())
                } else {
                    listItem.add(hash.sliceArray(0..27).toPlutusData())
                    listItem.add(hash.sliceArray(28..56).toPlutusData())
                }
            }
    }

private fun Token.toPlutusData(): PlutusData =
    plutusData {
        constr = 0
        list =
            plutusDataList {
                listItem.add(policyId.hexToByteArray().toPlutusData())
                listItem.add(assetName.hexToByteArray().toPlutusData())
                listItem.add(amount.toPlutusData())
            }
    }
