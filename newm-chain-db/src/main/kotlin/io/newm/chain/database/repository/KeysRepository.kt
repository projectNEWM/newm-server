package io.newm.chain.database.repository

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.LoadingCache
import io.newm.chain.config.Config
import io.newm.chain.database.entity.Key
import io.newm.chain.database.table.KeysTable
import io.newm.chain.database.table.LedgerTable
import io.newm.chain.database.table.LedgerUtxosTable
import io.newm.chain.util.hexToByteArray
import io.newm.chain.util.toHexString
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.security.crypto.encrypt.Encryptors
import java.time.Duration

object KeysRepository {

    private val keyCache: LoadingCache<Long, Key?> = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofHours(1))
        .build<Long, Key?> { id ->
            transaction {
                KeysTable.select { KeysTable.id eq id }.firstOrNull()?.let { row ->
                    val skey = row[KeysTable.skey].let { skey ->
                        if (skey.length == 64) {
                            // unencrypted in the db. encrypt it
                            val unencryptedSKey = skey.hexToByteArray()
                            KeysTable.update({ KeysTable.id eq id }) { row ->
                                row[KeysTable.skey] = encryptSKeyContent(unencryptedSKey)
                            }
                            invalidate(id)
                            invalidate(row[KeysTable.address])
                            unencryptedSKey
                        } else {
                            decryptSKeyContent(skey)
                        }
                    }
                    Key(
                        id = row[KeysTable.id].value,
                        skey = skey,
                        vkey = row[KeysTable.vkey],
                        address = row[KeysTable.address],
                        script = row[KeysTable.script],
                        scriptAddress = row[KeysTable.scriptAddress],
                        created = row[KeysTable.created],
                    )
                }
            }
        }

    fun get(id: Long): Key? = keyCache[id]

    private val addressKeyCache = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofHours(1))
        .build<String, Key?> { address ->
            transaction {
                KeysTable.select { KeysTable.address eq address }.firstOrNull()?.let { row ->
                    Key(
                        id = row[KeysTable.id].value,
                        skey = decryptSKeyContent(row[KeysTable.skey]),
                        vkey = row[KeysTable.vkey],
                        address = row[KeysTable.address],
                        script = row[KeysTable.script],
                        scriptAddress = row[KeysTable.scriptAddress],
                        created = row[KeysTable.created],
                    )
                }
            }
        }

    fun findByAddress(address: String): Key? = addressKeyCache[address]

    fun findByScriptAddress(scriptAddress: String): Key? = transaction {
        KeysTable.select { KeysTable.scriptAddress eq scriptAddress }.firstOrNull()?.let { row ->
            Key(
                id = row[KeysTable.id].value,
                skey = decryptSKeyContent(row[KeysTable.skey]),
                vkey = row[KeysTable.vkey],
                address = row[KeysTable.address],
                script = row[KeysTable.script],
                scriptAddress = row[KeysTable.scriptAddress],
                created = row[KeysTable.created],
            )
        }
    }

    fun findByTxId(transactionId: String): List<Key> = transaction {
        // select k.* from keys k join ledger l on k.address=l.address join ledger_utxos lu on l.id=lu.ledger_id where lu.tx_id='378187f2cb064d6edc76f09c07b58423c3e89f705709e9ad872b5a97a7264a8f' and lu.block_spent is null;
        KeysTable
            .innerJoin(LedgerTable, { address }, { address })
            .innerJoin(LedgerUtxosTable, { LedgerTable.id }, { ledgerId })
            .slice(KeysTable.columns)
            .select { (LedgerUtxosTable.txId eq transactionId) and LedgerUtxosTable.blockSpent.isNull() }
            .map { row ->
                Key(
                    id = row[KeysTable.id].value,
                    skey = decryptSKeyContent(row[KeysTable.skey]),
                    vkey = row[KeysTable.vkey],
                    address = row[KeysTable.address],
                    script = row[KeysTable.script],
                    scriptAddress = row[KeysTable.scriptAddress],
                    created = row[KeysTable.created],
                )
            }.distinct()
    }

    fun insert(key: Key): Long {
        return transaction {
            KeysTable.insert { row ->
                row[skey] = encryptSKeyContent(key.skey)
                row[vkey] = key.vkey
                row[address] = key.address
                row[script] = key.script
                row[scriptAddress] = key.scriptAddress
                row[created] = key.created
            }[KeysTable.id].value
        }
    }

    /**
     * Only used to update to multiSig key so only update those fields
     */
    fun updateScriptAndScriptAddress(key: Key): Int {
        return transaction {
            KeysTable.update({ KeysTable.id eq key.id!! }) { row ->
                row[script] = key.script
                row[scriptAddress] = key.scriptAddress
            }.also {
                invalidate(key.id!!)
            }
        }
    }

    private fun invalidate(id: Long) = keyCache.invalidate(id)
    private fun invalidate(address: String) = addressKeyCache.invalidate(address)

    private fun encryptSKeyContent(bytes: ByteArray): String {
        return ENCRYPTOR.encrypt(bytes).toHexString()
    }

    private fun decryptSKeyContent(ciphertext: String): ByteArray {
        return ENCRYPTOR.decrypt(ciphertext.hexToByteArray())
    }

    private val ENCRYPTOR by lazy { Encryptors.stronger(Config.spendingPassword, Config.S) }
}
