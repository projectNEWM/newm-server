package io.newm.chain.database

import com.viartemev.ktor.flyway.FlywayPlugin
import com.viartemev.ktor.flyway.Info
import com.viartemev.ktor.flyway.Migrate
import com.viartemev.ktor.flyway.Validate
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.*
import io.newm.chain.config.Config
import org.jetbrains.exposed.sql.Database
import java.util.*

fun Application.initializeDatabase() {
    // Init database key encryptor
    Config.S = environment.config.property("database.s").getString()
    Config.spendingPassword = environment.config.property("database.spendingPassword").getString()
    Config.shelleyGenesisHash = environment.config.property("database.shelleyGenesisHash").getString()

    val ds = HikariDataSource(
        HikariConfig(
            Properties().apply {
                this["dataSourceClassName"] = environment.config.property("database.dataSourceClassName").getString()
                this["dataSource.user"] = environment.config.property("database.username").getString()
                this["dataSource.password"] = environment.config.property("database.password").getString()
                this["dataSource.databaseName"] = environment.config.property("database.name").getString()
                this["dataSource.portNumber"] = environment.config.property("database.port").getString()
                this["dataSource.serverName"] = environment.config.property("database.server").getString()
                this["autoCommit"] = false
                this["transactionIsolation"] = "TRANSACTION_REPEATABLE_READ"
                this["connectionTimeout"] = 40_000L
                this["maximumPoolSize"] = 30
                this["minimumIdle"] = 5
                this["maxLifetime"] = 600_000L // 10 minutes
                this["validationTimeout"] = 12_000L
                this["idleTimeout"] = 12_000L
                this["leakDetectionThreshold"] = 60_000L
            }
        )
    )
    Database.connect(ds)
    install(FlywayPlugin) {
        dataSource = ds
        locations = arrayOf("io/newm/chain/database/migration")
        commands(
            *listOf(
                Info,
                Migrate,
                Validate
            ).toTypedArray()
        )
    }
}
