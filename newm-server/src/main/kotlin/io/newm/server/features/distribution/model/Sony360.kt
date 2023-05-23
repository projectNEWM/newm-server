package io.newm.server.features.distribution.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Sony360(
    @SerialName("L0")
    val l0: String? = null,
    @SerialName("L1")
    val l1: String? = null,
    @SerialName("L2")
    val l2: String? = null,
    @SerialName("L3")
    val l3: String? = null,
)
