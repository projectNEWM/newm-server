package io.newm.ardrive.turbo.upload.model

import kotlinx.serialization.Serializable

@Serializable
data class DataItemOptions(
    val tags: List<DataItemTag> = emptyList(),
    val target: String? = null,
    val anchor: String? = null,
    val paidBy: List<String> = emptyList(),
)
