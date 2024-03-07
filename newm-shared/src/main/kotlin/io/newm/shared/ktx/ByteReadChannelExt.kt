package io.newm.shared.ktx

import io.ktor.util.cio.use
import io.ktor.util.cio.writeChannel
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.copyTo
import kotlinx.coroutines.Dispatchers
import java.io.File
import java.util.UUID

@Suppress("BlockingMethodInNonBlockingContext")
suspend fun ByteReadChannel.toTempFile(name: String = UUID.randomUUID().toString()): Pair<File, Long> {
    var size = 0L
    val file = File.createTempFile(name, null)
    try {
        val byteWriteChannel = file.writeChannel(Dispatchers.IO)
        byteWriteChannel.use {
            while (!isClosedForRead) {
                size += copyTo(byteWriteChannel)
            }
        }
        return file to size
    } catch (throwable: Throwable) {
        file.delete()
        throw throwable
    }
}
