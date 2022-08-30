package io.newm.server.aws

import io.ktor.server.application.Application
import io.newm.server.ext.getConfigChildren

fun Application.initializeAws() {

    for (config in environment.getConfigChildren("aws.sqs")) {
        config.startSqsMessageReceiver()
    }
}
