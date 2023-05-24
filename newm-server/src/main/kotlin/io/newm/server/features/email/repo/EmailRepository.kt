package io.newm.server.features.email.repo

interface EmailRepository {
    suspend fun send(
        to: String,
        subject: String,
        messageUrl: String,
        messageArgs: Map<String, Any> = emptyMap()
    )

    suspend fun send(
        to: List<String>,
        bcc: List<String>,
        subject: String,
        messageUrl: String,
        messageArgs: Map<String, Any> = emptyMap()
    )
}
