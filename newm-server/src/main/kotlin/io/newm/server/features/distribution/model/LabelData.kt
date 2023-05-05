package io.newm.server.features.distribution.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LabelData(
    @SerialName("label_name")
    val labelName: String,
    @SerialName("label_id")
    val labelId: String
)
