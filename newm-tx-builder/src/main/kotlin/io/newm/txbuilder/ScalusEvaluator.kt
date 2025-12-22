package io.newm.txbuilder

import io.newm.chain.grpc.Utxo
import io.newm.chain.util.config.Config
import io.newm.kogmios.protocols.model.ExecutionUnits
import io.newm.kogmios.protocols.model.Validator
import io.newm.kogmios.protocols.model.result.EvaluateTx
import io.newm.kogmios.protocols.model.result.EvaluateTxResult
import scalus.cardano.ledger.Redeemer
import scalus.cardano.ledger.RedeemerTag
import java.math.BigInteger
import scala.Option
import scala.Tuple2
import scalus.builtin.ByteString
import scalus.cardano.address.Address
import scalus.cardano.ledger.CardanoInfo
import scalus.cardano.ledger.Coin
import scalus.cardano.ledger.EvaluatorMode
import scalus.builtin.Data
import scalus.cardano.ledger.DatumOption
import scalus.cardano.ledger.MultiAsset
import scalus.cardano.ledger.PlutusScriptEvaluator
import scalus.cardano.ledger.ProtocolVersion
import scalus.cardano.ledger.Transaction
import scalus.cardano.ledger.TransactionInput
import scalus.cardano.ledger.TransactionOutput
import scalus.cardano.ledger.Value
import io.newm.chain.util.hexToByteArray

import scalus.cardano.ledger.`Hashes$package$`

/**
 * A script evaluator that uses Scalus' local CEK machine implementation, allowing to get tx redeemers without
 * using the network.
 */
object ScalusEvaluator {
    /**
     * Evaluates a transaction's Plutus scripts and returns execution units.
     *
     * @param cborBytes The transaction CBOR bytes
     * @param utxos All UTxOs needed for evaluation (source + reference inputs)
     * @param config The configuration object containing network information
     */
    fun evaluateTx(
        cborBytes: ByteArray,
        utxos: Set<Utxo>,
        config: Config,
    ): EvaluateTxResult {
        val cardanoInfo =
            if (config.isMainnet) {
                CardanoInfo.mainnet()
            } else {
                CardanoInfo.preprod()
            }

        val evaluator = PlutusScriptEvaluator.apply(cardanoInfo, EvaluatorMode.EvaluateAndComputeCost)
        val tx = Transaction.fromCbor(cborBytes, ProtocolVersion.conwayPV())
        val scalusUtxos = convertToScalusUtxos(utxos)

        val evaluatedRedeemers = evaluator.evalPlutusScripts(tx, scalusUtxos)
        return convertToEvaluateTxResult(evaluatedRedeemers)
    }

    private fun convertToEvaluateTxResult(evaluatedRedeemers: scala.collection.immutable.Seq<Redeemer>): EvaluateTxResult {
        val result = EvaluateTxResult()
        val iterator = evaluatedRedeemers.iterator()
        while (iterator.hasNext()) {
            val redeemer = iterator.next()

            val tag = redeemer.tag()
            val purpose =
                when (tag) {
                    RedeemerTag.valueOf("Spend") -> "spend"
                    RedeemerTag.valueOf("Mint") -> "mint"
                    RedeemerTag.valueOf("Cert") -> "certificate"
                    RedeemerTag.valueOf("Reward") -> "withdrawal"
                    RedeemerTag.valueOf("Voting") -> "vote"
                    RedeemerTag.valueOf("Proposing") -> "propose"
                    else -> throw IllegalStateException("Unknown redeemer tag: $tag")
                }

            val validator = Validator(redeemer.index().toInt(), purpose)
            val executionUnits =
                ExecutionUnits(
                    memory = BigInteger.valueOf(redeemer.exUnits().memory()),
                    cpu = BigInteger.valueOf(redeemer.exUnits().steps())
                )

            result.add(EvaluateTx(validator, executionUnits))
        }

        return result
    }

    private fun convertToScalusUtxos(utxos: Set<Utxo>): scala.collection.immutable.Map<TransactionInput, TransactionOutput> {
        val entries = mutableListOf<Tuple2<TransactionInput, TransactionOutput>>()

        // Map the utxo to Scalus ledger types.
        utxos.forEach { utxo ->
            val txHash = `Hashes$package$`.TransactionHash.fromHex(utxo.hash)
            val txInput = TransactionInput.apply(txHash, utxo.ix.toInt())

            val address = Address.fromBech32(utxo.address)

            val lovelace = Coin.apply(utxo.lovelace.toLong())
            val value = Value.apply(lovelace, MultiAsset.empty())

            val datumOption: Option<DatumOption> =
                when {
                    utxo.hasDatum() && utxo.isInlineDatum -> {
                        // Inline datum - parse from CBOR hex
                        val datumBytes = utxo.datum.cborHex.hexToByteArray()
                        val datumData = Data.fromCbor(datumBytes)
                        Option.apply(DatumOption.Inline.apply(datumData))
                    }

                    !utxo.datumHash.isNullOrEmpty() -> {
                        val datumHash = `Hashes$package$`.DataHash.fromHex(utxo.datumHash)
                        Option.apply(DatumOption.Hash.apply(datumHash))
                    }

                    else -> {
                        Option.empty<DatumOption>()
                    }
                }

            val txOutput = TransactionOutput.apply(address, value, datumOption, Option.empty())

            entries.add(Tuple2(txInput, txOutput))
        }

        val scalaBuffer = scala.jdk.javaapi.CollectionConverters
            .asScala(entries.toList())
        val scalaSeq = scalaBuffer.toSeq()
        return scala.collection.immutable.Map
            .from(scalaSeq)
    }
}
