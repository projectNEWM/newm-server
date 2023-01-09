package io.newm.chain.database.repository

import io.newm.chain.database.entity.Key

interface KeysRepository {
    fun get(id: Long): Key?

    fun findByAddress(address: String): Key?

    fun findByScriptAddress(scriptAddress: String): Key?

    fun findByTxId(transactionId: String): List<Key>

    fun insert(key: Key): Long

    fun updateScriptAndScriptAddress(key: Key): Int
}
