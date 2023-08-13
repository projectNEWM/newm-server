package io.newm.shared.ktx

import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.jvm.javaio.toInputStream
import java.io.File
import java.util.UUID

fun ByteReadChannel.toTempFile(name: String = UUID.randomUUID().toString()): File {
    val file = File.createTempFile(name, null)
    try {
        toInputStream().use { input ->
            file.outputStream().buffered().use { output ->
                input.copyTo(output)
            }
        }
        return file
    } catch (throwable: Throwable) {
        file.delete()
        throw throwable
    }
}
