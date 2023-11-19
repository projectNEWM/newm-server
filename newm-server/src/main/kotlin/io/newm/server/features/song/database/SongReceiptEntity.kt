package io.newm.server.features.song.database

import io.newm.server.features.song.model.SongReceipt
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import java.time.LocalDateTime
import java.util.UUID

class SongReceiptEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    var createdAt: LocalDateTime by SongReceiptTable.createdAt
    var songId: EntityID<UUID> by SongReceiptTable.songId
    var adaPrice: Long by SongReceiptTable.adaPrice
    var usdPrice: Long by SongReceiptTable.usdPrice
    var adaDspPrice: Long by SongReceiptTable.adaDspPrice
    var usdDspPrice: Long by SongReceiptTable.usdDspPrice
    var adaMintPrice: Long by SongReceiptTable.adaMintPrice
    var usdMintPrice: Long by SongReceiptTable.usdMintPrice
    var adaCollabPrice: Long by SongReceiptTable.adaCollabPrice
    var usdCollabPrice: Long by SongReceiptTable.usdCollabPrice
    var usdAdaExchangeRate: Long by SongReceiptTable.usdAdaExchangeRate

    fun toModel(): SongReceipt = SongReceipt(
        id = id.value,
        createdAt = createdAt,
        songId = songId.value,
        adaPrice = adaPrice,
        usdPrice = usdPrice,
        adaDspPrice = adaDspPrice,
        usdDspPrice = usdDspPrice,
        adaMintPrice = adaMintPrice,
        usdMintPrice = usdMintPrice,
        adaCollabPrice = adaCollabPrice,
        usdCollabPrice = usdCollabPrice,
        usdAdaExchangeRate = usdAdaExchangeRate,
    )

    companion object : UUIDEntityClass<SongReceiptEntity>(SongReceiptTable) {
    }
}
