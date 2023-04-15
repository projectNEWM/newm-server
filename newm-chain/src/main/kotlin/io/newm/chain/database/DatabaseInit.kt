package io.newm.chain.database

import com.viartemev.ktor.flyway.FlywayPlugin
import com.viartemev.ktor.flyway.Info
import com.viartemev.ktor.flyway.Migrate
import com.viartemev.ktor.flyway.Validate
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.*
import io.newm.chain.config.Config
import io.newm.shared.ktx.getConfigString
import org.jetbrains.exposed.sql.Database
import java.util.*

fun Application.initializeDatabase() {
    // Init database key encryptor
    Config.S = environment.getConfigString("database.s")
    Config.spendingPassword = environment.getConfigString("database.spendingPassword")
    Config.shelleyGenesisHash = environment.getConfigString("database.shelleyGenesisHash")

    val ds = HikariDataSource(
        HikariConfig(
            Properties().apply {
                this["dataSourceClassName"] = environment.getConfigString("database.dataSourceClassName")
                this["dataSource.user"] = environment.getConfigString("database.username")
                this["dataSource.password"] = environment.getConfigString("database.password")
                this["dataSource.databaseName"] = environment.getConfigString("database.name")
                this["dataSource.portNumber"] = environment.getConfigString("database.port")
                this["dataSource.serverName"] = environment.getConfigString("database.server")
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
