package io.newm.server.features.distribution.model

import io.newm.shared.serialization.LocalDateSerializer
import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
data class GetDateResponse(
    @Serializable(with = LocalDateSerializer::class)
    val date: LocalDate
)
