package io.newm.shared.ktx

import io.ktor.util.cio.use
import io.ktor.util.cio.writeChannel
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.copyTo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import java.io.File
import java.util.UUID

@Suppress("BlockingMethodInNonBlockingContext")
suspend fun ByteReadChannel.toTempFile(name: String = UUID.randomUUID().toString()): File {
    var bytesWritten = 0L
    val file = File.createTempFile(name, null)
    try {
        val byteWriteChannel = file.writeChannel(Dispatchers.IO)
        byteWriteChannel.use {
            while (!isClosedForRead) {
                bytesWritten += copyTo(byteWriteChannel)
            }
        }

        // timeout in case something is really broken to prevent infinite loop.
        withTimeout(10_000L) {
            // enforce file size
            while (file.length() != bytesWritten) {
                delay(100L)
            }
        }

        return file
    } catch (throwable: Throwable) {
        file.delete()
        throw throwable
    }
}
