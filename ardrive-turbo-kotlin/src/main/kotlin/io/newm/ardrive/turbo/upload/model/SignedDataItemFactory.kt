package io.newm.ardrive.turbo.upload.model

import java.io.InputStream

/**
 * Factory for creating signed data items.
 *
 * Performance optimization: Added [dataItemBytes] parameter to allow direct
 * byte access for in-memory data, avoiding unnecessary stream wrapping/unwrapping.
 *
 * @param dataItemStreamFactory Factory to create an InputStream of the signed data
 * @param dataItemSizeFactory Factory to get the size of the signed data
 * @param dataItemBytes Optional direct byte array access (avoids stream overhead for in-memory data)
 */
data class SignedDataItemFactory(
    val dataItemStreamFactory: () -> InputStream,
    val dataItemSizeFactory: () -> Int,
    val dataItemBytes: ByteArray? = null,
) {
    /**
     * Gets the signed data item bytes.
     *
     * If [dataItemBytes] is available (for in-memory data), returns it directly.
     * Otherwise, reads from the stream factory.
     */
    fun getBytes(): ByteArray = dataItemBytes ?: dataItemStreamFactory().readBytes()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SignedDataItemFactory

        if (dataItemBytes != null) {
            if (other.dataItemBytes == null) return false
            if (!dataItemBytes.contentEquals(other.dataItemBytes)) return false
        } else if (other.dataItemBytes != null) {
            return false
        }

        return true
    }

    override fun hashCode(): Int = dataItemBytes?.contentHashCode() ?: 0
}
