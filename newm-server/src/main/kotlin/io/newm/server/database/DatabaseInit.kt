package io.newm.server.database

import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.Application
import io.newm.server.auth.jwt.database.JwtTable
import io.newm.server.auth.twofactor.database.TwoFactorAuthTable
import io.newm.server.ext.getConfigString
import io.newm.server.features.playlist.database.PlaylistTable
import io.newm.server.features.playlist.database.SongsInPlaylistsTable
import io.newm.server.features.song.database.SongTable
import io.newm.server.features.user.database.UserTable
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
