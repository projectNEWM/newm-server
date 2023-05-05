package io.newm.server.features.distribution.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Country(
    @SerialName("country_code")
    val countryCode: String,
    @SerialName("country_name")
    val countryName: String,
    @SerialName("state")
    val states: List<State>,
)
