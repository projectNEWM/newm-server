package io.newm.ardrive.turbo.upload.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MultiPartStatus(
    val status: String,
    val receipt: MultiPartReceipt? = null,
)

@Serializable
data class MultiPartReceipt(
    val id: String,
    val deadlineHeight: Int,
    val timestamp: Long,
    val version: String,
    val dataCaches: List<String>,
    val fastFinalityIndexes: List<String>,
    val winc: String,
    val owner: String,
    @SerialName("public")
    val publicKey: String,
    val signature: String,
)
