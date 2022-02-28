package io.projectnewm.server

import io.ktor.server.application.Application
import io.projectnewm.server.debug.configureDebugHomePage
import io.projectnewm.server.plugins.configureAuthentication
import io.projectnewm.server.plugins.configureDependencyInjection
import io.projectnewm.server.plugins.configureMonitoring
import io.projectnewm.server.plugins.configureSerialization
import io.projectnewm.server.plugins.configureSessions

fun main(args: Array<String>) = io.ktor.server.cio.EngineMain.main(args)

@Suppress("unused")
fun Application.mainModule() {
    configureMonitoring()
    configureDependencyInjection()
    configureSerialization()
    configureSessions()
    configureAuthentication()
    configureDebugHomePage()
}
