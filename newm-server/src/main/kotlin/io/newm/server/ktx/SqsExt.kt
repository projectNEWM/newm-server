package io.newm.server.ktx

import io.newm.shared.koin.inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityRequest
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityResponse
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest
import software.amazon.awssdk.services.sqs.model.DeleteMessageResponse
import software.amazon.awssdk.services.sqs.model.Message
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import software.amazon.awssdk.services.sqs.model.SendMessageResponse

private val sqs: SqsAsyncClient by inject()

suspend fun SendMessageRequest.await(): SendMessageResponse =
    suspendCoroutine { continuation ->
        sqs.sendMessage(this).whenComplete { sendMessageResponse, throwable ->
            throwable?.let { continuation.resumeWithException(it) } ?: continuation.resume(sendMessageResponse)
        }
    }

suspend fun ReceiveMessageRequest.await(): ReceiveMessageResponse =
    suspendCoroutine { continuation ->
        sqs.receiveMessage(this).whenComplete { receiveMessageResponse, throwable ->
            throwable?.let { continuation.resumeWithException(it) } ?: continuation.resume(receiveMessageResponse)
        }
    }

suspend fun Message.delete(queueUrl: String): DeleteMessageResponse =
    suspendCoroutine { continuation ->
        val request = DeleteMessageRequest
            .builder()
            .queueUrl(queueUrl)
            .receiptHandle(receiptHandle())
            .build()
        sqs.deleteMessage(request).whenComplete { deleteMessageResponse, throwable ->
            throwable?.let { continuation.resumeWithException(it) } ?: continuation.resume(deleteMessageResponse)
        }
    }

suspend fun Message.markFailed(queueUrl: String): ChangeMessageVisibilityResponse =
    suspendCoroutine { continuation ->
        val request = ChangeMessageVisibilityRequest
            .builder()
            .queueUrl(queueUrl)
            .receiptHandle(receiptHandle())
            .visibilityTimeout(0)
            .build()
        sqs.changeMessageVisibility(request).whenComplete { changeMessageVisibilityResponse, throwable ->
            throwable?.let { continuation.resumeWithException(it) } ?: continuation.resume(
                changeMessageVisibilityResponse
            )
        }
    }
