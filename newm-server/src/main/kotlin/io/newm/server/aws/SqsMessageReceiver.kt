package io.newm.server.aws

import aws.sdk.kotlin.services.sqs.model.Message

interface SqsMessageReceiver {
    suspend fun onMessageReceived(message: Message)
}
