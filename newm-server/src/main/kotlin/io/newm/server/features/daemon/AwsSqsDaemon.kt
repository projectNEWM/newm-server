package io.newm.server.features.daemon

import aws.sdk.kotlin.services.sqs.SqsClient
import aws.sdk.kotlin.services.sqs.deleteMessage
import aws.sdk.kotlin.services.sqs.model.ReceiveMessageRequest
import com.amazonaws.regions.Regions
import io.ktor.server.application.ApplicationEnvironment
import io.ktor.server.config.ApplicationConfig
import io.newm.server.aws.SqsMessageReceiver
import io.newm.server.logging.captureToSentry
import io.newm.shared.daemon.Daemon
import io.newm.shared.koin.inject
import io.newm.shared.ktx.getConfigChildren
import io.newm.shared.ktx.getInt
import io.newm.shared.ktx.getLong
import io.newm.shared.ktx.getString
import io.newm.shared.ktx.info
import io.newm.shared.ktx.warn
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.core.parameter.parametersOf
import org.slf4j.Logger
import kotlin.reflect.full.primaryConstructor

class AwsSqsDaemon : Daemon {
    override val log: Logger by inject { parametersOf(javaClass.simpleName) }
    private val environment: ApplicationEnvironment by inject()
    private val regions: Regions by inject()

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
            val sqsQueueUrl = getString("queueUrl")
            if (sqsQueueUrl.isNotBlank()) {
                val waitTime = getInt("waitTime")
                val delayTimeMs = getLong("delayTimeMs")
                val receiverClass = Class.forName(getString("receiver"))
                val receiver = receiverClass.kotlin.primaryConstructor!!.call() as SqsMessageReceiver
                log.info { "SQS Listening on $sqsQueueUrl -> ${receiverClass.simpleName}" }
                while (true) {
                    try {
                        val request = ReceiveMessageRequest {
                            queueUrl = sqsQueueUrl
                            waitTimeSeconds = waitTime
                            maxNumberOfMessages = 1
                        }
                        SqsClient {
                            region = regions.name
                        }.use { sqsClient ->
                            val response = sqsClient.receiveMessage(request)
                            response.messages?.forEach { message ->
                                try {
                                    receiver.onMessageReceived(message)

                                    // If receiver processing throws, we skip deleting the message
                                    // so it ends up in the dead-letter queue for reprocessing.
                                    log.info { "$sqsQueueUrl -> Deleting SQS message: ${message.body}" }
                                    sqsClient.deleteMessage {
                                        queueUrl = sqsQueueUrl
                                        receiptHandle = message.receiptHandle
                                    }
                                    log.info { "$sqsQueueUrl -> Deleted SQS message: ${message.body}" }
                                } catch (e: Throwable) {
                                    log.error("Failure processing SQS message: $sqsQueueUrl", e)
                                    e.captureToSentry()
                                }
                            }
                        }
                    } catch (e: Throwable) {
                        log.error("Error receiving from SQS queue: $sqsQueueUrl", e)
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
