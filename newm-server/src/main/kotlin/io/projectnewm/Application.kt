package io.projectnewm

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.slf4j.LoggerFactory

private val log by lazy { LoggerFactory.getLogger("Application") }

fun main(args: Array<String>) {
    // Start the Ktor engine
    io.ktor.server.cio.EngineMain.main(args)
}

fun Application.module() {
    routing {
        get("/") {
            io.projectnewm.log.info("Received a call!")
            call.respond("Welcome to the Ktor Jungle, NEWM!")
        }
    }
}