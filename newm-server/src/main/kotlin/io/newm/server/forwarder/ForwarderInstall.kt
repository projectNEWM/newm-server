package io.newm.server.forwarder

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.forwardedheaders.XForwardedHeaders

fun Application.installForwarder() {
    install(XForwardedHeaders)
}
