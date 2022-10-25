package io.newm.chain.database.table

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.Column

object ChainTable : LongIdTable(name = "chain") {
    val blockNumber: Column<Long> = long("block_number").index()
    val slotNumber: Column<Long> = long("slot_number").index()
    val hash: Column<String> = text("hash")
    val prevHash: Column<String> = text("prev_hash")
    val poolId: Column<String> = text("pool_id")
    val etaV: Column<String> = text("eta_v")
    val nodeVkey: Column<String> = text("node_vkey")
    val nodeVrfVkey: Column<String> = text("node_vrf_vkey")
    val blockVrf0: Column<String> = text("block_vrf_0")
    val blockVrf1: Column<String> = text("block_vrf_1")
    val etaVrf0: Column<String> = text("eta_vrf_0")
    val etaVrf1: Column<String> = text("eta_vrf_1")
    val leaderVrf0: Column<String> = text("leader_vrf_0")
    val leaderVrf1: Column<String> = text("leader_vrf_1")
    val blockSize: Column<Int> = integer("block_size")
    val blockBodyHash: Column<String> = text("block_body_hash")
    val poolOpcert: Column<String> = text("pool_opcert")
    val sequenceNumber: Column<Int> = integer("sequence_number")
    val kesPeriod: Column<Int> = integer("kes_period")
    val sigmaSignature: Column<String> = text("sigma_signature")
    val protocolMajorVersion: Column<Int> = integer("protocol_major_version")
    val protocolMinorVersion: Column<Int> = integer("protocol_minor_version")
    val created: Column<Long> = long("created").index()
    val processed: Column<Boolean> = bool("processed").index()
}
