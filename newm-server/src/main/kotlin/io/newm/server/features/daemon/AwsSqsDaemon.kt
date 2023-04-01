package io.newm.server.features.daemon

import com.amazonaws.services.sqs.model.ReceiveMessageRequest
import io.ktor.server.application.ApplicationEnvironment
import io.ktor.server.config.ApplicationConfig
import io.newm.server.aws.SqsMessageReceiver
import io.newm.server.ext.await
import io.newm.server.ext.delete
import io.newm.shared.daemon.Daemon
import io.newm.shared.ext.debug
import io.newm.shared.ext.getConfigChildren
import io.newm.shared.ext.getInt
import io.newm.shared.ext.getLong
import io.newm.shared.ext.getString
import io.newm.shared.ext.info
import io.newm.shared.ext.warn
import io.newm.shared.koin.inject
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.core.parameter.parametersOf
import org.slf4j.Logger
import kotlin.reflect.full.primaryConstructor

class AwsSqsDaemon : Daemon {
    override val log: Logger by inject { parametersOf(javaClass.simpleName) }
    private val environment: ApplicationEnvironment by inject()

    override fun start() {
        log.info { "starting..." }
        startSqsReceivers()
        log.info { "startup complete." }
    }

    override fun shutdown() {
        log.info { "begin shutdown..." }
        coroutineContext.cancelChildren()
        log.info { "shutdown complete." }
    }

    private fun startSqsReceivers() {
        for (config in environment.getConfigChildren("aws.sqs")) {
            config.startSqsMessageReceiver()
        }
    }

    private fun ApplicationConfig.startSqsMessageReceiver() {
        launch {
            val queueUrl = getString("queueUrl")
            if (queueUrl.isNotBlank()) {
                val waitTime = getInt("waitTime")
                val delayTimeMs = getLong("delayTimeMs")
                val receiverClass = Class.forName(getString("receiver"))
                val receiver = receiverClass.kotlin.primaryConstructor!!.call() as SqsMessageReceiver
                val request = ReceiveMessageRequest()
                    .withQueueUrl(queueUrl)
                    .withWaitTimeSeconds(waitTime)
                log.info { "SQS Listening on $queueUrl -> ${receiverClass.simpleName}" }
                while (true) {
                    try {
                        val result = request.await()
                        log.debug { "$queueUrl -> Received ${result.messages.size} SQS messages" }
                        for (message in result.messages) {
                            try {
                                receiver.onMessageReceived(message)

                                // If receiver processing throws, we skip deleting the message
                                // so it ends up in the dead-letter queue for reprocessing.
                                message.delete(queueUrl)
                            } catch (e: Throwable) {
                                log.error("Failure processing SQS message: $queueUrl", e)
                            }
                        }
                    } catch (e: Throwable) {
                        log.error("Error receiving from SQS queue: $queueUrl", e)
                    } finally {
                        delay(delayTimeMs)
                    }
                }
            } else {
                log.warn { "Empty SQS url for ${this@startSqsMessageReceiver}. disabling..." }
            }
        }
    }
}