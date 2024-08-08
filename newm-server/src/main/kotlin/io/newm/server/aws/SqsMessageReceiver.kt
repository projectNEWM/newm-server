package io.newm.server.aws

import software.amazon.awssdk.services.sqs.model.Message

interface SqsMessageReceiver {
    suspend fun onMessageReceived(message: Message)
}
