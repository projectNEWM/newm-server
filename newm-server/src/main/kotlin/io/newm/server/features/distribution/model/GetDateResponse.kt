package io.newm.server.features.distribution.model

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
data class GetDateResponse(
    @Contextual
    val date: LocalDate
)
