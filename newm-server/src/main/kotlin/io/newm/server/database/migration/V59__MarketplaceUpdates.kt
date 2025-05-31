package io.newm.server.database.migration

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import org.jetbrains.exposed.sql.transactions.transaction

@Suppress("unused")
class V59__MarketplaceUpdates : BaseJavaMigration() {
    override fun migrate(context: Context?) {
        transaction {
            execInBatch(
                listOf(
                    """
                    CREATE TABLE IF NOT EXISTS marketplace_pending_sales (
                        id uuid PRIMARY KEY,
                        created_at timestamp without time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        owner_address text NOT NULL,
                        bundle_policy_id text NOT NULL,
                        bundle_asset_name text NOT NULL,
                        bundle_amount bigint NOT NULL,
                        cost_policy_id text NOT NULL,
                        cost_asset_name text NOT NULL,
                        cost_amount bigint NOT NULL,
                        total_bundle_quantity bigint NOT NULL
                    )
                    """.trimIndent(),
                    "INSERT INTO config VALUES ('marketplace.monitoringEnabled','true') ON CONFLICT(id) DO NOTHING",
                    "INSERT INTO config VALUES ('marketplace.monitoringRetryDelay','30') ON CONFLICT(id) DO NOTHING",
                    "INSERT INTO config VALUES ('marketplace.saleContractAddress','addr_test1xrdcxs8czy6k778aa6dql97l97845qvk8ne3r895at74ntk40rxn7yxhflf44xw7hazl5ttaym2samu9av394s3e8cwq9dp8zt') ON CONFLICT(id) DO NOTHING",
                    "INSERT INTO config VALUES ('marketplace.queueContractAddress','addr_test1xzgsgmax3sf0u66ymfmyu6vaeuj7r2tv729uyh9gtd0ul5x40rxn7yxhflf44xw7hazl5ttaym2samu9av394s3e8cwqgar3zw') ON CONFLICT(id) DO NOTHING",
                    "INSERT INTO config VALUES ('marketplace.pendingSaleTimeToLive','300') ON CONFLICT(id) DO NOTHING",
                    "INSERT INTO config VALUES ('marketplace.pendingOrderTimeToLive','300') ON CONFLICT(id) DO NOTHING",
                    "INSERT INTO config VALUES ('marketplace.orderLovelace','4504110') ON CONFLICT(id) DO NOTHING",
                    "INSERT INTO config VALUES ('marketplace.saleLovelace','4504110') ON CONFLICT(id) DO NOTHING",
                    "INSERT INTO config VALUES ('marketplace.pointerPolicyId','8430a7e28b864f60cf0df03bbdaf842bc2565b8db2da5d675918d103') ON CONFLICT(id) DO NOTHING",
                    "INSERT INTO config VALUES ('marketplace.currencyPolicyId','769c4c6e9bc3ba5406b9b89fb7beb6819e638ff2e2de63f008d5bcff') ON CONFLICT(id) DO NOTHING",
                    "INSERT INTO config VALUES ('marketplace.currencyAssetName','744e45574d') ON CONFLICT(id) DO NOTHING",
                    "INSERT INTO config VALUES ('marketplace.incentiveMinAmount','5000000') ON CONFLICT(id) DO NOTHING"
                )
            )
        }
    }
}
