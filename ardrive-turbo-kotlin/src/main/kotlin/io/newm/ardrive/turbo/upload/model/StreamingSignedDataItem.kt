package io.newm.ardrive.turbo.upload.model

import java.io.InputStream

/**
 * Represents a signed data item where the data payload is accessed via streaming.
 *
 * Used for memory-efficient uploads of large files (>100MB). Instead of loading the
 * entire file into memory, this class holds only the small ANS-104 header (~1KB)
 * and provides a factory to stream the data payload on demand.
 *
 * Memory usage: O(header size) ~1KB, instead of O(file size)
 *
 * @param header ANS-104 header bytes (signature type, signature, owner, target, anchor, tags)
 * @param dataStreamFactory Factory to create a fresh InputStream for reading the data payload
 * @param dataSize Size of the raw data payload in bytes
 */
data class StreamingSignedDataItem(
    /** ANS-104 header bytes (signature type, signature, owner, target, anchor, tags) */
    val header: ByteArray,
    /** Factory to create a fresh InputStream for reading the data payload */
    val dataStreamFactory: () -> InputStream,
    /** Size of the raw data payload in bytes */
    val dataSize: Long,
) {
    /** Total size of the signed data item (header + data) */
    val totalSize: Long get() = header.size + dataSize

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as StreamingSignedDataItem

        if (!header.contentEquals(other.header)) return false
        if (dataSize != other.dataSize) return false

        return true
    }

    override fun hashCode(): Int {
        var result = header.contentHashCode()
        result = 31 * result + dataSize.hashCode()
        return result
    }
}
