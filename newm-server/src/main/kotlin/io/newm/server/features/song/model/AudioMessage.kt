package io.newm.server.features.song.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AudioMessage(
    @SerialName("detail")
    val detail: Detail?
) {
    val status: String?
        get() = detail?.status

    val transcodingType: String? by lazy {
        detail?.userMetadata?.transcodingType
    }

    val durationInMs: Int? by lazy {
        detail
            ?.outputGroupDetails
            ?.firstOrNull()
            ?.outputDetails
            ?.firstOrNull()
            ?.durationInMs
    }

    val outputFilePath: String? by lazy {
        detail
            ?.outputGroupDetails
            ?.firstOrNull()
            ?.outputDetails
            ?.firstOrNull()
            ?.outputFilePaths
            ?.firstOrNull()
    }

    @Serializable
    data class Detail(
        @SerialName("outputGroupDetails")
        val outputGroupDetails: List<OutputGroupDetail>?,
        @SerialName("status")
        val status: String?,
        @SerialName("userMetadata")
        val userMetadata: UserMetadata?
    ) {
        @Serializable
        data class OutputGroupDetail(
            @SerialName("outputDetails")
            val outputDetails: List<OutputDetail>
        ) {
            @Serializable
            data class OutputDetail(
                @SerialName("outputFilePaths")
                val outputFilePaths: List<String>?,
                @SerialName("durationInMs")
                val durationInMs: Int?
            )
        }

        @Serializable
        data class UserMetadata(
            @SerialName("transcodingType")
            val transcodingType: String?
        )
    }
}
