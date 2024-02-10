package io.newm.server.recaptcha.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RecaptchaAssessmentResponse(
    @SerialName("tokenProperties")
    val tokenProperties: TokenProperties,
    @SerialName("riskAnalysis")
    val riskAnalysis: RiskAnalysis
) {
    @Serializable
    data class TokenProperties(
        @SerialName("valid")
        val valid: Boolean,
        @SerialName("action")
        val action: String,
        @SerialName("invalidReason")
        val invalidReason: String
    )

    @Serializable
    data class RiskAnalysis(
        @SerialName("score")
        val score: Double
    )
}
