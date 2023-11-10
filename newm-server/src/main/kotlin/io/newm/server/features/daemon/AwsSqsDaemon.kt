package io.newm.server.features.daemon

import com.amazonaws.services.sqs.model.ReceiveMessageRequest
import io.ktor.server.application.ApplicationEnvironment
import io.ktor.server.config.ApplicationConfig
import io.newm.server.aws.SqsMessageReceiver
import io.newm.server.ktx.await
import io.newm.server.ktx.delete
import io.newm.server.logging.captureToSentry
import io.newm.shared.daemon.Daemon
import io.newm.shared.ktx.getConfigChildren
import io.newm.shared.ktx.getInt
import io.newm.shared.ktx.getLong
import io.newm.shared.ktx.getString
import io.newm.shared.ktx.info
import io.newm.shared.ktx.warn
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
                log.info { "SQS Listening on $queueUrl -> ${receiverClass.simpleName}" }
                while (true) {
                    try {
                        val request = ReceiveMessageRequest()
                            .withQueueUrl(queueUrl)
                            .withWaitTimeSeconds(waitTime)
                            .withMaxNumberOfMessages(1)
                        log.info { "$queueUrl -> Receiving SQS messages..." }
                        val result = request.await()
//                        if (result.messages.size > 0) {
                        log.info { "$queueUrl -> Received ${result.messages.size} SQS messages" }
//                        } else {
//                            log.debug { "$queueUrl -> Received ${result.messages.size} SQS messages" }
//                        }
                        for (message in result.messages) {
                            try {
                                receiver.onMessageReceived(message)

                                // If receiver processing throws, we skip deleting the message
                                // so it ends up in the dead-letter queue for reprocessing.
                                log.info { "$queueUrl -> Deleting SQS message: ${message.body}" }
                                message.delete(queueUrl)
                                log.info { "$queueUrl -> Deleted SQS message: ${message.body}" }
                            } catch (e: Throwable) {
                                log.error("Failure processing SQS message: $queueUrl", e)
                                e.captureToSentry()
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
