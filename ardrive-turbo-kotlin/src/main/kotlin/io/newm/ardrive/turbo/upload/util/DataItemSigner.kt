package io.newm.ardrive.turbo.upload.util

import io.newm.ardrive.turbo.auth.ArweaveSigner
import io.newm.ardrive.turbo.upload.model.DataItemOptions
import io.newm.ardrive.turbo.upload.model.DataItemTag
import io.newm.ardrive.turbo.upload.model.SignedDataItem
import java.security.MessageDigest
import java.util.Base64

class DataItemSigner(
    private val signer: ArweaveSigner,
) {
    suspend fun sign(
        data: ByteArray,
        options: DataItemOptions?,
    ): SignedDataItem {
        val normalizedOptions = options ?: DataItemOptions()
        val tags = normalizedOptions.tags
        val target = normalizedOptions.target
        val anchor = normalizedOptions.anchor
        val unsigned = createUnsignedDataItem(data, tags, target, anchor)
        val signaturePayload = signaturePayload(data, tags, target, anchor)
        val signature = signer.sign(signaturePayload)

        val signatureOffset = 2
        signature.copyInto(unsigned, destinationOffset = signatureOffset)

        return SignedDataItem(bytes = unsigned, size = unsigned.size)
    }

    fun estimateSize(
        dataSize: Int,
        options: DataItemOptions?,
    ): Int {
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
        return 2 + Ans104Constants.SIGNATURE_LENGTH_ARWEAVE + Ans104Constants.OWNER_LENGTH_ARWEAVE + targetLength + anchorLength + tagsLength + dataSize
    }

    private fun createUnsignedDataItem(
        data: ByteArray,
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
        val dataLength = data.size

        val totalLength =
            2 +
                Ans104Constants.SIGNATURE_LENGTH_ARWEAVE +
                Ans104Constants.OWNER_LENGTH_ARWEAVE +
                targetLength +
                anchorLength +
                tagsLength +
                dataLength

        val bytes = ByteArray(totalLength)
        shortTo2ByteArray(Ans104Constants.SIGNATURE_TYPE_ARWEAVE).copyInto(bytes, 0)
        val signatureOffset = 2
        val ownerOffset = signatureOffset + Ans104Constants.SIGNATURE_LENGTH_ARWEAVE
        val owner = signer.publicKeyModulus
        require(owner.size == Ans104Constants.OWNER_LENGTH_ARWEAVE) {
            "Owner must be ${Ans104Constants.OWNER_LENGTH_ARWEAVE} bytes, but was ${owner.size}"
        }
        owner.copyInto(bytes, ownerOffset)

        var position = ownerOffset + Ans104Constants.OWNER_LENGTH_ARWEAVE
        bytes[position] = if (targetBytes == null) 0 else 1
        if (targetBytes != null) {
            require(targetBytes.size == Ans104Constants.TARGET_LENGTH) {
                "Target must be ${Ans104Constants.TARGET_LENGTH} bytes but was ${targetBytes.size}"
            }
            targetBytes.copyInto(bytes, position + 1)
        }
        position += targetLength

        bytes[position] = if (anchorBytes == null) 0 else 1
        if (anchorBytes != null) {
            require(anchorBytes.size == Ans104Constants.ANCHOR_LENGTH) {
                "Anchor must be ${Ans104Constants.ANCHOR_LENGTH} bytes but was ${anchorBytes.size}"
            }
            anchorBytes.copyInto(bytes, position + 1)
        }
        position += anchorLength

        longTo8ByteArray(tags.size.toLong()).copyInto(bytes, position)
        longTo8ByteArray(serializedTags.size.toLong()).copyInto(bytes, position + 8)
        if (serializedTags.isNotEmpty()) {
            serializedTags.copyInto(bytes, position + 16)
        }
        position += tagsLength

        data.copyInto(bytes, position)
        return bytes
    }

    private fun signaturePayload(
        data: ByteArray,
        tags: List<DataItemTag>,
        target: String?,
        anchor: String?,
    ): ByteArray {
        val owner = signer.publicKeyModulus
        val targetBytes = target?.let { Base64.getUrlDecoder().decode(it) } ?: ByteArray(0)
        val anchorBytes = anchor?.toByteArray(Charsets.UTF_8) ?: ByteArray(0)
        val tagsBytes = if (tags.isEmpty()) ByteArray(0) else serializeTags(tags)

        return deepHash(
            listOf(
                "dataitem".toByteArray(Charsets.UTF_8),
                "1".toByteArray(Charsets.UTF_8),
                Ans104Constants.SIGNATURE_TYPE_ARWEAVE.toString().toByteArray(Charsets.UTF_8),
                owner,
                targetBytes,
                anchorBytes,
                tagsBytes,
                data,
            ),
        )
    }

    private fun deepHash(chunks: List<ByteArray>): ByteArray {
        val tag = concatBytes(
            "list".toByteArray(Charsets.UTF_8),
            chunks.size.toString().toByteArray(Charsets.UTF_8),
        )
        return deepHashChunks(chunks, hash(tag))
    }

    private fun deepHashChunks(
        chunks: List<ByteArray>,
        acc: ByteArray,
    ): ByteArray {
        if (chunks.isEmpty()) {
            return acc
        }
        val headHash = deepHashBlob(chunks.first())
        val combined = concatBytes(acc, headHash)
        val nextAcc = hash(combined)
        return deepHashChunks(chunks.drop(1), nextAcc)
    }

    private fun deepHashBlob(data: ByteArray): ByteArray {
        val tag = concatBytes(
            "blob".toByteArray(Charsets.UTF_8),
            data.size.toString().toByteArray(Charsets.UTF_8),
        )
        val tagHash = hash(tag)
        val dataHash = hash(data)
        return hash(concatBytes(tagHash, dataHash))
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
