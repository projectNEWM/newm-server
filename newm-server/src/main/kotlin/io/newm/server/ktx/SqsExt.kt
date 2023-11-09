package io.newm.server.ktx

import com.amazonaws.handlers.AsyncHandler
import com.amazonaws.services.sqs.AmazonSQSAsync
import com.amazonaws.services.sqs.model.DeleteMessageRequest
import com.amazonaws.services.sqs.model.DeleteMessageResult
import com.amazonaws.services.sqs.model.Message
import com.amazonaws.services.sqs.model.ReceiveMessageRequest
import com.amazonaws.services.sqs.model.ReceiveMessageResult
import com.amazonaws.services.sqs.model.SendMessageRequest
import com.amazonaws.services.sqs.model.SendMessageResult
import io.newm.shared.koin.inject
import org.koin.core.qualifier.named
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

private val sqsSender: AmazonSQSAsync by inject(named("sqsSender"))
private val sqsReceiver: AmazonSQSAsync by inject(named("sqsReceiver"))
suspend fun SendMessageRequest.await(): SendMessageResult {
    return suspendCoroutine { continuation ->
        sqsSender.sendMessageAsync(
            this,
            object : AsyncHandler<SendMessageRequest, SendMessageResult> {
                override fun onSuccess(request: SendMessageRequest, result: SendMessageResult) {
                    continuation.resume(result)
                }

                override fun onError(exception: Exception) {
                    continuation.resumeWithException(exception)
                }
            }
        )
    }
}

suspend fun ReceiveMessageRequest.await(): ReceiveMessageResult {
    return suspendCoroutine { continuation ->
        sqsReceiver.receiveMessageAsync(
            this,
            object : AsyncHandler<ReceiveMessageRequest, ReceiveMessageResult> {
                override fun onSuccess(request: ReceiveMessageRequest, result: ReceiveMessageResult) {
                    continuation.resume(result)
                }

                override fun onError(exception: Exception) {
                    continuation.resumeWithException(exception)
                }
            }
        )
    }
}

suspend fun Message.delete(queueUrl: String): DeleteMessageResult {
    return suspendCoroutine { continuation ->
        sqsReceiver.deleteMessageAsync(
            queueUrl,
            receiptHandle,
            object : AsyncHandler<DeleteMessageRequest, DeleteMessageResult> {
                override fun onSuccess(request: DeleteMessageRequest, result: DeleteMessageResult) {
                    continuation.resume(result)
                }

                override fun onError(exception: Exception) {
                    continuation.resumeWithException(exception)
                }
            }
        )
    }
}
