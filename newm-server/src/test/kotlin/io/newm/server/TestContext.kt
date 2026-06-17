package io.newm.server

import org.testcontainers.postgresql.PostgreSQLContainer

object TestContext {
    val container = PostgreSQLContainer("postgres:12").apply {
        withDatabaseName("newm-db")
        withUsername("tester")
        withPassword("newm1234")
    }

    init {
        container.start()
    }
}
