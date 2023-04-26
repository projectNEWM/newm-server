package com.firehose.model

import kotlinx.serialization.Serializable

@Serializable
data class CliKeyPair(
    val name: String,
    val vkey: CliKey,
    val skey: CliKey? = null,
)
