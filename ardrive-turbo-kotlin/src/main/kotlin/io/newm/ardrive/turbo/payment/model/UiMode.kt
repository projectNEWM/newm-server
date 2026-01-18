package io.newm.ardrive.turbo.payment.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class UiMode {
    @SerialName("hosted")
    HOSTED,

    @SerialName("embedded")
    EMBEDDED,
}
