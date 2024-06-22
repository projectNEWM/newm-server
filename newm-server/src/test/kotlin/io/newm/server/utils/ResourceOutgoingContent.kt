package io.newm.server.utils

import io.ktor.http.content.OutgoingContent
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.jvm.javaio.toOutputStream

class ResourceOutgoingContent(
    private val name: String
) : OutgoingContent.WriteChannelContent() {
    override suspend fun writeTo(channel: ByteWriteChannel) {
        ClassLoader.getSystemResourceAsStream(name)!!.use { input ->
            channel.toOutputStream().use { output ->
                input.copyTo(output)
            }
        }
    }
}
