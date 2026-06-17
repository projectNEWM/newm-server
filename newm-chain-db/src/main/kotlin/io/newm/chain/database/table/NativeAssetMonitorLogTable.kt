package io.newm.chain.database.table

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable

object NativeAssetMonitorLogTable : LongIdTable(name = "native_asset_log") {
    val monitorNativeAssetsResponseBytes: Column<ByteArray> = binary("monitor_native_assets_response")
    val blockNumber: Column<Long> = long("block_number")
}
