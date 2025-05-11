package io.newm.server.features.song.database

import io.newm.server.features.song.model.SongReceipt
import java.time.LocalDateTime
import java.util.UUID
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class SongReceiptEntity(
    id: EntityID<UUID>
) : UUIDEntity(id) {
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
    var newmPrice: Long by SongReceiptTable.newmPrice
    var newmDspPrice: Long by SongReceiptTable.newmDspPrice
    var newmMintPrice: Long by SongReceiptTable.newmMintPrice
    var newmCollabPrice: Long by SongReceiptTable.newmCollabPrice
    var usdNewmExchangeRate: Long by SongReceiptTable.usdNewmExchangeRate

    fun toModel(): SongReceipt =
        SongReceipt(
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
            newmPrice = newmPrice,
            newmDspPrice = newmDspPrice,
            newmMintPrice = newmMintPrice,
            newmCollabPrice = newmCollabPrice,
            usdNewmExchangeRate = usdNewmExchangeRate,
        )

    companion object : UUIDEntityClass<SongReceiptEntity>(SongReceiptTable) {
    }
}
