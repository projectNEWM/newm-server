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
    val status: Status,
    @SerialName("data")
    val data: Data
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

    @Serializable
    data class Data(
        @SerialName("docFirstName")
        val docFirstName: String? = null,
        @SerialName("docLastName")
        val docLastName: String? = null,
        @SerialName("selectedCountry")
        val selectedCountry: String? = null,
        @SerialName("docIssuingCountry")
        val docIssuingCountry: String? = null,
        @SerialName("docNationality")
        val docNationality: String? = null,
        @SerialName("orgNationality")
        val orgNationality: String? = null,
    )
}
