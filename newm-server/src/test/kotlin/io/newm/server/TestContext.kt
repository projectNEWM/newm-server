package io.newm.server

import org.testcontainers.containers.PostgreSQLContainer

object TestContext {
    val container = PostgreSQLContainer<Nothing>("postgres:12").apply {
        withDatabaseName("newm-db")
        withUsername("tester")
        withPassword("newm1234")
    }

    init {
        container.start()
    }
}
