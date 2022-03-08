package io.projectnewm.server.database

import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.Application
import io.projectnewm.server.ext.getConfigString
import io.projectnewm.server.user.database.UserTable
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
            schema
        }
    )
    transaction {
        SchemaUtils.create(
            UserTable
        )
    }
}
