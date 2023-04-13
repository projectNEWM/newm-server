package io.newm.server.database

import com.viartemev.ktor.flyway.FlywayPlugin
import com.viartemev.ktor.flyway.Info
import com.viartemev.ktor.flyway.Migrate
import com.viartemev.ktor.flyway.Validate
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.*
import io.newm.shared.ktx.getConfigString
import org.jetbrains.exposed.sql.Database

fun Application.initializeDatabase() {
    val ds = HikariDataSource().apply {
        driverClassName = environment.getConfigString("database.driverClassName")
        jdbcUrl = environment.getConfigString("database.jdbcUrl")
        username = environment.getConfigString("database.username")
        password = environment.getConfigString("database.password")
        isAutoCommit = false
        transactionIsolation = "TRANSACTION_REPEATABLE_READ"
        connectionTimeout = 40_000L
        maximumPoolSize = 30
        minimumIdle = 5
        maxLifetime = 600_000L // 10 minutes
        validationTimeout = 12_000L
        idleTimeout = 12_000L
        leakDetectionThreshold = 60_000L
    }
    Database.connect(ds)
    install(FlywayPlugin) {
        dataSource = ds
        locations = arrayOf("io/newm/server/database/migration")
        commands(
            Info,
            Migrate,
            Validate
        )
    }
}
