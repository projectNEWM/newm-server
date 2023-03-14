package io.newm.txbuilder

import com.google.common.annotations.VisibleForTesting
import com.google.iot.cbor.CborArray
import com.google.iot.cbor.CborByteString
import com.google.iot.cbor.CborInteger
import com.google.iot.cbor.CborMap
import com.google.iot.cbor.CborObject
import com.google.iot.cbor.CborSimple
import io.newm.chain.grpc.NativeAsset
import io.newm.chain.grpc.NativeScript
import io.newm.chain.grpc.OutputUtxo
import io.newm.chain.grpc.PlutusData
import io.newm.chain.grpc.Redeemer
import io.newm.chain.grpc.Signature
import io.newm.chain.grpc.SigningKey
import io.newm.chain.grpc.Utxo
import io.newm.chain.grpc.exUnits
import io.newm.chain.grpc.outputUtxo
import io.newm.chain.grpc.redeemer
import io.newm.chain.util.Blake2b
import io.newm.chain.util.NetworkId
import io.newm.chain.util.toHexString
import io.newm.kogmios.protocols.messages.EvaluationResult
import io.newm.kogmios.protocols.model.QueryCurrentProtocolBabbageParametersResult
import io.newm.txbuilder.ktx.sign
import io.newm.txbuilder.ktx.toCborObject
import io.newm.txbuilder.ktx.toNativeAssetCborMap
import io.newm.txbuilder.ktx.toNativeAssetMap
import io.newm.txbuilder.ktx.toRedeemerTagAndIndex
import io.newm.txbuilder.ktx.withMinUtxo
import java.math.BigDecimal
import java.security.SecureRandom
import kotlin.math.ceil

/**
 * A builder for cardano babbage transactions.
 * reference: https://github.com/input-output-hk/cardano-ledger/blob/master/eras/babbage/test-suite/cddl-files/babbage.cddl
 */
class TransactionBuilder(
    private val protocolParameters: QueryCurrentProtocolBabbageParametersResult,
    private val calculateTxExecutionUnits: (suspend (ByteArray) -> EvaluationResult)? = null,
) {
    private val secureRandom by lazy { SecureRandom() }

    private val txFeeFixed by lazy { protocolParameters.minFeeConstant.toLong() }
    private val txFeePerByte by lazy { protocolParameters.minFeeCoefficient.toLong() }
    private val utxoCostPerByte by lazy { protocolParameters.coinsPerUtxoByte.toLong() }

    private var sourceUtxos: MutableList<Utxo>? = null
    private var outputUtxos: MutableList<OutputUtxo>? = null
    private var signingKeys: MutableList<SigningKey>? = null
    private var signatures: MutableList<Signature>? = null
    private var mintTokens: MutableList<NativeAsset>? = null
    private var referenceInputs: MutableList<Utxo>? = null
    private var nativeScripts: MutableList<NativeScript>? = null
    private var plutusV1Scripts: MutableList<ByteArray>? = null
    private var plutusV2Scripts: MutableList<ByteArray>? = null
    private var auxNativeScripts: MutableList<NativeScript>? = null
    private var auxPlutusV1Scripts: MutableList<ByteArray>? = null
    private var auxPlutusV2Scripts: MutableList<ByteArray>? = null
    private var collateralUtxos: MutableList<Utxo>? = null
    private var requiredSigners: MutableList<ByteArray>? = null
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

    fun sourceUtxos(block: MutableList<Utxo>.() -> Unit) {
        if (this.sourceUtxos == null) {
            this.sourceUtxos = mutableListOf()
        }
        block.invoke(this.sourceUtxos!!)
    }

    fun outputUtxos(block: MutableList<OutputUtxo>.() -> Unit) {
        if (this.outputUtxos == null) {
            this.outputUtxos = mutableListOf()
        }
        block.invoke(this.outputUtxos!!)
    }

    fun signingKeys(block: MutableList<SigningKey>.() -> Unit) {
        if (this.signingKeys == null) {
            this.signingKeys = mutableListOf()
        }
        block.invoke(this.signingKeys!!)
    }

    fun signatures(block: MutableList<Signature>.() -> Unit) {
        if (this.signatures == null) {
            this.signatures = mutableListOf()
        }
        block.invoke(this.signatures!!)
    }

    fun mintTokens(block: MutableList<NativeAsset>.() -> Unit) {
        if (this.mintTokens == null) {
            this.mintTokens = mutableListOf()
        }
        block.invoke(this.mintTokens!!)
    }

    fun collateralUtxos(block: MutableList<Utxo>.() -> Unit) {
        if (this.collateralUtxos == null) {
            this.collateralUtxos = mutableListOf()
        }
        block.invoke(this.collateralUtxos!!)
    }

    fun requiredSigners(block: MutableList<ByteArray>.() -> Unit) {
        if (this.requiredSigners == null) {
            this.requiredSigners = mutableListOf()
        }
        block.invoke(this.requiredSigners!!)
    }

    fun referenceInputs(block: MutableList<Utxo>.() -> Unit) {
        if (this.referenceInputs == null) {
            this.referenceInputs = mutableListOf()
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
        createScriptDataHash()
        calculateTemporaryCollateral()
        calculateTxFees(auxData)

        // assemble the final transaction now that fees and collateral are correct
        val txBody = createTxBody()
        _transactionId = Blake2b.hash256(txBody.toCborByteArray())
        return CborArray.create().apply {
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
        if (signingKeys == null) {
            require(!signatures.isNullOrEmpty()) { "signingKeys or signatures must be defined!" }
        }
        if (signatures == null) {
            require(!signingKeys.isNullOrEmpty()) { "signingKeys or signatures must be defined!" }
        }
        if (!redeemers.isNullOrEmpty() || !referenceInputs.isNullOrEmpty()) {
            // There's some type of smart contract here. We must have collateral defined
            require(!collateralUtxos.isNullOrEmpty()) { "collateralUtxos must be defined!" }
            collateralUtxos?.forEach {
                require(it.lovelace.isNotBlank()) { "collateralUtxos must have lovelace defined!" }
            }
        }
    }

    private fun updateFeesAndCollateral(cborByteSize: Int, computationalFee: Long?) {
        fee = (txFeeFixed + txFeePerByte * cborByteSize) + (computationalFee ?: 0L)
        computationalFee?.let {
            totalCollateral = ceil(protocolParameters.collateralPercentage.toLong() * fee!! / 100.0).toLong()

            // fix collateralReturn now that we have accurate totalCollateral
            val collateralReturnLovelace = collateralUtxos!!.sumOf { it.lovelace.toLong() } - totalCollateral!!
            collateralReturn = outputUtxo {
                address = collateralReturnAddress!!
                lovelace = collateralReturnLovelace.toString()
                nativeAssets.addAll(
                    collateralUtxos!!.map { it.nativeAssetsList }.flatten().toNativeAssetMap().values.flatten()
                )
            }
        }
    }

    @VisibleForTesting
    internal fun createAuxData(): CborObject? {
        return mapOf<CborObject, CborObject?>(
            // Metadata
            AUX_DATA_KEY_METADATA to transactionMetadata,
            AUX_DATA_KEY_NATIVE_SCRIPT to auxNativeScripts.takeUnless { it.isNullOrEmpty() }?.let {
                CborArray.create(it.map { nativeScript -> nativeScript.toCborObject() })
            },
            AUX_DATA_KEY_PLUTUS_V1_SCRIPT to auxPlutusV1Scripts.takeUnless { it.isNullOrEmpty() }?.let {
                CborArray.create(it.map { plutusV1Script -> CborByteString.create(plutusV1Script) })
            },
            AUX_DATA_KEY_PLUTUS_V2_SCRIPT to auxPlutusV2Scripts.takeUnless { it.isNullOrEmpty() }?.let {
                CborArray.create(it.map { plutusV2Script -> CborByteString.create(plutusV2Script) })
            }
        ).filterValues { it != null }.ifEmpty { null }?.let {
            CborMap.create(it, AUX_DATA_TAG).also { auxData ->
                auxDataHash = Blake2b.hash256(auxData.toCborByteArray())
            }
        }
    }

    private fun createScriptDataHash() {
        if (scriptDataHash == null && (!redeemers.isNullOrEmpty() || !datums.isNullOrEmpty())) {
            // calculate the scriptDataHash - // redeemerBytes + datumBytes + languageViewMap
            val redeemerBytes = createRedeemerWitnesses()?.toCborByteArray() ?: ByteArray(1) { 0x80.toByte() }
            val datumBytes = createDatumWitnesses()?.toCborByteArray() ?: ByteArray(0)
            val languageViewMap = if (!redeemers.isNullOrEmpty()) {
                protocolParameters.costModels.plutusV2!!.toCborObject().toCborByteArray()
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
            collateralReturn = outputUtxo {
                address = collateralReturnAddress!!
                lovelace = collateralReturnLovelace.toString()
                nativeAssets.addAll(
                    collateralUtxos!!.map { it.nativeAssetsList }.flatten().toNativeAssetMap().values.flatten()
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

            var dummyTxCbor = CborArray.create().apply {
                add(txBody)
                add(createTxWitnessSet())
                add(CborSimple.TRUE)
                add(auxData ?: CborSimple.NULL)
            }.toCborByteArray()

            // Calculate minimum transaction fee
            val maxComputationalFee = if (totalCollateral != null) {
                ceil((protocolParameters.prices.memory * protocolParameters.maxExecutionUnitsPerTransaction.memory + protocolParameters.prices.steps * protocolParameters.maxExecutionUnitsPerTransaction.steps).toDouble()).toLong()
            } else {
                null
            }
            updateFeesAndCollateral(dummyTxCbor.size, maxComputationalFee)

            if (totalCollateral != null && calculateTxExecutionUnits != null) {
                // calculate actual totalCollateral
                val evaluationResult = calculateTxExecutionUnits.invoke(dummyTxCbor)

                var totalMemory = BigDecimal.ZERO
                var totalSteps = BigDecimal.ZERO
                evaluationResult.forEach { (redeemerAction, executionUnits) ->
                    totalMemory += executionUnits.memory
                    totalSteps += executionUnits.steps
                    val (redeemerTag, idx) = redeemerAction.toRedeemerTagAndIndex()
                    redeemers?.replaceAll {
                        if (it.tag == redeemerTag && it.index == idx) {
                            // Update with new executionUnits
                            redeemer {
                                tag = redeemerTag
                                index = idx
                                data = it.data
                                exUnits = exUnits {
                                    mem = executionUnits.memory.toLong()
                                    steps = executionUnits.steps.toLong()
                                }
                            }
                        } else {
                            // Leave unchanged
                            it
                        }
                    }
                }

                val computationalFee: Long =
                    ceil((protocolParameters.prices.memory * totalMemory + protocolParameters.prices.steps * totalSteps).toDouble()).toLong()
                updateFeesAndCollateral(dummyTxCbor.size, computationalFee)

                txBody = createTxBody()
                // set a random _transactionId for dummy signing
                _transactionId = ByteArray(32)
                secureRandom.nextBytes(_transactionId)

                dummyTxCbor = CborArray.create().apply {
                    add(txBody)
                    add(createTxWitnessSet())
                    add(CborSimple.TRUE)
                    add(auxData ?: CborSimple.NULL)
                }.toCborByteArray()

                updateFeesAndCollateral(dummyTxCbor.size, computationalFee)
            }
        }
    }

    private fun createTxBody(): CborObject {
        return CborMap.create(
            mapOf<CborObject, CborObject?>(
                // Utxo inputs
                TX_KEY_UTXO_INPUTS to sourceUtxos!!.toCborObject(),
                // Utxo outputs
                TX_KEY_UTXO_OUTPUTS to createOutputUtxos(),
                TX_KEY_FEE to (fee?.let { CborInteger.create(it) } ?: CborInteger.create(3000000L)),
                TX_KEY_TTL to ttlAbsoluteSlot?.let { CborInteger.create(it) },
                TX_KEY_CERTIFICATES to null, // TODO: Implement posting certificates to chain
                TX_KEY_WITHDRAWALS to null, // TODO: Implement reward withdrawals
                TX_KEY_UPDATES to null, // TODO: Implement protocol param updates
                TX_KEY_AUX_DATA_HASH to auxDataHash?.let { CborByteString.create(it) },
                TX_KEY_VALIDITY_INTERVAL_START to validityIntervalStart?.let { CborInteger.create(it) },
                TX_KEY_MINT to mintTokens?.toNativeAssetCborMap(),
                TX_KEY_SCRIPT_DATA_HASH to scriptDataHash?.let { CborByteString.create(it) },
                TX_KEY_COLLATERAL_INPUTS to collateralUtxos?.toCborObject(),
                TX_KEY_REQUIRED_SIGNERS to requiredSigners.takeUnless { it.isNullOrEmpty() }?.let {
                    CborArray.create(
                        it.map { requiredSigner -> CborByteString.create(requiredSigner) }
                    )
                },
                TX_KEY_NETWORK_ID to networkId?.value?.let { CborInteger.create(it) },
                TX_KEY_COLLATERAL_RETURN to collateralReturn?.toCborObject(),
                TX_KEY_TOTAL_COLLATERAL to totalCollateral?.let { CborInteger.create(it) },
                TX_KEY_REFERENCE_INPUTS to referenceInputs?.toCborObject(),
            ).filterValues { it != null }
        )
    }

    private fun createOutputUtxos(): CborObject {
        val changeNativeAssets = mutableListOf<NativeAsset>()
        var changeLovelace = 0L
        sourceUtxos!!.forEach { sourceUtxo ->
            changeLovelace += sourceUtxo.lovelace.toLong()
            changeNativeAssets.addAll(sourceUtxo.nativeAssetsList)
        }

        val changeNativeAssetMap = changeNativeAssets.toNativeAssetMap()

        outputUtxos!!.forEach { outputUtxo ->
            changeLovelace -= outputUtxo.withMinUtxo(utxoCostPerByte).lovelace.toLong()
            outputUtxo.nativeAssetsList.forEach { nativeAsset ->
                val changePolicyNativeAssets = changeNativeAssetMap[nativeAsset.policy]?.toMutableList()
                changePolicyNativeAssets?.find { it.name == nativeAsset.name }?.let { changeNativeAsset ->
                    val amountRemaining = changeNativeAsset.amount.toLong() - nativeAsset.amount.toLong()
                    if (amountRemaining < 0) {
                        throw IllegalStateException("amountRemaining for ${nativeAsset.policy}.${nativeAsset.name} == $amountRemaining")
                    } else if (amountRemaining == 0L) {
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
                    }
                }
            }
        }

        if (fee != null) {
            changeLovelace -= fee!!
        }

        val changeUtxos = if (changeLovelace > 0) {
            listOf(
                OutputUtxo.newBuilder().apply {
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
            (outputUtxos!! + changeUtxos)
                .map { outputUtxo -> outputUtxo.withMinUtxo(utxoCostPerByte) }
                .map { it.toCborObject() }
        )
    }

    private fun createTxWitnessSet(): CborObject {
        return CborMap.create(
            mapOf<CborObject, CborObject?>(
                WITNESS_SET_KEY_VKEYWITNESS to createVKeyWitnesses(),
                WITNESS_SET_KEY_NATIVE_SCRIPT to createNativeScriptWitnesses(),
                WITNESS_SET_KEY_BOOTSTRAP_WITNESS to null, // TODO: implement
                WITNESS_SET_KEY_PLUTUS_V1_SCRIPT to createPlutusV1ScriptWitnesses(),
                WITNESS_SET_KEY_PLUTUS_DATA to createDatumWitnesses(),
                WITNESS_SET_KEY_REDEEMER to createRedeemerWitnesses(),
                WITNESS_SET_KEY_PLUTUS_V2_SCRIPT to createPlutusV2ScriptWitnesses(),
            ).filterValues { it != null }
        )
    }

    private fun createVKeyWitnesses(): CborObject? {
        val rawSignatures = signatures?.map { signature ->
            CborArray.create(
                listOf(
                    CborByteString.create(signature.pkh.toByteArray()),
                    CborByteString.create(signature.sig.toByteArray()),
                )
            )
        }.orEmpty()

        val keySignatures = signingKeys?.map { signingKey ->
            CborArray.create(
                listOf(
                    CborByteString.create(signingKey.vkey.toByteArray()),
                    CborByteString.create(
                        signingKey.sign(_transactionId)
                    )
                )
            )
        }.orEmpty()

        return (rawSignatures + keySignatures).takeUnless { it.isEmpty() }?.let {
            CborArray.create(it)
        }
    }

    private fun createNativeScriptWitnesses(): CborObject? {
        return nativeScripts.takeUnless { it.isNullOrEmpty() }?.let {
            CborArray.create(it.map { nativeScript -> nativeScript.toCborObject() })
        }
    }

    private fun createPlutusV1ScriptWitnesses(): CborObject? {
        return plutusV1Scripts.takeUnless { it.isNullOrEmpty() }?.let {
            CborArray.create(it.map { plutusV1Script -> CborByteString.create(plutusV1Script) })
        }
    }

    private fun createPlutusV2ScriptWitnesses(): CborObject? {
        return plutusV2Scripts.takeUnless { it.isNullOrEmpty() }?.let {
            CborArray.create(it.map { plutusV2Script -> CborByteString.create(plutusV2Script) })
        }
    }

    private fun createRedeemerWitnesses(): CborObject? {
        return redeemers.takeUnless { it.isNullOrEmpty() }?.let {
            CborArray.create(it.map { redeemer -> redeemer.toCborObject() })
        }
    }

    private fun createDatumWitnesses(): CborObject? {
        return datums.takeUnless { it.isNullOrEmpty() }?.let {
            CborArray.create(it.map { plutusData -> plutusData.toCborObject() })
        }
    }

    companion object {
        suspend fun transactionBuilder(
            protocolParameters: QueryCurrentProtocolBabbageParametersResult,
            calculateTxExecutionUnits: (suspend (ByteArray) -> EvaluationResult)? = null,
            block: TransactionBuilder.() -> Unit
        ): ByteArray {
            val transactionBuilder = TransactionBuilder(protocolParameters, calculateTxExecutionUnits)
            block.invoke(transactionBuilder)
            return transactionBuilder.build()
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

        private val AUX_DATA_KEY_METADATA by lazy { CborInteger.create(0) }
        private val AUX_DATA_KEY_NATIVE_SCRIPT by lazy { CborInteger.create(1) }
        private val AUX_DATA_KEY_PLUTUS_V1_SCRIPT by lazy { CborInteger.create(2) }
        private val AUX_DATA_KEY_PLUTUS_V2_SCRIPT by lazy { CborInteger.create(3) }

        internal val DATUM_KEY_HASH by lazy { CborInteger.create(0) }
        internal val DATUM_KEY_INLINE by lazy { CborInteger.create(1) }

        internal const val SCRIPT_REF_TAG = 24
        internal const val INLINE_DATUM_TAG = 24

        internal const val AUX_DATA_TAG = 259

        private val DUMMY_TOTAL_COLLATERAL = 4000000L // 4 ada
    }
}
