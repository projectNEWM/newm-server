package io.newm.server.ktx

import com.amazonaws.handlers.AsyncHandler
import com.amazonaws.services.sqs.AmazonSQSAsync
import com.amazonaws.services.sqs.model.ChangeMessageVisibilityRequest
import com.amazonaws.services.sqs.model.ChangeMessageVisibilityResult
import com.amazonaws.services.sqs.model.DeleteMessageRequest
import com.amazonaws.services.sqs.model.DeleteMessageResult
import com.amazonaws.services.sqs.model.Message
import com.amazonaws.services.sqs.model.ReceiveMessageRequest
import com.amazonaws.services.sqs.model.ReceiveMessageResult
import com.amazonaws.services.sqs.model.SendMessageRequest
import com.amazonaws.services.sqs.model.SendMessageResult
import io.newm.shared.koin.inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

private val sqs: AmazonSQSAsync by inject()

suspend fun SendMessageRequest.await(): SendMessageResult {
    return suspendCoroutine { continuation ->
        sqs.sendMessageAsync(
            this,
            object : AsyncHandler<SendMessageRequest, SendMessageResult> {
                override fun onSuccess(
                    request: SendMessageRequest,
                    result: SendMessageResult
                ) {
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
        sqs.receiveMessageAsync(
            this,
            object : AsyncHandler<ReceiveMessageRequest, ReceiveMessageResult> {
                override fun onSuccess(
                    request: ReceiveMessageRequest,
                    result: ReceiveMessageResult
                ) {
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
        sqs.deleteMessageAsync(
            queueUrl,
            receiptHandle,
            object : AsyncHandler<DeleteMessageRequest, DeleteMessageResult> {
                override fun onSuccess(
                    request: DeleteMessageRequest,
                    result: DeleteMessageResult
                ) {
                    continuation.resume(result)
                }

                override fun onError(exception: Exception) {
                    continuation.resumeWithException(exception)
                }
            }
        )
    }
}

suspend fun Message.markFailed(queueUrl: String): ChangeMessageVisibilityResult {
    return suspendCoroutine { continuation ->
        sqs.changeMessageVisibilityAsync(
            queueUrl,
            receiptHandle,
            0,
            object : AsyncHandler<ChangeMessageVisibilityRequest, ChangeMessageVisibilityResult> {
                override fun onSuccess(
                    request: ChangeMessageVisibilityRequest,
                    result: ChangeMessageVisibilityResult
                ) {
                    continuation.resume(result)
                }

                override fun onError(exception: Exception) {
                    continuation.resumeWithException(exception)
                }
            }
        )
    }
}
