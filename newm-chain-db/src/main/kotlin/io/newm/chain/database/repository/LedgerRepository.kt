package io.newm.chain.database.repository

import io.newm.chain.database.entity.LedgerAsset
import io.newm.chain.database.entity.LedgerAssetMetadata
import io.newm.chain.database.entity.RawTransaction
import io.newm.chain.database.entity.StakeDelegation
import io.newm.chain.database.entity.StakeRegistration
import io.newm.chain.model.CreatedUtxo
import io.newm.chain.model.SpentUtxo
import io.newm.chain.model.Utxo

interface LedgerRepository {
    companion object {
        /**
         * How many blocks behind tip do we feel is safe?
         */
        const val STABILITY_WINDOW = 3L
    }

    fun queryUtxos(address: String): Set<Utxo>

    suspend fun queryLiveUtxos(address: String): Set<Utxo>

    suspend fun updateLiveLedgerState(
        transactionId: String,
        cborByteArray: ByteArray
    )

    fun doRollback(blockNumber: Long)

    fun queryLedgerAsset(
        policyId: String,
        hexName: String
    ): LedgerAsset?

    fun queryLedgerAssets(ledgerAssetList: List<LedgerAsset>): List<LedgerAsset>

    fun upcertLedgerAssets(ledgerAssets: List<LedgerAsset>): List<LedgerAsset>

    fun insertLedgerAssetMetadataList(assetMetadataList: List<LedgerAssetMetadata>)

    fun pruneSpent(blockNumber: Long)

    fun spendUtxos(
        blockNumber: Long,
        spentUtxos: Set<SpentUtxo>
    )

    fun createUtxos(
        blockNumber: Long,
        createdUtxos: Set<CreatedUtxo>
    )

    fun createStakeRegistrations(stakeRegistrations: List<StakeRegistration>)

    fun findPointerStakeRegistration(
        slot: Long,
        txIndex: Int,
        certIndex: Int
    ): StakeRegistration?

    fun createStakeDelegations(stakeDelegations: List<StakeDelegation>)

    fun queryPoolLoyalty(
        stakeAddress: String,
        poolId: String,
        currentEpoch: Long
    ): Long

    fun queryAdaHandle(adaHandleName: String): String?

    fun siblingHashCount(hash: String): Long

    fun queryPayerAddress(receivedUtxo: Utxo): String

    fun createRawTransactions(rawTransactions: List<RawTransaction>)

    fun queryAddressForUtxo(
        hash: String,
        ix: Int
    ): String?

    fun queryUtxoHavingAddress(
        address: String,
        hash: String,
        ix: Int
    ): Utxo?

    fun queryAddressTxLogsAfter(
        address: String,
        afterTxId: String?,
        limit: Int,
        offset: Long
    ): List<ByteArray>

    fun queryNativeAssetLogsAfter(
        afterTableId: Long?,
        limit: Int,
        offset: Long
    ): List<Pair<Long, ByteArray>>

    fun queryLedgerAssetMetadataList(
        assetId: Long,
        parentId: Long? = null
    ): List<LedgerAssetMetadata>

    fun queryLedgerAssetMetadataListByNativeAsset(
        name: String,
        policy: String
    ): List<LedgerAssetMetadata>

    fun queryTransactionConfirmationCounts(txIds: List<String>): Map<String, Long>

    fun queryPublicKeyHashByOutputRef(
        hash: String,
        ix: Int
    ): String?

    fun queryDatumByHash(datumHashHex: String): String?

    fun queryUtxosByOutputRef(
        hash: String,
        ix: Int
    ): Set<Utxo>

    suspend fun queryLiveUtxosByOutputRef(
        hash: String,
        ix: Int
    ): Set<Utxo>

    fun queryUtxosByStakeAddress(address: String): Set<Utxo>

    fun queryUtxoByNativeAsset(
        name: String,
        policy: String
    ): Utxo?

    suspend fun queryLiveUtxoByNativeAsset(
        name: String,
        policy: String
    ): Utxo?

    fun snapshotNativeAssets(
        policy: String,
        name: String
    ): Map<String, Long>

    fun createLedgerUtxoHistory(
        createdUtxos: Set<CreatedUtxo>,
        blockNumber: Long
    )

    fun queryUsedAddresses(addresses: List<String>): Set<String>
}
