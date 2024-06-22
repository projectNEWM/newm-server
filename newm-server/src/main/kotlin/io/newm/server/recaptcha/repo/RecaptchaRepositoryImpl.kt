package io.newm.server.recaptcha.repo

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.newm.server.config.repo.ConfigRepository
import io.newm.server.config.repo.ConfigRepository.Companion.CONFIG_KEY_RECAPTCHA_ENABLED
import io.newm.server.config.repo.ConfigRepository.Companion.CONFIG_KEY_RECAPTCHA_MIN_SCORE
import io.newm.server.ktx.checkedBody
import io.newm.server.ktx.getSecureConfigString
import io.newm.server.ktx.requiredHeader
import io.newm.server.recaptcha.RecaptchaHeaders
import io.newm.server.recaptcha.model.RecaptchaAssessmentRequest
import io.newm.server.recaptcha.model.RecaptchaAssessmentResponse
import io.newm.shared.exception.HttpForbiddenException
import io.newm.shared.exception.HttpStatusException
import io.newm.shared.ktx.getConfigString

private val supportedPlatforms = arrayOf("web", "android", "ios")

internal class RecaptchaRepositoryImpl(
    private val environment: ApplicationEnvironment,
    private val configRepository: ConfigRepository,
    private val httpClient: HttpClient
) : RecaptchaRepository {
    private val logger = KotlinLogging.logger {}

    @Throws(HttpStatusException::class)
    override suspend fun verify(
        action: String,
        request: ApplicationRequest
    ) {
        logger.debug { "verify: action = $action" }

        if (!configRepository.getBoolean(CONFIG_KEY_RECAPTCHA_ENABLED)) {
            logger.debug { "verify: recaptcha is disabled" }
            return
        }

        val platform = request.requiredHeader(RecaptchaHeaders.Platform).lowercase()
        if (platform !in supportedPlatforms) {
            val message = "Recaptcha failed - invalid platform: $platform"
            logger.warn { message }
            throw HttpForbiddenException(message)
        }

        val response: RecaptchaAssessmentResponse =
            httpClient
                .post(environment.getConfigString("recaptcha.assessmentUrl")) {
                    parameter("key", environment.getSecureConfigString("recaptcha.apiKey"))
                    contentType(ContentType.Application.Json)
                    accept(ContentType.Application.Json)
                    setBody(
                        RecaptchaAssessmentRequest(
                            RecaptchaAssessmentRequest.Event(
                                siteKey = environment.getSecureConfigString("recaptcha.${platform}SiteKey"),
                                token = request.requiredHeader(RecaptchaHeaders.Token)
                            )
                        )
                    )
                }.checkedBody()

        if (!response.tokenProperties.valid) {
            val message = "Recaptcha failed - reason: ${response.tokenProperties.invalidReason}"
            logger.warn { message }
            throw HttpForbiddenException(message)
        }
        if (response.tokenProperties.action != action) {
            val message = "Recaptcha failed - action expected: $action, actual: ${response.tokenProperties.action}"
            logger.warn { message }
            throw HttpForbiddenException(message)
        }
        if (response.riskAnalysis.score < configRepository.getDouble(CONFIG_KEY_RECAPTCHA_MIN_SCORE)) {
            val message = "Recaptcha failed - score too low: ${response.riskAnalysis.score}"
            logger.warn { message }
            throw HttpForbiddenException(message)
        }
    }
}
