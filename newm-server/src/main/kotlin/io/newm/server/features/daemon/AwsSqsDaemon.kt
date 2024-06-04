package io.newm.server.features.daemon

import com.amazonaws.services.sqs.model.ReceiveMessageRequest
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.application.ApplicationEnvironment
import io.ktor.server.config.ApplicationConfig
import io.newm.server.aws.SqsMessageReceiver
import io.newm.server.ktx.await
import io.newm.server.ktx.delete
import io.newm.server.ktx.markFailed
import io.newm.server.logging.captureToSentry
import io.newm.shared.daemon.Daemon
import io.newm.shared.koin.inject
import io.newm.shared.ktx.getConfigChildren
import io.newm.shared.ktx.getInt
import io.newm.shared.ktx.getLong
import io.newm.shared.ktx.getString
import kotlin.reflect.full.primaryConstructor
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AwsSqsDaemon : Daemon {
    override val log = KotlinLogging.logger {}
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
                        val request =
                            ReceiveMessageRequest()
                                .withQueueUrl(queueUrl)
                                .withWaitTimeSeconds(waitTime)
                                .withMaxNumberOfMessages(1)
                        val result = request.await()
                        for (message in result.messages) {
                            try {
                                receiver.onMessageReceived(message)

                                // If receiver processing throws, we skip deleting the message
                                // so it ends up in the dead-letter queue for reprocessing.
                                log.info { "$queueUrl -> Deleting SQS message: ${message.body}" }
                                message.delete(queueUrl)
                                log.info { "$queueUrl -> Deleted SQS message: ${message.body}" }
                            } catch (e: Throwable) {
                                log.error(e) { "Failure processing SQS message: $queueUrl" }
                                e.captureToSentry()
                                try {
                                    message.markFailed(queueUrl)
                                } catch (e: Throwable) {
                                    log.error { "Failed to set visibility timeout to zero: ${message.body}" }
                                }
                            }
                        }
                    } catch (e: Throwable) {
                        log.error(e) { "Error receiving from SQS queue: $queueUrl" }
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
