package io.projectnewm.server.database

import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.Application
import io.projectnewm.server.auth.jwt.database.JwtTable
import io.projectnewm.server.auth.twofactor.database.TwoFactorAuthTable
import io.projectnewm.server.ext.getConfigString
import io.projectnewm.server.features.playlist.database.PlaylistTable
import io.projectnewm.server.features.playlist.database.SongsInPlaylistsTable
import io.projectnewm.server.features.song.database.SongTable
import io.projectnewm.server.features.user.database.UserTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

fun Application.initializeDatabase() {
    Database.connect(
        HikariDataSource().apply {
            driverClassName = environment.getConfigString("database.driverClassName")
            jdbcUrl = environment.getConfigString("database.jdbcUrl")
            username = environment.getConfigString("database.username")
            password = environment.getConfigString("database.password")
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
        }
    )
    transaction {
        SchemaUtils.create(
            UserTable,
            TwoFactorAuthTable,
            JwtTable,
            SongTable,
            PlaylistTable,
            SongsInPlaylistsTable
        )
    }
}
