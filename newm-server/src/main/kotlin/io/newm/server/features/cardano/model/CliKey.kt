package com.firehose.model

import kotlinx.serialization.Serializable

@Serializable
data class CliKey(
    val type: String,
    val description: String,
    val cborHex: String
)
