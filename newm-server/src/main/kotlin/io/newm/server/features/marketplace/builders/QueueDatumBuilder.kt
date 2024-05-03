package io.newm.server.features.marketplace.builders

import io.newm.chain.grpc.PlutusData
import io.newm.chain.grpc.plutusData
import io.newm.chain.grpc.plutusDataList
import io.newm.chain.util.hashFromPaymentAddress
import io.newm.chain.util.hexToByteArray
import io.newm.txbuilder.ktx.toPlutusData

fun buildQueueDatum(
    ownerAddress: String,
    numberOfBundles: Long,
    incentivePolicyId: String,
    incentiveAssetName: String,
    incentiveAmount: Long,
    pointerAssetName: String
): PlutusData =
    plutusData {
        constr = 0
        list =
            plutusDataList {
                listItem.add(
                    plutusData {
                        constr = 0
                        list =
                            plutusDataList {
                                val hash = hashFromPaymentAddress(ownerAddress)
                                if (hash.size == 28) {
                                    listItem.add(hash.toPlutusData())
                                    listItem.add(byteArrayOf().toPlutusData())
                                } else {
                                    listItem.add(hash.sliceArray(0..27).toPlutusData())
                                    listItem.add(hash.sliceArray(28..56).toPlutusData())
                                }
                            }
                    }
                )
                listItem.add(numberOfBundles.toPlutusData())
                listItem.add(
                    plutusData {
                        constr = 0
                        list =
                            plutusDataList {
                                listItem.add(incentivePolicyId.hexToByteArray().toPlutusData())
                                listItem.add(incentiveAssetName.hexToByteArray().toPlutusData())
                                listItem.add(incentiveAmount.toPlutusData())
                            }
                    }
                )
                listItem.add(pointerAssetName.hexToByteArray().toPlutusData())
            }
    }
