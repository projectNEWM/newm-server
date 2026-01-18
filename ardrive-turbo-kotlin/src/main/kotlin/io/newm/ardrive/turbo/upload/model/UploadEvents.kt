package io.newm.ardrive.turbo.upload.model

data class UploadProgress(
    val processedBytes: Long,
    val totalBytes: Long,
    val step: UploadStep,
)

enum class UploadStep {
    SIGNING,
    UPLOADING,
}

data class UploadEvents(
    val onProgress: ((UploadProgress) -> Unit)? = null,
    val onError: ((Throwable) -> Unit)? = null,
    val onSuccess: (() -> Unit)? = null,
)
