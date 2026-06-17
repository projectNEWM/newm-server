package io.newm.chain.database.repository

import com.google.common.truth.Truth.assertThat
import com.zaxxer.hikari.HikariDataSource
import java.util.UUID
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.testcontainers.containers.PostgreSQLContainer
import io.newm.chain.database.table.ChainTable
import io.newm.chain.database.table.LedgerAssetsTable
import io.newm.chain.database.table.LedgerTable
import io.newm.chain.database.table.LedgerUtxoAssetsTable
import io.newm.chain.database.table.LedgerUtxosTable

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class QueryTransactionInfoRepositoryTest {
    private val repository = LedgerRepositoryImpl()
    private val container =
        PostgreSQLContainer("postgres:12").apply {
            withDatabaseName("newm-chain-db")
            withUsername("tester")
            withPassword("newm1234")
        }

    private lateinit var hikariDataSource: HikariDataSource

    @BeforeAll
    fun beforeAll() {
        container.start()
        hikariDataSource = HikariDataSource().apply {
            driverClassName = container.driverClassName
            jdbcUrl = container.jdbcUrl
            username = container.username
            password = container.password
        }
        Database.connect(hikariDataSource)
        transaction {
            SchemaUtils.create(
                ChainTable,
                LedgerTable,
                LedgerAssetsTable,
                LedgerUtxosTable,
                LedgerUtxoAssetsTable,
            )
        }
    }

    @AfterAll
    fun afterAll() {
        hikariDataSource.close()
        container.stop()
    }

    @BeforeEach
    fun beforeEach() {
        transaction {
            exec(
                "TRUNCATE ${listOf(LedgerUtxoAssetsTable, LedgerUtxosTable, LedgerAssetsTable, LedgerTable, ChainTable).joinToString(", ") { it.tableName }} RESTART IDENTITY CASCADE"
            )
        }
    }

    @Test
    fun `queryTransactionInfo returns block slot spent and created utxos`() {
        insertChainBlock(blockNumber = 100L, slotNumber = 1000L)
        insertLedgerUtxo(
            address = "addr_test_source1",
            txId = "source-1",
            txIx = 0,
            lovelace = "1000",
            blockCreated = 90L,
            blockSpent = 100L,
            transactionSpent = "target-tx",
        )
        insertLedgerUtxo(
            address = "addr_test_source2",
            txId = "source-2",
            txIx = 1,
            lovelace = "2000",
            blockCreated = 91L,
            blockSpent = 100L,
            transactionSpent = "target-tx",
        )
        insertLedgerUtxo(
            address = "addr_test_dest1",
            txId = "target-tx",
            txIx = 0,
            lovelace = "2500",
            blockCreated = 100L,
        )
        insertLedgerUtxo(
            address = "addr_test_dest2",
            txId = "target-tx",
            txIx = 1,
            lovelace = "500",
            blockCreated = 100L,
        )

        val result = repository.queryTransactionInfo("target-tx")

        assertThat(result).isNotNull()
        assertThat(result!!.blockNumber).isEqualTo(100L)
        assertThat(result.slotNumber).isEqualTo(1000L)
        assertThat(result.spentUtxos.map { utxo -> utxo.hash to utxo.ix })
            .containsExactly("source-1" to 0L, "source-2" to 1L)
        assertThat(result.createdUtxos.map { utxo -> utxo.hash to utxo.ix })
            .containsExactly("target-tx" to 0L, "target-tx" to 1L)
    }

    @Test
    fun `queryTransactionInfo returns null when no retained rows exist`() {
        assertThat(repository.queryTransactionInfo("missing-tx")).isNull()
    }

    @Test
    fun `queryTransactionInfo fails when created utxos span multiple blocks`() {
        insertChainBlock(blockNumber = 100L, slotNumber = 1000L)
        insertChainBlock(blockNumber = 101L, slotNumber = 1001L)
        insertLedgerUtxo(
            address = "addr_test_dest1",
            txId = "target-tx",
            txIx = 0,
            lovelace = "2500",
            blockCreated = 100L,
        )
        insertLedgerUtxo(
            address = "addr_test_dest2",
            txId = "target-tx",
            txIx = 1,
            lovelace = "500",
            blockCreated = 101L,
        )

        assertThrows<TransactionInfoIntegrityException> {
            repository.queryTransactionInfo("target-tx")
        }
    }

    @Test
    fun `queryTransactionInfo fails when spent utxos span multiple blocks`() {
        insertChainBlock(blockNumber = 100L, slotNumber = 1000L)
        insertChainBlock(blockNumber = 101L, slotNumber = 1001L)
        insertLedgerUtxo(
            address = "addr_test_source1",
            txId = "source-1",
            txIx = 0,
            lovelace = "1000",
            blockCreated = 90L,
            blockSpent = 100L,
            transactionSpent = "target-tx",
        )
        insertLedgerUtxo(
            address = "addr_test_source2",
            txId = "source-2",
            txIx = 1,
            lovelace = "2000",
            blockCreated = 91L,
            blockSpent = 101L,
            transactionSpent = "target-tx",
        )

        assertThrows<TransactionInfoIntegrityException> {
            repository.queryTransactionInfo("target-tx")
        }
    }

    @Test
    fun `queryTransactionInfo fails when created and spent utxos disagree on block`() {
        insertChainBlock(blockNumber = 100L, slotNumber = 1000L)
        insertChainBlock(blockNumber = 101L, slotNumber = 1001L)
        insertLedgerUtxo(
            address = "addr_test_source1",
            txId = "source-1",
            txIx = 0,
            lovelace = "1000",
            blockCreated = 90L,
            blockSpent = 101L,
            transactionSpent = "target-tx",
        )
        insertLedgerUtxo(
            address = "addr_test_dest1",
            txId = "target-tx",
            txIx = 0,
            lovelace = "2500",
            blockCreated = 100L,
        )

        assertThrows<TransactionInfoIntegrityException> {
            repository.queryTransactionInfo("target-tx")
        }
    }

    @Test
    fun `queryTransactionInfo fails when chain block is missing`() {
        insertLedgerUtxo(
            address = "addr_test_dest1",
            txId = "target-tx",
            txIx = 0,
            lovelace = "2500",
            blockCreated = 100L,
        )

        assertThrows<TransactionInfoIntegrityException> {
            repository.queryTransactionInfo("target-tx")
        }
    }

    private fun insertChainBlock(
        blockNumber: Long,
        slotNumber: Long,
    ) {
        transaction {
            ChainTable.insert { row ->
                row[ChainTable.blockNumber] = blockNumber
                row[ChainTable.slotNumber] = slotNumber
                row[ChainTable.hash] = UUID.randomUUID().toString()
                row[ChainTable.prevHash] = UUID.randomUUID().toString()
                row[ChainTable.poolId] = "pool1"
                row[ChainTable.etaV] = "eta"
                row[ChainTable.nodeVkey] = "node-vkey"
                row[ChainTable.nodeVrfVkey] = "node-vrf"
                row[ChainTable.blockVrf0] = "block-vrf-0"
                row[ChainTable.blockVrf1] = "block-vrf-1"
                row[ChainTable.etaVrf0] = "eta-vrf-0"
                row[ChainTable.etaVrf1] = "eta-vrf-1"
                row[ChainTable.leaderVrf0] = "leader-vrf-0"
                row[ChainTable.leaderVrf1] = "leader-vrf-1"
                row[ChainTable.blockSize] = 1
                row[ChainTable.blockBodyHash] = "body-hash"
                row[ChainTable.poolOpcert] = "opcert"
                row[ChainTable.sequenceNumber] = 1
                row[ChainTable.kesPeriod] = 1
                row[ChainTable.sigmaSignature] = "sigma"
                row[ChainTable.protocolMajorVersion] = 8
                row[ChainTable.protocolMinorVersion] = 0
            }
        }
    }

    private fun insertLedgerUtxo(
        address: String,
        txId: String,
        txIx: Int,
        lovelace: String,
        blockCreated: Long,
        blockSpent: Long? = null,
        transactionSpent: String? = null,
    ) {
        transaction {
            val ledgerId =
                LedgerTable
                    .insertAndGetId { row ->
                        row[LedgerTable.address] = address
                        row[LedgerTable.stakeAddress] = null
                        row[LedgerTable.addressType] = "00"
                    }.value

            LedgerUtxosTable.insert { row ->
                row[LedgerUtxosTable.ledgerId] = ledgerId
                row[LedgerUtxosTable.txId] = txId
                row[LedgerUtxosTable.txIx] = txIx
                row[LedgerUtxosTable.datumHash] = null
                row[LedgerUtxosTable.datum] = null
                row[LedgerUtxosTable.isInlineDatum] = null
                row[LedgerUtxosTable.scriptRef] = null
                row[LedgerUtxosTable.scriptRefVersion] = null
                row[LedgerUtxosTable.lovelace] = lovelace
                row[LedgerUtxosTable.blockCreated] = blockCreated
                row[LedgerUtxosTable.blockSpent] = blockSpent
                row[LedgerUtxosTable.transactionSpent] = transactionSpent
                row[LedgerUtxosTable.cbor] = null
                row[LedgerUtxosTable.paymentCred] = null
                row[LedgerUtxosTable.stakeCred] = null
            }
        }
    }
}
