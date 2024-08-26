package io.newm.txbuilder

import com.google.common.annotations.VisibleForTesting
import com.google.iot.cbor.CborArray
import com.google.iot.cbor.CborByteString
import com.google.iot.cbor.CborInteger
import com.google.iot.cbor.CborMap
import com.google.iot.cbor.CborObject
import com.google.iot.cbor.CborReader
import com.google.iot.cbor.CborSimple
import io.newm.chain.grpc.NativeAsset
import io.newm.chain.grpc.NativeScript
import io.newm.chain.grpc.NetworkId
import io.newm.chain.grpc.OutputUtxo
import io.newm.chain.grpc.PlutusData
import io.newm.chain.grpc.Redeemer
import io.newm.chain.grpc.Signature
import io.newm.chain.grpc.SigningKey
import io.newm.chain.grpc.TransactionBuilderRequest
import io.newm.chain.grpc.Utxo
import io.newm.chain.grpc.exUnits
import io.newm.chain.grpc.outputUtxo
import io.newm.chain.grpc.redeemer
import io.newm.chain.util.Blake2b
import io.newm.chain.util.toHexString
import io.newm.kogmios.protocols.model.CardanoEra
import io.newm.kogmios.protocols.model.result.EvaluateTxResult
import io.newm.kogmios.protocols.model.result.ProtocolParametersResult
import io.newm.txbuilder.ktx.sign
import io.newm.txbuilder.ktx.toCborObject
import io.newm.txbuilder.ktx.toConwayCborObject
import io.newm.txbuilder.ktx.toNativeAssetCborMap
import io.newm.txbuilder.ktx.toNativeAssetMap
import io.newm.txbuilder.ktx.toRedeemerTagAndIndex
import io.newm.txbuilder.ktx.toSetTag
import io.newm.txbuilder.ktx.withMinUtxo
import java.math.BigInteger
import java.security.SecureRandom
import kotlin.math.ceil
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow

/**
 * A builder for cardano babbage transactions.
 * reference: https://github.com/input-output-hk/cardano-ledger/blob/master/eras/babbage/test-suite/cddl-files/babbage.cddl
 */
class TransactionBuilder(
    private val protocolParameters: ProtocolParametersResult,
    private val cardanoEra: CardanoEra = CardanoEra.BABBAGE,
    private val calculateTxExecutionUnits: (suspend (ByteArray) -> EvaluateTxResult)? = null,
    private val calculateReferenceScriptBytes: (suspend (Set<Utxo>) -> Long)? = null,
) {
    private val secureRandom by lazy { SecureRandom() }

    private val txFeeFixed by lazy {
        protocolParameters.minFeeConstant.ada.lovelace
            .toLong()
    }
    private val txFeePerByte by lazy { protocolParameters.minFeeCoefficient.toLong() }
    private val utxoCostPerByte by lazy { protocolParameters.minUtxoDepositCoefficient.toLong() }
    private val maxTxExecutionMemory by lazy { protocolParameters.maxExecutionUnitsPerTransaction.memory.toLong() }
    private val maxTxExecutionSteps by lazy { protocolParameters.maxExecutionUnitsPerTransaction.cpu.toLong() }

    private var sourceUtxos: MutableSet<Utxo>? = null
    private var outputUtxos: MutableSet<OutputUtxo>? = null
    private var signingKeys: MutableSet<SigningKey>? = null
    private var signatures: MutableSet<Signature>? = null
    private var mintTokens: MutableList<NativeAsset>? = null
    private var referenceInputs: MutableSet<Utxo>? = null
    private var nativeScripts: MutableList<NativeScript>? = null
    private var plutusV1Scripts: MutableList<ByteArray>? = null
    private var plutusV2Scripts: MutableList<ByteArray>? = null
    private var plutusV3Scripts: MutableList<ByteArray>? = null
    private var auxNativeScripts: MutableList<NativeScript>? = null
    private var auxPlutusV1Scripts: MutableList<ByteArray>? = null
    private var auxPlutusV2Scripts: MutableList<ByteArray>? = null
    private var auxPlutusV3Scripts: MutableList<ByteArray>? = null
    private var collateralUtxos: MutableSet<Utxo>? = null
    private var requiredSigners: MutableSet<ByteArray>? = null
    private var redeemers: MutableList<Redeemer>? = null
    private var datums: MutableList<PlutusData>? = null

    var changeAddress: String? = null
    var fee: Long? = null
    var ttlAbsoluteSlot: Long? = null
    var auxDataHash: ByteArray? = null
    var validityIntervalStart: Long? = null
    var scriptDataHash: ByteArray? = null
    var networkId: NetworkId? = null
    var collateralReturnAddress: String? = null
    var collateralReturn: OutputUtxo? = null
    var totalCollateral: Long? = null
    var transactionMetadata: CborMap? = null

    private lateinit var _transactionId: ByteArray

    val transactionId: String
        get() = _transactionId.toHexString()

    fun sourceUtxos(block: MutableSet<Utxo>.() -> Unit) {
        if (this.sourceUtxos == null) {
            this.sourceUtxos = mutableSetOf()
        }
        block.invoke(this.sourceUtxos!!)
    }

    fun outputUtxos(block: MutableSet<OutputUtxo>.() -> Unit) {
        if (this.outputUtxos == null) {
            this.outputUtxos = mutableSetOf()
        }
        block.invoke(this.outputUtxos!!)
    }

    fun signingKeys(block: MutableSet<SigningKey>.() -> Unit) {
        if (this.signingKeys == null) {
            this.signingKeys = mutableSetOf()
        }
        block.invoke(this.signingKeys!!)
    }

    fun signatures(block: MutableSet<Signature>.() -> Unit) {
        if (this.signatures == null) {
            this.signatures = mutableSetOf()
        }
        block.invoke(this.signatures!!)
    }

    fun mintTokens(block: MutableList<NativeAsset>.() -> Unit) {
        if (this.mintTokens == null) {
            this.mintTokens = mutableListOf()
        }
        block.invoke(this.mintTokens!!)
    }

    fun collateralUtxos(block: MutableSet<Utxo>.() -> Unit) {
        if (this.collateralUtxos == null) {
            this.collateralUtxos = mutableSetOf()
        }
        block.invoke(this.collateralUtxos!!)
    }

    fun requiredSigners(block: MutableSet<ByteArray>.() -> Unit) {
        if (this.requiredSigners == null) {
            this.requiredSigners = mutableSetOf()
        }
        block.invoke(this.requiredSigners!!)
    }

    fun referenceInputs(block: MutableSet<Utxo>.() -> Unit) {
        if (this.referenceInputs == null) {
            this.referenceInputs = mutableSetOf()
        }
        block.invoke(this.referenceInputs!!)
    }

    fun nativeScripts(block: MutableList<NativeScript>.() -> Unit) {
        if (this.nativeScripts == null) {
            this.nativeScripts = mutableListOf()
        }
        block.invoke(this.nativeScripts!!)
    }

    fun plutusV1Scripts(block: MutableList<ByteArray>.() -> Unit) {
        if (this.plutusV1Scripts == null) {
            this.plutusV1Scripts = mutableListOf()
        }
        block.invoke(this.plutusV1Scripts!!)
    }

    fun plutusV2Scripts(block: MutableList<ByteArray>.() -> Unit) {
        if (this.plutusV2Scripts == null) {
            this.plutusV2Scripts = mutableListOf()
        }
        block.invoke(this.plutusV2Scripts!!)
    }

    fun plutusV3Scripts(block: MutableList<ByteArray>.() -> Unit) {
        if (this.plutusV3Scripts == null) {
            this.plutusV3Scripts = mutableListOf()
        }
        block.invoke(this.plutusV3Scripts!!)
    }

    fun auxNativeScripts(block: MutableList<NativeScript>.() -> Unit) {
        if (this.auxNativeScripts == null) {
            this.auxNativeScripts = mutableListOf()
        }
        block.invoke(this.auxNativeScripts!!)
    }

    fun auxPlutusV1Scripts(block: MutableList<ByteArray>.() -> Unit) {
        if (this.auxPlutusV1Scripts == null) {
            this.auxPlutusV1Scripts = mutableListOf()
        }
        block.invoke(this.auxPlutusV1Scripts!!)
    }

    fun auxPlutusV2Scripts(block: MutableList<ByteArray>.() -> Unit) {
        if (this.auxPlutusV2Scripts == null) {
            this.auxPlutusV2Scripts = mutableListOf()
        }
        block.invoke(this.auxPlutusV2Scripts!!)
    }

    fun auxPlutusV3Scripts(block: MutableList<ByteArray>.() -> Unit) {
        if (this.auxPlutusV3Scripts == null) {
            this.auxPlutusV3Scripts = mutableListOf()
        }
        block.invoke(this.auxPlutusV3Scripts!!)
    }

    fun redeemers(block: MutableList<Redeemer>.() -> Unit) {
        if (this.redeemers == null) {
            this.redeemers = mutableListOf()
        }
        block.invoke(this.redeemers!!)
    }

    fun datums(block: MutableList<PlutusData>.() -> Unit) {
        if (this.datums == null) {
            this.datums = mutableListOf()
        }
        block.invoke(this.datums!!)
    }

    /**
     * Build the final transaction cbor to submit.
     */
    suspend fun build(): ByteArray {
        validateInputs()
        val auxData: CborObject? = createAuxData()
        val startingScriptDataHash = scriptDataHash
        createScriptDataHash()
        calculateTemporaryCollateral()
        calculateTxFees(auxData)

        if (startingScriptDataHash == null) {
            // recalculate scriptDataHash now that we've calculated proper exUnits
            scriptDataHash = null
            createScriptDataHash()
        }

        // assemble the final transaction now that fees and collateral are correct
        val txBody = createTxBody()
        _transactionId = Blake2b.hash256(txBody.toCborByteArray())
        return CborArray
            .create()
            .apply {
                add(txBody)
                add(createTxWitnessSet())
                add(CborSimple.TRUE)
                add(auxData ?: CborSimple.NULL)
            }.toCborByteArray()
    }

    private fun validateInputs() {
        require(!sourceUtxos.isNullOrEmpty()) { "sourceUtxos must be defined!" }
        sourceUtxos?.forEach {
            require(it.lovelace.isNotBlank()) { "sourceUtxos must have lovelace defined!" }
        }
        if (outputUtxos.isNullOrEmpty()) {
            requireNotNull(changeAddress) { "outputUtxos or changeAddress must be defined!" }
        }
        if (changeAddress == null) {
            require(!outputUtxos.isNullOrEmpty()) { "outputUtxos or changeAddress must be defined!" }
        }
        if (!redeemers.isNullOrEmpty() || !referenceInputs.isNullOrEmpty()) {
            // There's some type of smart contract here. We must have collateral defined
            require(!collateralUtxos.isNullOrEmpty()) { "collateralUtxos must be defined!" }
            collateralUtxos?.forEach {
                require(it.lovelace.isNotBlank()) { "collateralUtxos must have lovelace defined!" }
            }
        }
    }

    private suspend fun updateFeesAndCollateral(
        cborByteSize: Int,
        computationalFee: Long?,
        referenceScriptBytes: Long?,
    ) {
        val referenceInputsFee: Long? = listOf(
            referenceScriptBytes?.takeIf { it > 0 },
            protocolParameters.maxReferenceScriptsSize?.bytes?.toLong(),
            protocolParameters.minFeeReferenceScripts?.range,
            protocolParameters.minFeeReferenceScripts?.base,
            protocolParameters.minFeeReferenceScripts?.multiplier,
        ).takeIf { it.all { item -> item != null } }?.let { (sb, mrsb, r, b, m) ->
            val scriptBytes: Long = sb!!.toLong()
            val maxReferenceScriptBytes: Long = mrsb!!.toLong()
            val range: Long = r!!.toLong()
            val base: Double = b!!.toDouble()
            val multiplier: Double = m!!.toDouble()

            require(scriptBytes <= maxReferenceScriptBytes) {
                "referenceScriptBytes($scriptBytes) must be less than or equal to maxReferenceScriptsSize($maxReferenceScriptBytes)"
            }
            val rangeFlow: Flow<Pair<LongRange, Long>> = flow {
                var rangeFrom = 1L
                var rangeTo = range
                var lovelacePerByte = base
                while (true) {
                    emit(rangeFrom..rangeTo to ceil(lovelacePerByte).toLong())
                    rangeFrom += range
                    rangeTo += range
                    lovelacePerByte *= multiplier
                }
            }
            val (_, lovelacePerByte) = rangeFlow.first { (range, _) -> scriptBytes in range }
            scriptBytes * lovelacePerByte
        }

        fee = (txFeeFixed + txFeePerByte * cborByteSize) + (computationalFee ?: 0L) + (referenceInputsFee ?: 0L)
        computationalFee?.let {
            totalCollateral = ceil(protocolParameters.collateralPercentage.toLong() * fee!! / 100.0).toLong()

            // fix collateralReturn now that we have accurate totalCollateral
            val collateralReturnLovelace = collateralUtxos!!.sumOf { it.lovelace.toLong() } - totalCollateral!!
            collateralReturn =
                outputUtxo {
                    address = collateralReturnAddress!!
                    lovelace = collateralReturnLovelace.toString()
                    nativeAssets.addAll(
                        collateralUtxos!!
                            .map { it.nativeAssetsList }
                            .flatten()
                            .toNativeAssetMap()
                            .values
                            .flatten()
                    )
                }
        }
    }

    @VisibleForTesting
    internal fun createAuxData(): CborObject? =
        mapOf<CborObject, CborObject?>(
            // Metadata
            AUX_DATA_KEY_METADATA to transactionMetadata,
            AUX_DATA_KEY_NATIVE_SCRIPT to
                auxNativeScripts.takeUnless { it.isNullOrEmpty() }?.let {
                    CborArray.create(it.map { nativeScript -> nativeScript.toCborObject() })
                },
            AUX_DATA_KEY_PLUTUS_V1_SCRIPT to
                auxPlutusV1Scripts.takeUnless { it.isNullOrEmpty() }?.let {
                    CborArray.create(it.map { plutusV1Script -> CborByteString.create(plutusV1Script) })
                },
            AUX_DATA_KEY_PLUTUS_V2_SCRIPT to
                auxPlutusV2Scripts.takeUnless { it.isNullOrEmpty() }?.let {
                    CborArray.create(it.map { plutusV2Script -> CborByteString.create(plutusV2Script) })
                },
            AUX_DATA_KEY_PLUTUS_V3_SCRIPT to
                auxPlutusV2Scripts.takeUnless { it.isNullOrEmpty() }?.let {
                    CborArray.create(it.map { plutusV3Script -> CborByteString.create(plutusV3Script) })
                },
        ).filterValues { it != null }.ifEmpty { null }?.let {
            CborMap.create(it, AUX_DATA_TAG).also { auxData ->
                auxDataHash = Blake2b.hash256(auxData.toCborByteArray())
            }
        }

    private fun createScriptDataHash() {
        if (scriptDataHash == null && (!redeemers.isNullOrEmpty() || !datums.isNullOrEmpty())) {
            // calculate the scriptDataHash - // redeemerBytes + datumBytes + languageViewMap
            val redeemerBytes = createRedeemerWitnesses()?.toCborByteArray() ?: ByteArray(1) { 0x80.toByte() }
            val datumBytes = createDatumWitnesses()?.toCborByteArray() ?: ByteArray(0)
            val languageViewMap =
                if (!redeemers.isNullOrEmpty()) {
                    if (!plutusV3Scripts.isNullOrEmpty() || !auxPlutusV3Scripts.isNullOrEmpty()) {
                        // Plutus V3
                        protocolParameters.plutusCostModels.plutusV3!!
                            .toCborObject()
                            .toCborByteArray()
                    } else {
                        // Plutus V2
                        protocolParameters.plutusCostModels.plutusV2!!
                            .toCborObject()
                            .toCborByteArray()
                    }
                } else {
                    // empty cbor map
                    ByteArray(1) { 0xa0.toByte() }
                }
            scriptDataHash = Blake2b.hash256(redeemerBytes + datumBytes + languageViewMap)
        }
    }

    private fun calculateTemporaryCollateral() {
        if (totalCollateral == null && !collateralUtxos.isNullOrEmpty()) {
            // set a dummy value of 4 ada
            totalCollateral = DUMMY_TOTAL_COLLATERAL
        }

        if (collateralReturnAddress != null && collateralReturn == null) {
            val collateralReturnLovelace = collateralUtxos!!.sumOf { it.lovelace.toLong() } - totalCollateral!!
            collateralReturn =
                outputUtxo {
                    address = collateralReturnAddress!!
                    lovelace = collateralReturnLovelace.toString()
                    nativeAssets.addAll(
                        collateralUtxos!!
                            .map { it.nativeAssetsList }
                            .flatten()
                            .toNativeAssetMap()
                            .values
                            .flatten()
                    )
                }
        }
    }

    private suspend fun calculateTxFees(auxData: CborObject?) {
        if (fee == null) {
            var txBody = createTxBody()
            // set a random _transactionId for dummy signing
            _transactionId = ByteArray(32)
            secureRandom.nextBytes(_transactionId)

            var dummyTxCbor =
                CborArray
                    .create()
                    .apply {
                        add(txBody)
                        add(createTxWitnessSet(true))
                        add(CborSimple.TRUE)
                        add(auxData ?: CborSimple.NULL)
                    }.toCborByteArray()

            // Calculate minimum transaction fee
            val maxComputationalFee =
                if (totalCollateral != null) {
                    ceil(
                        (
                            protocolParameters.scriptExecutionPrices.memory
                                .multiply(
                                    protocolParameters.maxExecutionUnitsPerTransaction.memory
                                ).add(
                                    protocolParameters.scriptExecutionPrices.cpu.multiply(
                                        protocolParameters.maxExecutionUnitsPerTransaction.cpu
                                    )
                                )
                        ).toDouble()
                    ).toLong()
                } else {
                    null
                }

            // Calculate referenceInputs fee
            val referenceScriptBytes =
                referenceInputs?.takeIf { it.isNotEmpty() && calculateReferenceScriptBytes != null }.let {
                    calculateReferenceScriptBytes!!.invoke(referenceInputs!!)
                }

            updateFeesAndCollateral(
                cborByteSize = dummyTxCbor.size,
                computationalFee = maxComputationalFee,
                referenceScriptBytes = referenceScriptBytes
            )

            if (totalCollateral != null && calculateTxExecutionUnits != null) {
                // calculate actual totalCollateral
                val evaluationResult = calculateTxExecutionUnits.invoke(dummyTxCbor)

                var totalMemory = BigInteger.ZERO
                var totalSteps = BigInteger.ZERO
                evaluationResult.forEach { (redeemerAction, executionUnits) ->
                    totalMemory += executionUnits.memory
                    totalSteps += executionUnits.cpu
                    val (redeemerTag, idx) = redeemerAction.toRedeemerTagAndIndex()
                    redeemers?.replaceAll {
                        if (it.tag == redeemerTag && it.index == idx) {
                            // Update with new executionUnits
                            redeemer {
                                tag = redeemerTag
                                index = idx
                                data = it.data
                                exUnits =
                                    exUnits {
                                        mem = executionUnits.memory.toLong()
                                        steps = executionUnits.cpu.toLong()
                                    }
                            }
                        } else {
                            // Leave unchanged
                            it
                        }
                    }
                }

                val computationalFee: Long = ceil(
                    (
                        protocolParameters.scriptExecutionPrices.memory
                            .multiply(totalMemory)
                            .add(protocolParameters.scriptExecutionPrices.cpu.multiply(totalSteps))
                    ).toDouble()
                ).toLong()
                updateFeesAndCollateral(
                    cborByteSize = dummyTxCbor.size,
                    computationalFee = computationalFee,
                    referenceScriptBytes = referenceScriptBytes
                )

                txBody = createTxBody()
                // set a random _transactionId for dummy signing
                _transactionId = ByteArray(32)
                secureRandom.nextBytes(_transactionId)

                dummyTxCbor =
                    CborArray
                        .create()
                        .apply {
                            add(txBody)
                            add(createTxWitnessSet(true))
                            add(CborSimple.TRUE)
                            add(auxData ?: CborSimple.NULL)
                        }.toCborByteArray()

                updateFeesAndCollateral(
                    cborByteSize = dummyTxCbor.size,
                    computationalFee = computationalFee,
                    referenceScriptBytes = referenceScriptBytes
                )
            }
        }
    }

    private fun createTxBody(): CborObject =
        CborMap.create(
            mapOf<CborObject, CborObject?>(
                // Utxo inputs
                TX_KEY_UTXO_INPUTS to sourceUtxos!!.toCborObject(cardanoEra),
                // Utxo outputs
                TX_KEY_UTXO_OUTPUTS to createOutputUtxos(),
                TX_KEY_FEE to (fee?.let { CborInteger.create(it) } ?: CborInteger.create(DUMMY_TX_FEE)),
                TX_KEY_TTL to ttlAbsoluteSlot?.let { CborInteger.create(it) },
                // TODO: Implement posting certificates to chain
                TX_KEY_CERTIFICATES to null,
                // TODO: Implement reward withdrawals
                TX_KEY_WITHDRAWALS to null,
                // TODO: Implement protocol param updates
                TX_KEY_UPDATES to null,
                TX_KEY_AUX_DATA_HASH to auxDataHash?.let { CborByteString.create(it) },
                TX_KEY_VALIDITY_INTERVAL_START to validityIntervalStart?.let { CborInteger.create(it) },
                TX_KEY_MINT to mintTokens?.toNativeAssetCborMap(),
                TX_KEY_SCRIPT_DATA_HASH to scriptDataHash?.let { CborByteString.create(it) },
                TX_KEY_COLLATERAL_INPUTS to collateralUtxos?.toCborObject(cardanoEra),
                TX_KEY_REQUIRED_SIGNERS to
                    requiredSigners.takeUnless { it.isNullOrEmpty() }?.let {
                        CborArray.create(
                            it.map { requiredSigner -> CborByteString.create(requiredSigner) },
                            cardanoEra.toSetTag()
                        )
                    },
                TX_KEY_NETWORK_ID to networkId?.number?.let { CborInteger.create(it) },
                TX_KEY_COLLATERAL_RETURN to collateralReturn?.toCborObject(),
                TX_KEY_TOTAL_COLLATERAL to totalCollateral?.let { CborInteger.create(it) },
                TX_KEY_REFERENCE_INPUTS to referenceInputs?.toCborObject(cardanoEra),
                // TODO: Implement voting procedures
                TX_KEY_VOTING_PROCEDURES to null,
                // TODO: Implement proposal procedures
                TX_KEY_PROPOSAL_PROCEDURES to null,
                // TODO: Implement current treasury value
                TX_KEY_CURRENT_TREASURY_VALUE to null,
                // TODO: Implement donation
                TX_KEY_DONATION to null,
            ).filterValues { it != null }
        )

    private fun createOutputUtxos(): CborObject {
        val changeNativeAssets = mutableListOf<NativeAsset>()
        var changeLovelace = 0L
        sourceUtxos!!.forEach { sourceUtxo ->
            changeLovelace += sourceUtxo.lovelace.toLong()
            changeNativeAssets.addAll(sourceUtxo.nativeAssetsList)
        }

        val changeNativeAssetMap = changeNativeAssets.toNativeAssetMap()

        outputUtxos.orEmpty().forEach { outputUtxo ->
            changeLovelace -= outputUtxo.withMinUtxo(utxoCostPerByte).lovelace.toLong()
            outputUtxo.nativeAssetsList.forEach { nativeAsset ->
                val changePolicyNativeAssets = changeNativeAssetMap[nativeAsset.policy]?.toMutableList()
                changePolicyNativeAssets?.find { it.name == nativeAsset.name }?.let { changeNativeAsset ->
                    val amountRemaining = changeNativeAsset.amount.toBigInteger() - nativeAsset.amount.toBigInteger()
                    if (amountRemaining < BigInteger.ZERO) {
                        throw IllegalStateException("amountRemaining for ${nativeAsset.policy}.${nativeAsset.name} == $amountRemaining")
                    } else if (amountRemaining == BigInteger.ZERO) {
                        changePolicyNativeAssets.remove(changeNativeAsset)
                        if (changePolicyNativeAssets.isEmpty()) {
                            changeNativeAssetMap.remove(nativeAsset.policy)
                        } else {
                            changeNativeAssetMap[nativeAsset.policy] = changePolicyNativeAssets
                        }
                    } else {
                        val index = changePolicyNativeAssets.indexOf(changeNativeAsset)
                        changePolicyNativeAssets[index] =
                            changeNativeAsset.toBuilder().setAmount(amountRemaining.toString()).build()
                        changeNativeAssetMap[nativeAsset.policy] = changePolicyNativeAssets
                    }
                }
            }
        }

        changeLovelace -= (fee ?: DUMMY_TX_FEE)

        val changeUtxos =
            if (changeLovelace > 0) {
                listOf(
                    OutputUtxo
                        .newBuilder()
                        .apply {
                            address = changeAddress
                            lovelace = changeLovelace.toString()
                            if (changeNativeAssetMap.isNotEmpty()) {
                                addAllNativeAssets(changeNativeAssetMap.values.flatten())
                            }
                        }.build()
                )
            } else {
                emptyList()
            }

        return CborArray.create(
            (outputUtxos.orEmpty() + changeUtxos)
                .map { outputUtxo -> outputUtxo.withMinUtxo(utxoCostPerByte) }
                .map { it.toCborObject() }
        )
    }

    private fun createTxWitnessSet(isFeeCalculation: Boolean = false): CborObject =
        CborMap.create(
            mapOf<CborObject, CborObject?>(
                WITNESS_SET_KEY_VKEYWITNESS to createVKeyWitnesses(isFeeCalculation),
                WITNESS_SET_KEY_NATIVE_SCRIPT to createNativeScriptWitnesses(),
                // TODO: implement bootstrap witness
                WITNESS_SET_KEY_BOOTSTRAP_WITNESS to null,
                WITNESS_SET_KEY_PLUTUS_V1_SCRIPT to createPlutusV1ScriptWitnesses(),
                WITNESS_SET_KEY_PLUTUS_DATA to createDatumWitnesses(),
                WITNESS_SET_KEY_REDEEMER to createRedeemerWitnesses(),
                WITNESS_SET_KEY_PLUTUS_V2_SCRIPT to createPlutusV2ScriptWitnesses(),
                WITNESS_SET_KEY_PLUTUS_V3_SCRIPT to createPlutusV3ScriptWitnesses(),
            ).filterValues { it != null }
        )

    private fun createVKeyWitnesses(isFeeCalculation: Boolean): CborObject? {
        val rawSignatures =
            signatures
                ?.map { signature ->
                    CborArray.create(
                        listOf(
                            CborByteString.create(signature.vkey.toByteArray()),
                            CborByteString.create(signature.sig.toByteArray()),
                        )
                    )
                }.orEmpty()

        val keySignatures =
            signingKeys
                ?.map { signingKey ->
                    CborArray.create(
                        listOf(
                            CborByteString.create(signingKey.vkey.toByteArray()),
                            CborByteString.create(
                                signingKey.sign(_transactionId)
                            )
                        )
                    )
                }.orEmpty()

        val requiredSignerDummySignatures =
            if (isFeeCalculation && rawSignatures.isEmpty() && keySignatures.isEmpty()) {
                // We have no signatures. Use dummy signatures to calculate the fee.
                requiredSigners
                    ?.map { _ ->
                        CborArray.create(
                            listOf(
                                CborByteString.create(ByteArray(32) { 0 }),
                                CborByteString.create(ByteArray(64) { 0 }),
                            )
                        )
                    }.orEmpty()
            } else {
                emptyList()
            }

        return (rawSignatures + keySignatures + requiredSignerDummySignatures).takeUnless { it.isEmpty() }?.let {
            CborArray.create(it, cardanoEra.toSetTag())
        }
    }

    private fun createNativeScriptWitnesses(): CborObject? =
        nativeScripts.takeUnless { it.isNullOrEmpty() }?.let {
            CborArray.create(
                it.map { nativeScript -> nativeScript.toCborObject() },
                cardanoEra.toSetTag(),
            )
        }

    private fun createPlutusV1ScriptWitnesses(): CborObject? =
        plutusV1Scripts.takeUnless { it.isNullOrEmpty() }?.let {
            CborArray.create(
                it.map { plutusV1Script -> CborByteString.create(plutusV1Script) },
                cardanoEra.toSetTag(),
            )
        }

    private fun createPlutusV2ScriptWitnesses(): CborObject? =
        plutusV2Scripts.takeUnless { it.isNullOrEmpty() }?.let {
            CborArray.create(
                it.map { plutusV2Script -> CborByteString.create(plutusV2Script) },
                cardanoEra.toSetTag(),
            )
        }

    private fun createPlutusV3ScriptWitnesses(): CborObject? =
        plutusV3Scripts.takeUnless { it.isNullOrEmpty() }?.let {
            CborArray.create(
                it.map { plutusV3Script -> CborByteString.create(plutusV3Script) },
                cardanoEra.toSetTag(),
            )
        }

    private fun createRedeemerWitnesses(): CborObject? =
        redeemers.takeUnless { it.isNullOrEmpty() }?.let {
            when (cardanoEra) {
                CardanoEra.CONWAY -> {
                    CborMap.create(
                        it.associate { redeemer ->
                            redeemer.toConwayCborObject(
                                maxTxExecutionMemory,
                                maxTxExecutionSteps
                            )
                        }
                    )
                }

                else -> {
                    CborArray.create(
                        it.map { redeemer -> redeemer.toCborObject(maxTxExecutionMemory, maxTxExecutionSteps) }
                    )
                }
            }
        }

    private fun createDatumWitnesses(): CborObject? =
        datums.takeUnless { it.isNullOrEmpty() }?.let {
            CborArray.create(
                it.map { plutusData -> plutusData.toCborObject() },
                cardanoEra.toSetTag(),
            )
        }

    fun loadFrom(request: TransactionBuilderRequest) {
        sourceUtxos {
            addAll(request.sourceUtxosList)
        }

        outputUtxos {
            addAll(request.outputUtxosList)
        }

        signingKeys {
            addAll(request.signingKeysList)
        }

        signatures {
            addAll(request.signaturesList)
        }

        mintTokens {
            addAll(request.mintTokensList)
        }

        referenceInputs {
            addAll(request.referenceInputsList)
        }

        nativeScripts {
            addAll(request.nativeScriptsList)
        }

        plutusV1Scripts {
            addAll(request.plutusV1ScriptsList.map { it.toByteArray() })
        }

        plutusV2Scripts {
            addAll(request.plutusV2ScriptsList.map { it.toByteArray() })
        }

        plutusV3Scripts {
            addAll(request.plutusV3ScriptsList.map { it.toByteArray() })
        }

        auxNativeScripts {
            addAll(request.auxNativeScriptsList)
        }

        auxPlutusV1Scripts {
            addAll(request.auxPlutusV1ScriptsList.map { it.toByteArray() })
        }

        auxPlutusV2Scripts {
            addAll(request.auxPlutusV2ScriptsList.map { it.toByteArray() })
        }

        auxPlutusV3Scripts {
            addAll(request.auxPlutusV3ScriptsList.map { it.toByteArray() })
        }

        collateralUtxos {
            addAll(request.collateralUtxosList)
        }

        requiredSigners {
            addAll(request.requiredSignersList.map { it.toByteArray() })
        }

        redeemers {
            addAll(request.redeemersList)
        }

        datums {
            addAll(request.datumsList)
        }

        if (request.hasChangeAddress()) {
            changeAddress = request.changeAddress
        }

        if (request.hasFee()) {
            fee = request.fee
        }

        if (request.hasTtlAbsoluteSlot()) {
            ttlAbsoluteSlot = request.ttlAbsoluteSlot
        }

        if (request.hasAuxDataHash()) {
            auxDataHash = request.auxDataHash.toByteArray()
        }

        if (request.hasValidityIntervalStart()) {
            validityIntervalStart = request.validityIntervalStart
        }

        if (request.hasScriptDataHash()) {
            scriptDataHash = request.scriptDataHash.toByteArray()
        }

        if (request.hasNetworkId()) {
            networkId = request.networkId
        }

        if (request.hasCollateralReturnAddress()) {
            collateralReturnAddress = request.collateralReturnAddress
        }

        if (request.hasCollateralReturn()) {
            collateralReturn = request.collateralReturn
        }

        if (request.hasTotalCollateral()) {
            totalCollateral = request.totalCollateral
        }

        if (request.hasTransactionMetadataCbor()) {
            transactionMetadata =
                CborReader.createFromByteArray(request.transactionMetadataCbor.toByteArray()).readDataItem() as CborMap
        }
    }

    companion object {
        suspend fun transactionBuilder(
            protocolParameters: ProtocolParametersResult,
            cardanoEra: CardanoEra = CardanoEra.BABBAGE,
            calculateTxExecutionUnits: (suspend (ByteArray) -> EvaluateTxResult)? = null,
            calculateReferenceScriptBytes: (suspend (Set<Utxo>) -> Long)? = null,
            block: TransactionBuilder.() -> Unit
        ): Pair<String, ByteArray> {
            val transactionBuilder = TransactionBuilder(
                protocolParameters,
                cardanoEra,
                calculateTxExecutionUnits,
                calculateReferenceScriptBytes,
            )
            block.invoke(transactionBuilder)
            val cborBytes = transactionBuilder.build()
            return Pair(transactionBuilder.transactionId, cborBytes)
        }

        private val TX_KEY_UTXO_INPUTS by lazy { CborInteger.create(0) }
        private val TX_KEY_UTXO_OUTPUTS by lazy { CborInteger.create(1) }
        private val TX_KEY_FEE by lazy { CborInteger.create(2) }
        private val TX_KEY_TTL by lazy { CborInteger.create(3) }
        private val TX_KEY_CERTIFICATES by lazy { CborInteger.create(4) }
        private val TX_KEY_WITHDRAWALS by lazy { CborInteger.create(5) }
        private val TX_KEY_UPDATES by lazy { CborInteger.create(6) }
        private val TX_KEY_AUX_DATA_HASH by lazy { CborInteger.create(7) }
        private val TX_KEY_VALIDITY_INTERVAL_START by lazy { CborInteger.create(8) }
        private val TX_KEY_MINT by lazy { CborInteger.create(9) }
        private val TX_KEY_SCRIPT_DATA_HASH by lazy { CborInteger.create(11) }
        private val TX_KEY_COLLATERAL_INPUTS by lazy { CborInteger.create(13) }
        private val TX_KEY_REQUIRED_SIGNERS by lazy { CborInteger.create(14) }
        private val TX_KEY_NETWORK_ID by lazy { CborInteger.create(15) }
        private val TX_KEY_COLLATERAL_RETURN by lazy { CborInteger.create(16) }
        private val TX_KEY_TOTAL_COLLATERAL by lazy { CborInteger.create(17) }
        private val TX_KEY_REFERENCE_INPUTS by lazy { CborInteger.create(18) }
        private val TX_KEY_VOTING_PROCEDURES by lazy { CborInteger.create(19) }
        private val TX_KEY_PROPOSAL_PROCEDURES by lazy { CborInteger.create(20) }
        private val TX_KEY_CURRENT_TREASURY_VALUE by lazy { CborInteger.create(21) }
        private val TX_KEY_DONATION by lazy { CborInteger.create(22) }

        internal val UTXO_OUTPUT_KEY_ADDRESS by lazy { CborInteger.create(0) }
        internal val UTXO_OUTPUT_KEY_AMOUNT by lazy { CborInteger.create(1) }
        internal val UTXO_OUTPUT_KEY_DATUM by lazy { CborInteger.create(2) }
        internal val UTXO_OUTPUT_KEY_SCRIPTREF by lazy { CborInteger.create(3) }

        private val WITNESS_SET_KEY_VKEYWITNESS by lazy { CborInteger.create(0) }
        private val WITNESS_SET_KEY_NATIVE_SCRIPT by lazy { CborInteger.create(1) }
        private val WITNESS_SET_KEY_BOOTSTRAP_WITNESS by lazy { CborInteger.create(2) }
        private val WITNESS_SET_KEY_PLUTUS_V1_SCRIPT by lazy { CborInteger.create(3) }
        private val WITNESS_SET_KEY_PLUTUS_DATA by lazy { CborInteger.create(4) }
        private val WITNESS_SET_KEY_REDEEMER by lazy { CborInteger.create(5) }
        private val WITNESS_SET_KEY_PLUTUS_V2_SCRIPT by lazy { CborInteger.create(6) }
        private val WITNESS_SET_KEY_PLUTUS_V3_SCRIPT by lazy { CborInteger.create(7) }

        private val AUX_DATA_KEY_METADATA by lazy { CborInteger.create(0) }
        private val AUX_DATA_KEY_NATIVE_SCRIPT by lazy { CborInteger.create(1) }
        private val AUX_DATA_KEY_PLUTUS_V1_SCRIPT by lazy { CborInteger.create(2) }
        private val AUX_DATA_KEY_PLUTUS_V2_SCRIPT by lazy { CborInteger.create(3) }
        private val AUX_DATA_KEY_PLUTUS_V3_SCRIPT by lazy { CborInteger.create(4) }

        internal val DATUM_KEY_HASH by lazy { CborInteger.create(0) }
        internal val DATUM_KEY_INLINE by lazy { CborInteger.create(1) }

        internal const val SCRIPT_REF_TAG = 24
        internal const val INLINE_DATUM_TAG = 24

        internal const val AUX_DATA_TAG = 259

        private const val DUMMY_TX_FEE = 2000000L // 2 ada
        private const val DUMMY_TOTAL_COLLATERAL = 4000000L // 4 ada
    }
}
