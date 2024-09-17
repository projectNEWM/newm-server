package io.newm.server.database

import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.ApplicationEnvironment
import io.newm.server.ktx.getSecureConfigString
import io.newm.shared.ktx.getConfigString
import kotlinx.coroutines.runBlocking
import org.koin.dsl.module

val databaseKoinModule =
    module {
        single<HikariDataSource> {
            HikariDataSource().apply {
                val environment: ApplicationEnvironment = get()
                driverClassName = environment.getConfigString("database.driverClassName")
                runBlocking {
                    jdbcUrl = environment.getSecureConfigString("database.jdbcUrl")
                    username = environment.getSecureConfigString("database.username")
                    password = environment.getSecureConfigString("database.password")
                }
                isAutoCommit = false
                transactionIsolation = "TRANSACTION_REPEATABLE_READ"
                connectionTimeout = 30_000L
                maximumPoolSize = 30
                minimumIdle = 5
                maxLifetime = 300_000L // 5 minutes
                validationTimeout = 10_000L
                idleTimeout = 60_000L
                leakDetectionThreshold = 60_000L
            }
        }
    }
