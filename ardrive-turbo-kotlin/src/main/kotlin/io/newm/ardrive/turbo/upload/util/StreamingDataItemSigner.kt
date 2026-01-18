package io.newm.ardrive.turbo.upload.util

import io.newm.ardrive.turbo.auth.ArweaveSigner
import io.newm.ardrive.turbo.upload.model.DataItemOptions
import io.newm.ardrive.turbo.upload.model.DataItemTag
import io.newm.ardrive.turbo.upload.model.StreamingSignedDataItem
import java.io.InputStream
import java.security.MessageDigest
import java.util.Base64

/**
 * Signs data items using streaming - file is read twice but never fully loaded into memory.
 *
 * This is the memory-efficient alternative to [DataItemSigner] for large files (>100MB).
 *
 * Memory usage:
 * - Pass 1 (signing): O(buffer size) ~8KB
 * - Result: O(header size) ~1KB
 *
 * Trade-off: The source file must be read twice (once for signature hash, once during upload),
 * but this avoids OutOfMemoryErrors for multi-GB files.
 *
 * @param signer The Arweave wallet signer used to sign the data item
 */
class StreamingDataItemSigner(
    private val signer: ArweaveSigner,
) {
    companion object {
        private const val BUFFER_SIZE = 8192 // 8KB streaming buffer
    }

    /**
     * Signs a data item using streaming.
     *
     * The source is read once during this call to compute the deep hash signature.
     * The returned [StreamingSignedDataItem] contains only the small header (~1KB)
     * and a factory to re-read the source during upload.
     *
     * @param streamFactory Factory that creates a fresh InputStream for each read pass
     * @param dataSize Known size of the data in bytes (required for ANS-104 header)
     * @param options Data item options (tags, target, anchor)
     * @return A streaming signed data item with header and stream factory
     */
    suspend fun signStreaming(
        streamFactory: () -> InputStream,
        dataSize: Long,
        options: DataItemOptions?,
    ): StreamingSignedDataItem {
        val normalizedOptions = options ?: DataItemOptions()
        val tags = normalizedOptions.tags
        val target = normalizedOptions.target
        val anchor = normalizedOptions.anchor

        // PASS 1: Stream through data to compute signature hash
        val signaturePayload = streamFactory().use { stream ->
            computeSignaturePayloadStreaming(stream, dataSize, tags, target, anchor)
        }
        val signature = signer.sign(signaturePayload)

        // Build header (small, ~1KB typically)
        val header = createDataItemHeader(signature, tags, target, anchor)

        return StreamingSignedDataItem(
            header = header,
            dataStreamFactory = streamFactory,
            dataSize = dataSize,
        )
    }

    /**
     * Estimates the total size of the signed data item without reading the data.
     */
    fun estimateSignedSize(
        dataSize: Long,
        options: DataItemOptions?,
    ): Long {
        val normalized = options ?: DataItemOptions()
        val tags = normalized.tags
        val targetLength = if (normalized.target == null) {
            Ans104Constants.PRESENCE_BYTE_LENGTH
        } else {
            Ans104Constants.PRESENCE_BYTE_LENGTH + Ans104Constants.TARGET_LENGTH
        }
        val anchorLength = if (normalized.anchor == null) {
            Ans104Constants.PRESENCE_BYTE_LENGTH
        } else {
            Ans104Constants.PRESENCE_BYTE_LENGTH + Ans104Constants.ANCHOR_LENGTH
        }
        val serializedTags = if (tags.isEmpty()) ByteArray(0) else serializeTags(tags)
        val tagsLength = Ans104Constants.TAG_BYTES_OVERHEAD + serializedTags.size
        val headerSize = 2 +
            Ans104Constants.SIGNATURE_LENGTH_ARWEAVE +
            Ans104Constants.OWNER_LENGTH_ARWEAVE +
            targetLength +
            anchorLength +
            tagsLength
        return headerSize + dataSize
    }

    private fun computeSignaturePayloadStreaming(
        dataStream: InputStream,
        dataSize: Long,
        tags: List<DataItemTag>,
        target: String?,
        anchor: String?,
    ): ByteArray {
        val owner = signer.publicKeyModulus
        val targetBytes = target?.let { Base64.getUrlDecoder().decode(it) } ?: ByteArray(0)
        val anchorBytes = anchor?.toByteArray(Charsets.UTF_8) ?: ByteArray(0)
        val tagsBytes = if (tags.isEmpty()) ByteArray(0) else serializeTags(tags)

        return deepHashStreaming(
            metadataChunks = listOf(
                "dataitem".toByteArray(Charsets.UTF_8),
                "1".toByteArray(Charsets.UTF_8),
                Ans104Constants.SIGNATURE_TYPE_ARWEAVE.toString().toByteArray(Charsets.UTF_8),
                owner,
                targetBytes,
                anchorBytes,
                tagsBytes,
            ),
            dataStream = dataStream,
            dataSize = dataSize,
        )
    }

    /**
     * Computes deep hash with streaming for the data chunk.
     *
     * All small metadata chunks are hashed normally. The large data chunk
     * is streamed through the hash function to avoid loading it into memory.
     */
    private fun deepHashStreaming(
        metadataChunks: List<ByteArray>,
        dataStream: InputStream,
        dataSize: Long,
    ): ByteArray {
        val listSize = metadataChunks.size + 1 // +1 for data chunk
        val tag = concatBytes(
            "list".toByteArray(Charsets.UTF_8),
            listSize.toString().toByteArray(Charsets.UTF_8),
        )
        var acc = hash(tag)

        // Hash all small metadata chunks normally
        for (chunk in metadataChunks) {
            val headHash = deepHashBlob(chunk)
            acc = hash(concatBytes(acc, headHash))
        }

        // Stream-hash the large data chunk
        val dataHash = deepHashBlobStreaming(dataStream, dataSize)
        return hash(concatBytes(acc, dataHash))
    }

    /**
     * Computes deep hash of a blob using streaming.
     *
     * The tag (containing size) is computed first, then the data is
     * streamed through SHA-384 without loading it all into memory.
     */
    private fun deepHashBlobStreaming(
        stream: InputStream,
        size: Long
    ): ByteArray {
        val tag = concatBytes(
            "blob".toByteArray(Charsets.UTF_8),
            size.toString().toByteArray(Charsets.UTF_8),
        )
        val tagHash = hash(tag)
        val dataHash = hashStream(stream)
        return hash(concatBytes(tagHash, dataHash))
    }

    /**
     * Computes SHA-384 hash of an input stream without loading it into memory.
     */
    private fun hashStream(stream: InputStream): ByteArray {
        val digest = MessageDigest.getInstance("SHA-384")
        val buffer = ByteArray(BUFFER_SIZE)
        var bytesRead: Int
        while (stream.read(buffer).also { bytesRead = it } != -1) {
            digest.update(buffer, 0, bytesRead)
        }
        return digest.digest()
    }

    /**
     * Computes deep hash of a small blob (for metadata chunks).
     */
    private fun deepHashBlob(data: ByteArray): ByteArray {
        val tag = concatBytes(
            "blob".toByteArray(Charsets.UTF_8),
            data.size.toString().toByteArray(Charsets.UTF_8),
        )
        val tagHash = hash(tag)
        val dataHash = hash(data)
        return hash(concatBytes(tagHash, dataHash))
    }

    /**
     * Creates the ANS-104 header (everything except the data payload).
     *
     * Header structure:
     * - 2 bytes: signature type
     * - 512 bytes: signature
     * - 512 bytes: owner public key
     * - 1+ bytes: target (presence byte + optional 32-byte target)
     * - 1+ bytes: anchor (presence byte + optional 32-byte anchor)
     * - 16+ bytes: tags (count, byte length, serialized tags)
     */
    private fun createDataItemHeader(
        signature: ByteArray,
        tags: List<DataItemTag>,
        target: String?,
        anchor: String?,
    ): ByteArray {
        val targetBytes = target?.let { Base64.getUrlDecoder().decode(it) }
        val anchorBytes = anchor?.toByteArray(Charsets.UTF_8)
        val serializedTags = if (tags.isEmpty()) ByteArray(0) else serializeTags(tags)

        val targetLength = Ans104Constants.PRESENCE_BYTE_LENGTH + (targetBytes?.size ?: 0)
        val anchorLength = Ans104Constants.PRESENCE_BYTE_LENGTH + (anchorBytes?.size ?: 0)
        val tagsLength = Ans104Constants.TAG_BYTES_OVERHEAD + serializedTags.size

        val headerLength = 2 +
            Ans104Constants.SIGNATURE_LENGTH_ARWEAVE +
            Ans104Constants.OWNER_LENGTH_ARWEAVE +
            targetLength +
            anchorLength +
            tagsLength

        val header = ByteArray(headerLength)
        shortTo2ByteArray(Ans104Constants.SIGNATURE_TYPE_ARWEAVE).copyInto(header, 0)

        val signatureOffset = 2
        signature.copyInto(header, signatureOffset)

        val ownerOffset = signatureOffset + Ans104Constants.SIGNATURE_LENGTH_ARWEAVE
        val owner = signer.publicKeyModulus
        require(owner.size == Ans104Constants.OWNER_LENGTH_ARWEAVE) {
            "Owner must be ${Ans104Constants.OWNER_LENGTH_ARWEAVE} bytes, but was ${owner.size}"
        }
        owner.copyInto(header, ownerOffset)

        var position = ownerOffset + Ans104Constants.OWNER_LENGTH_ARWEAVE
        header[position] = if (targetBytes == null) 0 else 1
        if (targetBytes != null) {
            require(targetBytes.size == Ans104Constants.TARGET_LENGTH) {
                "Target must be ${Ans104Constants.TARGET_LENGTH} bytes but was ${targetBytes.size}"
            }
            targetBytes.copyInto(header, position + 1)
        }
        position += targetLength

        header[position] = if (anchorBytes == null) 0 else 1
        if (anchorBytes != null) {
            require(anchorBytes.size == Ans104Constants.ANCHOR_LENGTH) {
                "Anchor must be ${Ans104Constants.ANCHOR_LENGTH} bytes but was ${anchorBytes.size}"
            }
            anchorBytes.copyInto(header, position + 1)
        }
        position += anchorLength

        longTo8ByteArray(tags.size.toLong()).copyInto(header, position)
        longTo8ByteArray(serializedTags.size.toLong()).copyInto(header, position + 8)
        if (serializedTags.isNotEmpty()) {
            serializedTags.copyInto(header, position + 16)
        }

        return header
    }

    private fun hash(data: ByteArray): ByteArray = MessageDigest.getInstance("SHA-384").digest(data)

    private fun concatBytes(
        first: ByteArray,
        second: ByteArray
    ): ByteArray {
        val combined = ByteArray(first.size + second.size)
        first.copyInto(combined, 0)
        second.copyInto(combined, first.size)
        return combined
    }
}
