package io.newm.txbuilder

import io.newm.chain.grpc.Utxo
import io.newm.chain.util.config.Config
import io.newm.kogmios.protocols.model.ExecutionUnits
import io.newm.kogmios.protocols.model.Validator
import io.newm.kogmios.protocols.model.result.EvaluateTx
import io.newm.kogmios.protocols.model.result.EvaluateTxResult
import io.newm.kogmios.protocols.model.result.ProtocolParametersResult
import scalus.cardano.ledger.Redeemer
import scalus.cardano.ledger.RedeemerTag
import scalus.cardano.ledger.SlotConfig
import java.math.BigInteger
import scala.Option
import scala.Tuple2
import scalus.cardano.address.Address
import scalus.cardano.ledger.Coin
import scalus.cardano.ledger.EvaluatorMode
import scalus.cardano.ledger.ExUnits
import scalus.cardano.ledger.MajorProtocolVersion
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
     * @param protocolParameters Protocol parameters containing cost models and major protocol version
     * @param config Configuration object to determine network (mainnet vs testnet)
     */
    fun evaluateTx(
        cborBytes: ByteArray,
        utxos: Set<Utxo>,
        protocolParameters: ProtocolParametersResult,
        config: Config,
    ): EvaluateTxResult {
        val slotConfig = if (config.isMainnet) SlotConfig.Mainnet() else SlotConfig.Preprod()
        val costModels = convertCostModels(protocolParameters)

        val initialBudget = ExUnits(
            protocolParameters.maxExecutionUnitsPerTransaction.memory.toLong(),
            protocolParameters.maxExecutionUnitsPerTransaction.cpu.toLong()
        )

        val evaluator = PlutusScriptEvaluator.apply(
            slotConfig,
            initialBudget,
            MajorProtocolVersion(protocolParameters.version.major),
            costModels,
            EvaluatorMode.EvaluateAndComputeCost,
            false, // debugDumpFilesForTesting
            false  // logBudgetDifferences
        )

        val tx = Transaction.fromCbor(cborBytes, ProtocolVersion.conwayPV())
        val scalusUtxos = convertToScalusUtxos(utxos)

        val evaluatedRedeemers = evaluator.evalPlutusScripts(tx, scalusUtxos)
        return convertToEvaluateTxResult(evaluatedRedeemers)
    }

    private fun convertCostModels(protocolParameters: ProtocolParametersResult): scalus.cardano.ledger.CostModels {
        val kogmiosCostModels = protocolParameters.plutusCostModels
        val tuples = mutableListOf<scala.Tuple2<Int, scala.collection.immutable.IndexedSeq<Long>>>()

        // PlutusV1 = language 0
        kogmiosCostModels.plutusV1?.let { v1 ->
            tuples.add(scala.Tuple2(0, convertCostModelArray(v1)))
        }

        // PlutusV2 = language 1
        kogmiosCostModels.plutusV2?.let { v2 ->
            tuples.add(scala.Tuple2(1, convertCostModelArray(v2)))
        }

        // PlutusV3 = language 2
        kogmiosCostModels.plutusV3?.let { v3 ->
            tuples.add(scala.Tuple2(2, convertCostModelArray(v3)))
        }

        // Convert to Scala immutable.Map
        val scalaSeq = scala.jdk.javaapi.CollectionConverters.asScala(tuples).toSeq()
        @Suppress("UNCHECKED_CAST")
        val scalaMap = scala.collection.immutable.Map.from(scalaSeq) as scala.collection.immutable.Map<Any, scala.collection.immutable.IndexedSeq<Any>>

        return scalus.cardano.ledger.CostModels(scalaMap)
    }

    private fun convertCostModelArray(costModel: List<BigInteger>): scala.collection.immutable.IndexedSeq<Long> {
        val longArray = costModel.map { it.toLong() }
        return scala.jdk.javaapi.CollectionConverters.asScala(longArray).toIndexedSeq()
    }

    private fun convertToEvaluateTxResult(evaluatedRedeemers: scala.collection.immutable.Seq<Redeemer>): EvaluateTxResult {
        val result = EvaluateTxResult()
        val iterator = evaluatedRedeemers.iterator()
        while (iterator.hasNext()) {
            val redeemer = iterator.next()

            val tag = redeemer.tag()
            val purpose =
                when (tag) {
                    RedeemerTag.Spend -> "spend"
                    RedeemerTag.Mint -> "mint"
                    RedeemerTag.Cert -> "certificate"
                    RedeemerTag.Reward -> "withdrawal"
                    RedeemerTag.Voting -> "vote"
                    RedeemerTag.Proposing -> "propose"
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
