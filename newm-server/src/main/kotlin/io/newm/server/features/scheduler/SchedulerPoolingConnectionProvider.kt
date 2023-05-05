package io.newm.server.features.scheduler

import com.zaxxer.hikari.HikariDataSource
import io.newm.shared.koin.inject
import org.quartz.utils.PoolingConnectionProvider
import java.sql.Connection
import javax.sql.DataSource

@Suppress("unused")
class SchedulerPoolingConnectionProvider : PoolingConnectionProvider {
    val hikariDataSource: HikariDataSource by inject()

    override fun getConnection(): Connection {
        hikariDataSource.poolName
        return hikariDataSource.connection
    }

    override fun shutdown() {
        hikariDataSource.close()
    }

    override fun initialize() {
        // do nothing. already initialized during constructor call
    }

    override fun getDataSource(): DataSource = hikariDataSource
}
