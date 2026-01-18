package io.newm.ardrive.turbo.upload.model

import java.io.InputStream

data class UploadFileDescriptor(
    val path: String,
    val contentType: String,
    val streamFactory: () -> InputStream,
    val sizeFactory: () -> Long,
)
