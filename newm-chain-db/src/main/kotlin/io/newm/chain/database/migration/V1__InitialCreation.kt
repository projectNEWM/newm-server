package io.newm.chain.database.migration

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import org.jetbrains.exposed.sql.transactions.transaction

@Suppress("unused")
class V1__InitialCreation : BaseJavaMigration() {
    override fun migrate(context: Context?) {
        transaction {
            execInBatch(
                listOf(
                    // Keys
                    """CREATE TABLE IF NOT EXISTS "keys" ("id" BIGSERIAL PRIMARY KEY, "skey" TEXT NOT NULL, "vkey" TEXT NOT NULL, "address" TEXT NOT NULL, "script" TEXT, "script_address" TEXT, "created" BIGINT NOT NULL)""",
                    """CREATE INDEX IF NOT EXISTS "keys_created_index" ON "keys" (created DESC NULLS LAST)""",
                    """CREATE INDEX IF NOT EXISTS "keys_address_index" ON "keys" (address)""",
                    """CREATE INDEX IF NOT EXISTS "keys_script_address_index" ON "keys" (script_address ASC NULLS LAST)""",
                    // Chain
                    """
                    CREATE TABLE IF NOT EXISTS chain (
                    "id" BIGSERIAL PRIMARY KEY,
                    "block_number" BIGINT NOT NULL,
                    "slot_number" BIGINT NOT NULL,
                    "hash" TEXT NOT NULL,
                    "prev_hash" TEXT NOT NULL,
                    "pool_id" TEXT NOT NULL,
                    "eta_v" TEXT NOT NULL,
                    "node_vkey" TEXT NOT NULL,
                    "node_vrf_vkey" TEXT NOT NULL,
                    "eta_vrf_0" TEXT NOT NULL,
                    "eta_vrf_1" TEXT NOT NULL,
                    "block_vrf_0" text NOT NULL, 
                    "block_vrf_1" TEXT NOT NULL,
                    "leader_vrf_0" TEXT NOT NULL,
                    "leader_vrf_1" TEXT NOT NULL,
                    "block_size" INTEGER NOT NULL,
                    "block_body_hash" TEXT NOT NULL,
                    "pool_opcert" TEXT NOT NULL,
                    "sequence_number" INTEGER NOT NULL,
                    "kes_period" INTEGER NOT NULL,
                    "sigma_signature" TEXT NOT NULL,
                    "protocol_major_version" INTEGER NOT NULL,
                    "protocol_minor_version" INTEGER NOT NULL,
                    "created" BIGINT NOT NULL,
                    "processed" BOOLEAN DEFAULT false NOT NULL)
                    """.trimIndent(),
                    """CREATE INDEX IF NOT EXISTS "chain_slot_number_index" ON "chain" (slot_number)""",
                    """CREATE INDEX IF NOT EXISTS "chain_block_number_index" ON "chain" (block_number)""",
                    """CREATE INDEX IF NOT EXISTS "chain_created_index" ON "chain" (created DESC NULLS LAST)""",
                    """CREATE TABLE IF NOT EXISTS "transaction_dest_addresses" ("id" BIGSERIAL PRIMARY KEY, "address" TEXT NOT NULL, "chain_id" BIGINT NOT NULL, CONSTRAINT fk_addresses_chainid_id FOREIGN KEY ("chain_id") REFERENCES "chain"(id) ON DELETE CASCADE ON UPDATE CASCADE, "processed" BOOLEAN DEFAULT false NOT NULL)""",
                    """CREATE INDEX IF NOT EXISTS "addresses_address_index" ON "transaction_dest_addresses" (address)""",
                    """CREATE INDEX IF NOT EXISTS "addresses_processed_index" ON "transaction_dest_addresses" (processed)""",
                    """CREATE INDEX IF NOT EXISTS "chain_id_index" ON "transaction_dest_addresses" (chain_id)""",
                    // Ledger tables
                    """
                    CREATE TABLE IF NOT EXISTS "ledger" ("id" BIGSERIAL PRIMARY KEY, "address" TEXT NOT NULL, "stake_address" TEXT, "address_type" VARCHAR(2) NOT NULL DEFAULT '--')
                    """.trimIndent(),
                    """
                    CREATE TABLE IF NOT EXISTS "ledger_utxos" ("id" BIGSERIAL PRIMARY KEY, "ledger_id" BIGINT NOT NULL, "tx_id" TEXT NOT NULL, "tx_ix" INTEGER NOT NULL, "lovelace" TEXT NOT NULL, "block_created" BIGINT NOT NULL, "slot_created" BIGINT NOT NULL, "block_spent" BIGINT, "slot_spent" BIGINT, "transaction_spent" TEXT, "cbor" BYTEA, CONSTRAINT fk_ledger_utxos_ledger_id_id FOREIGN KEY ("ledger_id") REFERENCES "ledger"(id) ON DELETE CASCADE)
                    """.trimIndent(),
                    """
                    CREATE TABLE IF NOT EXISTS "ledger_assets" ("id" BIGSERIAL PRIMARY KEY, "policy" TEXT NOT NULL, "name" TEXT NOT NULL, "image" TEXT, "description" TEXT)
                    """.trimIndent(),
                    """
                    CREATE TABLE IF NOT EXISTS "ledger_utxo_assets" ("id" BIGSERIAL PRIMARY KEY, "ledger_utxo_id" BIGINT NOT NULL, "ledger_asset_id" BIGINT NOT NULL, "amount" TEXT NOT NULL, CONSTRAINT fk_ledger_utxo_id_id FOREIGN KEY ("ledger_utxo_id") REFERENCES "ledger_utxos"(id) ON DELETE CASCADE)
                    """.trimIndent(),
                    """
                    CREATE INDEX IF NOT EXISTS "ledger_address_index" ON "ledger" (address)
                    """.trimIndent(),
                    """
                    CREATE INDEX IF NOT EXISTS "ledger_utxo_spent_index" ON "ledger_utxos" (block_spent,slot_spent)
                    """.trimIndent(),
                    """
                    CREATE INDEX IF NOT EXISTS "ledger_utxos_transaction_spent_index" ON "ledger_utxos" (transaction_spent NULLS LAST)
                    """.trimIndent(),
                    """
                    CREATE INDEX IF NOT EXISTS "ledger_utxo_created_index" ON "ledger_utxos" (block_created)
                    """.trimIndent(),
                    """
                    CREATE INDEX IF NOT EXISTS "ledger_utxo_slot_spent_index" ON "ledger_utxos" (slot_spent)
                    """.trimIndent(),
                    """
                    CREATE INDEX IF NOT EXISTS "ledger_assets_policy_name_index" ON "ledger_assets" (policy, name)
                    """.trimIndent(),
                    """
                    CREATE INDEX IF NOT EXISTS "ledger_utxo_spent_index" ON "ledger_utxos" (block_spent)
                    """.trimIndent(),
                    """
                    CREATE INDEX IF NOT EXISTS "ledger_utxo_tx_ix_index" ON "ledger_utxos" (tx_id,tx_ix)
                    """.trimIndent(),
                    """
                    CREATE INDEX IF NOT EXISTS "ledger_utxo_ledger_id_index" ON "ledger_utxos" (ledger_id)
                    """.trimIndent(),
                    """
                    CREATE INDEX IF NOT EXISTS "ledger_utxo_assets_ledger_utxo_id_index" ON "ledger_utxo_assets" (ledger_utxo_id)
                    """.trimIndent(),
                    """
                    CREATE INDEX IF NOT EXISTS "ledger_utxo_assets_ledger_asset_id_index" ON "ledger_utxo_assets" (ledger_asset_id)
                    """.trimIndent(),
                    // Payment Stake Address table
                    """
                    CREATE TABLE IF NOT EXISTS "payment_stake_addresses" ("id" BIGSERIAL PRIMARY KEY, "receiving_address" TEXT NOT NULL, "stake_address" TEXT NOT NULL)
                    """.trimIndent(),
                    """
                    CREATE UNIQUE INDEX "payment_stake_address_index" ON "payment_stake_addresses" (stake_address)
                    """.trimIndent(),
                    // Stake Delegations and registrations
                    """
                    CREATE TABLE IF NOT EXISTS "stake_delegations" ("id" BIGSERIAL PRIMARY KEY, "block_number" BIGINT NOT NULL, "stake_address" TEXT NOT NULL, "epoch" BIGINT NOT NULL, "pool_id" TEXT)
                    """.trimIndent(),
                    """
                    CREATE INDEX IF NOT EXISTS "stake_delegations_block_number_index" ON "stake_delegations" (block_number)
                    """.trimIndent(),
                    """
                    CREATE INDEX IF NOT EXISTS "stake_delegations_stakeaddress_epoch_poolid_block_id_index" ON "stake_delegations" (stake_address,epoch,pool_id,block_number,id)
                    """.trimIndent(),
                    """
                    CREATE TABLE IF NOT EXISTS "stake_registrations" ("id" BIGSERIAL PRIMARY KEY, "stake_address" TEXT NOT NULL, "slot" BIGINT NOT NULL, "tx_index" INTEGER NOT NULL, "cert_index" INTEGER NOT NULL)
                    """.trimIndent(),
                    """
                    CREATE UNIQUE INDEX IF NOT EXISTS "stake_registrations_index" ON "stake_registrations" (slot, tx_index, cert_index)
                    """.trimIndent(),
                    // transaction log
                    """
                    CREATE TABLE IF NOT EXISTS "transaction_log" ("id" BIGSERIAL PRIMARY KEY, "transaction_id" TEXT NOT NULL, "cbor" TEXT NOT NULL, "timestamp" TEXT NOT NULL, "result" TEXT NOT NULL)
                    """.trimIndent(),
                    // raw transactions
                    """
                    CREATE TABLE IF NOT EXISTS "raw_transactions" ("id" BIGSERIAL PRIMARY KEY, "block_number" BIGINT NOT NULL, "slot_number" BIGINT NOT NULL, "block_size" INTEGER NOT NULL, "block_body_hash" TEXT NOT NULL, "protocol_version_major" INTEGER NOT NULL, "protocol_version_minor" INTEGER NOT NULL, "tx_id" TEXT NOT NULL, "tx" BYTEA NOT NULL)
                    """.trimIndent(),
                    """
                    CREATE INDEX IF NOT EXISTS "raw_transactions_block_number_index" ON "raw_transactions" (block_number)
                    """.trimIndent(),
                    """
                    CREATE INDEX IF NOT EXISTS "raw_transactions_tx_id_index" ON "raw_transactions" (tx_id)
                    """.trimIndent(),
                )
            )
        }
    }
}
