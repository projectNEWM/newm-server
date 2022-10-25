package io.newm.server.aws

import com.amazonaws.handlers.AsyncHandler
import com.amazonaws.services.sqs.AmazonSQSAsync
import com.amazonaws.services.sqs.model.Message
import com.amazonaws.services.sqs.model.ReceiveMessageRequest
import com.amazonaws.services.sqs.model.ReceiveMessageResult
import io.ktor.server.config.ApplicationConfig
import io.ktor.util.logging.Logger
import io.newm.server.di.inject
import io.newm.server.ext.getInt
import io.newm.server.ext.getString
import org.slf4j.MarkerFactory
import kotlin.reflect.full.primaryConstructor

interface SqsMessageReceiver {
    fun onMessageReceived(message: Message)
}

fun ApplicationConfig.startSqsMessageReceiver() {

    val queueUrl = getString("queueUrl")
    val waitTime = getInt("waitTime")
    val receiverClass = Class.forName(getString("receiver"))
    val receiver = receiverClass.kotlin.primaryConstructor!!.call() as SqsMessageReceiver

    val sqs: AmazonSQSAsync by inject()
    val logger: Logger by inject()
    val marker = MarkerFactory.getMarker(receiverClass.simpleName)

    fun receive() {
        logger.debug(marker, "Polling messages from SQS Queue: $queueUrl")
        sqs.receiveMessageAsync(
            ReceiveMessageRequest()
                .withQueueUrl(queueUrl)
                .withWaitTimeSeconds(waitTime),
            object : AsyncHandler<ReceiveMessageRequest, ReceiveMessageResult> {
                override fun onSuccess(request: ReceiveMessageRequest, result: ReceiveMessageResult) {
                    logger.debug(marker, "Received ${result.messages.size} SQS messages")
                    for (message in result.messages) {
                        try {
                            receiver.onMessageReceived(message)
                        } catch (exception: Exception) {
                            logger.error(marker, "Failure processing SQS message", exception)
                        }
                        sqs.deleteMessage(queueUrl, message.receiptHandle)
                    }
                    receive()
                }

                override fun onError(exception: Exception) {
                    logger.error(marker, "Failure receiving SQS messages", exception)
                }
            }
        )
    }
    if (queueUrl.isNotBlank()) {
        receive()
    } else {
        logger.warn("Empty SQS url. disabling...")
    }
}
