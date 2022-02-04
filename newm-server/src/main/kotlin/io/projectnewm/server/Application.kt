package io.projectnewm.server

import io.ktor.server.application.Application
import io.projectnewm.server.pugins.configureMonitoring
import io.projectnewm.server.pugins.configureSerialization

fun main(args: Array<String>) = io.ktor.server.cio.EngineMain.main(args)

@Suppress("unused")
fun Application.mainModule() {
    configureMonitoring()
    configureSerialization()
}
