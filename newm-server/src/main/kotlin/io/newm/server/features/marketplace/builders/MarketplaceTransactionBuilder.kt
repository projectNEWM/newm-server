package io.newm.server.features.marketplace.builders

import com.google.protobuf.ByteString
import io.newm.chain.grpc.NativeAsset
import io.newm.chain.grpc.PlutusData
import io.newm.chain.grpc.Redeemer
import io.newm.chain.grpc.RedeemerTag
import io.newm.chain.grpc.Signature
import io.newm.chain.grpc.TransactionBuilderResponse
import io.newm.chain.grpc.Utxo
import io.newm.chain.grpc.outputUtxo
import io.newm.chain.grpc.plutusData
import io.newm.chain.grpc.plutusDataList
import io.newm.chain.grpc.redeemer
import io.newm.chain.util.toHexString
import io.newm.server.features.cardano.repo.CardanoRepository
import io.newm.shared.exception.HttpUnprocessableEntityException
import io.newm.txbuilder.ktx.toCborObject

suspend fun CardanoRepository.buildSaleTransaction(
    sourceUtxos: List<Utxo>,
    collateralUtxos: List<Utxo>,
    referenceInputUtxos: List<Utxo>,
    contractAddress: String,
    changeAddress: String,
    lovelace: Long,
    bundleAsset: NativeAsset,
    pointerAsset: NativeAsset,
    saleDatum: PlutusData,
    requiredSigners: List<ByteString>,
    signatures: List<Signature>,
    fee: Long? = null,
    totalCollateral: Long? = null,
    redeemers: List<Redeemer>? = null
): TransactionBuilderResponse =
    buildTransaction {
        this.sourceUtxos.addAll(sourceUtxos)
        this.outputUtxos.add(
            outputUtxo {
                address = contractAddress
                this.lovelace = lovelace.toString()
                nativeAssets.add(bundleAsset)
                nativeAssets.add(pointerAsset)
                datum = saleDatum.toCborObject().toCborByteArray().toHexString()
            }
        )
        this.changeAddress = changeAddress
        this.mintTokens.add(pointerAsset)
        this.referenceInputs.addAll(referenceInputUtxos)
        this.collateralUtxos.addAll(collateralUtxos)
        this.collateralReturnAddress = collateralUtxos.first().address
        this.requiredSigners.addAll(requiredSigners)
        this.signatures.addAll(signatures)
        fee?.let {
            this.fee = fee
        }
        totalCollateral?.let {
            this.totalCollateral = it
        }
        redeemers?.let {
            this.redeemers.addAll(it)
        } ?: run {
            this.redeemers.add(
                redeemer {
                    tag = RedeemerTag.MINT
                    index = 0L
                    data = plutusData {
                        constr = 0
                        list = plutusDataList { }
                    }
                }
            )
        }
    }.also {
        if (it.hasErrorMessage()) {
            throw HttpUnprocessableEntityException("Failed to build sale transaction: ${it.errorMessage}")
        }
    }
