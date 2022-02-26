package io.projectnewm.server

import io.ktor.server.application.Application
import io.projectnewm.server.pugins.configureAuthentication
import io.projectnewm.server.pugins.configureMonitoring
import io.projectnewm.server.pugins.configureSerialization
import io.projectnewm.server.pugins.configureSessions
import io.projectnewm.server.user.UserRepository

fun main(args: Array<String>) = io.ktor.server.cio.EngineMain.main(args)

@Suppress("unused")
fun Application.mainModule() {
    val userRepository = UserRepository()

    configureMonitoring()
    configureSerialization()
    configureSessions()
    configureAuthentication(userRepository)
}
