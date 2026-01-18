package io.newm.ardrive.turbo.upload.model

import kotlinx.serialization.Serializable

@Serializable
data class DataItemTag(
    val name: String,
    val value: String,
)
