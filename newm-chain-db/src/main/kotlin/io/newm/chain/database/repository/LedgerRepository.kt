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

    fun queryUtxos(address: String): Set<Utxo>

    suspend fun queryLiveUtxos(address: String): Set<Utxo>

    suspend fun updateLiveLedgerState(transactionId: String, cborByteArray: ByteArray)

    fun doRollback(blockNumber: Long)

    fun queryLedgerAsset(policyId: String, hexName: String): LedgerAsset?

    fun upcertLedgerAssets(ledgerAssets: List<LedgerAsset>): List<LedgerAsset>

    fun insertLedgerAssetMetadataList(assetMetadataList: List<LedgerAssetMetadata>)

    fun pruneSpent(slotNumber: Long)

    fun spendUtxos(slotNumber: Long, blockNumber: Long, spentUtxos: Set<SpentUtxo>)

    fun createUtxos(slotNumber: Long, blockNumber: Long, createdUtxos: Set<CreatedUtxo>)

    fun createStakeRegistrations(stakeRegistrations: List<StakeRegistration>)

    fun findPointerStakeRegistration(slot: Long, txIndex: Int, certIndex: Int): StakeRegistration?

    fun createStakeDelegations(stakeDelegations: List<StakeDelegation>)

    fun queryPoolLoyalty(stakeAddress: String, poolId: String, currentEpoch: Long): Long

    fun queryAdaHandle(adaHandleName: String): String?

    fun siblingHashCount(hash: String): Long

    fun queryPayerAddress(receivedUtxo: Utxo): String

    fun createRawTransactions(rawTransactions: List<RawTransaction>)

    fun queryAddressForUtxo(hash: String, ix: Int): String?

    fun queryUtxoHavingAddress(address: String, hash: String, ix: Int): Utxo?

    fun queryAddressTxLogsAfter(address: String, afterTxId: String?): List<ByteArray>

    fun queryNativeAssetLogsAfter(afterTableId: Long?): List<Pair<Long, ByteArray>>

    fun queryLedgerAssetMetadataList(assetId: Long, parentId: Long? = null): List<LedgerAssetMetadata>

    fun queryTransactionConfirmationCounts(txIds: List<String>): Map<String, Long>

    fun queryPublicKeyHashByOutputRef(hash: String, ix: Int): String?

    fun queryDatumByHash(datumHashHex: String): String?

    fun queryUtxosByOutputRef(hash: String, ix: Int): Set<Utxo>

    fun queryUtxosByStakeAddress(address: String): Set<Utxo>

    fun snapshotNativeAssets(policy: String, name: String): Map<String, Long>

    fun createLedgerUtxoHistory(createdUtxos: Set<CreatedUtxo>, blockNumber: Long)
}
