package io.newm.ardrive.turbo.upload.util

import io.newm.ardrive.turbo.upload.model.DataItemOptions
import io.newm.ardrive.turbo.upload.model.SignedDataItemFactory
import java.io.ByteArrayInputStream
import java.io.InputStream

object SignedDataItemStreamFactory {
    /**
     * Creates a SignedDataItemFactory from raw bytes.
     *
     * Performance optimization: Stores the signed bytes directly in the factory
     * to avoid stream wrapping/unwrapping overhead when [getBytes] is called.
     */
    suspend fun fromBytes(
        data: ByteArray,
        signer: DataItemSigner,
        options: DataItemOptions? = null,
    ): SignedDataItemFactory {
        val signed = signer.sign(data, options)
        return SignedDataItemFactory(
            dataItemStreamFactory = { ByteArrayInputStream(signed.bytes) },
            dataItemSizeFactory = { signed.size },
            dataItemBytes = signed.bytes,
        )
    }

    /**
     * Creates a SignedDataItemFactory from pre-signed bytes.
     *
     * Performance optimization: Stores the bytes directly in the factory
     * to avoid stream wrapping/unwrapping overhead when [getBytes] is called.
     */
    fun fromSignedBytes(
        data: ByteArray,
    ): SignedDataItemFactory =
        SignedDataItemFactory(
            dataItemStreamFactory = { ByteArrayInputStream(data) },
            dataItemSizeFactory = { data.size },
            dataItemBytes = data,
        )

    fun fromInputStream(
        streamFactory: () -> InputStream,
        sizeFactory: () -> Int,
    ): SignedDataItemFactory =
        SignedDataItemFactory(
            dataItemStreamFactory = streamFactory,
            dataItemSizeFactory = sizeFactory,
            dataItemBytes = null,
        )
}
