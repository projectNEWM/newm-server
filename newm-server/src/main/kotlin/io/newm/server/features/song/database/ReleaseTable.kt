package io.newm.server.features.song.database

import io.newm.server.features.song.model.ReleaseBarcodeType
import io.newm.server.features.song.model.ReleaseType
import io.newm.server.features.user.database.UserTable
import io.newm.server.typealiases.UserId
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.javatime.CurrentDateTime
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDate
import java.time.LocalDateTime

object ReleaseTable : UUIDTable(name = "releases") {
    val archived: Column<Boolean> = bool("archived").default(false)
    val createdAt: Column<LocalDateTime> = datetime("created_at").defaultExpression(CurrentDateTime)
    val ownerId: Column<EntityID<UserId>> = reference("owner_id", UserTable, onDelete = ReferenceOption.NO_ACTION)
    val title: Column<String> = text("title")
    val releaseType: Column<ReleaseType> =
        customEnumeration(
            "release_type",
            "varchar(20)",
            { value -> ReleaseType.valueOf((value as String).uppercase()) },
            { it.name.lowercase() }
        )
    val distributionReleaseId: Column<Long?> = long("distribution_release_id").nullable()
    val barcodeType: Column<ReleaseBarcodeType?> = enumeration("barcode_type", ReleaseBarcodeType::class).nullable()
    val barcodeNumber: Column<String?> = text("barcode_number").nullable()
    val releaseDate: Column<LocalDate?> = date("release_date").nullable()
    val publicationDate: Column<LocalDate?> = date("publication_date").nullable()
    val coverArtUrl: Column<String?> = text("cover_art_url").nullable()
    val arweaveCoverArtUrl: Column<String?> = text("arweave_cover_art_url").nullable()
    val hasSubmittedForDistribution: Column<Boolean> = bool("has_submitted_for_distribution").default(false)
    val errorMessage: Column<String?> = text("error_message").nullable()
    val forceDistributed: Column<Boolean?> = bool("force_distributed").nullable()
}
