package io.newm.server.features.song.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AgreementMessage(
    @SerialName("Records")
    val records: List<Record>?
) {

    val configurationId: String? by lazy {
        records?.firstOrNull()?.s3?.configurationId
    }

    val key: String? by lazy {
        records?.firstOrNull()?.s3?.objectX?.key
    }

    @Serializable
    data class Record(
        @SerialName("s3")
        val s3: S3?
    ) {

        @Serializable
        data class S3(
            @SerialName("configurationId")
            val configurationId: String?,
            @SerialName("object")
            val objectX: Object?,
        ) {
            @Serializable
            data class Object(
                @SerialName("key")
                val key: String?,
            )
        }
    }
}
