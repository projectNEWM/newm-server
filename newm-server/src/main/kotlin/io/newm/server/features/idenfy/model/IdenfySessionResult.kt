package io.newm.server.features.idenfy.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class IdenfySessionResult(
    @SerialName("clientId")
    val clientId: String,
    @SerialName("final")
    val isFinal: Boolean,
    @SerialName("status")
    val status: Status
) {
    val isApproved: Boolean by lazy {
        status.overall == "APPROVED"
    }

    @Serializable
    data class Status(
        @SerialName("overall")
        val overall: String,
        @SerialName("autoDocument")
        val autoDocument: String?,
        @SerialName("autoFace")
        val autoFace: String?,
        @SerialName("fraudTags")
        val fraudTags: List<String>?,
        @SerialName("manualDocument")
        val manualDocument: String?,
        @SerialName("manualFace")
        val manualFace: String?,
        @SerialName("mismatchTags")
        val mismatchTags: List<String>?,
        @SerialName("suspicionReasons")
        val suspicionReasons: List<String>?
    )
}
