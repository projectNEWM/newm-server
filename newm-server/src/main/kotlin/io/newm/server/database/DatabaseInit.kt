package io.newm.server.database

import com.viartemev.ktor.flyway.FlywayPlugin
import com.viartemev.ktor.flyway.Info
import com.viartemev.ktor.flyway.Migrate
import com.viartemev.ktor.flyway.Validate
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.newm.shared.koin.inject
import org.jetbrains.exposed.sql.Database

fun Application.initializeDatabase() {
    val hikariDataSource: HikariDataSource by inject()

    Database.connect(hikariDataSource)
    install(FlywayPlugin) {
        dataSource = hikariDataSource
        locations = arrayOf("io/newm/server/database/migration")
        commands(
            Info,
            Migrate,
            Validate
        )
    }
}
