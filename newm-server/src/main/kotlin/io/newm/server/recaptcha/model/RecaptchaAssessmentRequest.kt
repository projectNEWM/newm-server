package io.newm.server.recaptcha.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RecaptchaAssessmentRequest(
    @SerialName("event")
    val event: Event
) {
    @Serializable
    data class Event(
        @SerialName("siteKey")
        val siteKey: String,
        @SerialName("token")
        val token: String
    )
}
