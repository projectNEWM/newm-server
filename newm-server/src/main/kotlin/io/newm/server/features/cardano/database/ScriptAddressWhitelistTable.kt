package io.newm.server.features.cardano.database

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Column

object ScriptAddressWhitelistTable : UUIDTable(name = "script_address_whitelist") {
    // scriptAddress that is allowed to pull earnings. We prevent anything sitting in some smart contracts from pulling
    // earnings so tokens are never perma-locked. This is a whitelist of script addresses that are allowed to pull earnings
    // such as multi-sig wallets.
    val scriptAddress: Column<String> = text("script_address")
}
