package io.newm.chain.database.table

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.Column

object NativeAssetMonitorLogTable : LongIdTable(name = "native_asset_log") {
    val monitorNativeAssetsResponseBytes: Column<ByteArray> = binary("monitor_native_assets_response")
}
