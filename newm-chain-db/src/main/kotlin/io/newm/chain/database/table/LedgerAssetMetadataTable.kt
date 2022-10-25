package io.newm.chain.database.table

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.Column

object LedgerAssetMetadataTable : LongIdTable(name = "ledger_asset_metadata") {
    // fk to the ledger_assets table containing policyid/name
    val assetId: Column<Long> = long("asset_id")

    // integer, bytestring, text, array, map, etc... the cbor type
    val keyType: Column<String> = text("key_type")

    // the actual key we scraped from the metadata
    val key: Column<String> = text("key")

    // the cbor type of the value
    val valueType: Column<String> = text("value_type")

    // the actual value we scraped from the metadata
    val value: Column<String> = text("value")

    // 0 is the top level of metadata.
    val nestLevel: Column<Int> = integer("nest_level")

    // used to keep the hierarchy intact
    val parentId: Column<Long?> = long("parent_id").nullable()
}
