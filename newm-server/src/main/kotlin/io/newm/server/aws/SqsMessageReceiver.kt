package io.newm.server.aws

import com.amazonaws.services.sqs.model.Message

interface SqsMessageReceiver {
    suspend fun onMessageReceived(message: Message)
}
