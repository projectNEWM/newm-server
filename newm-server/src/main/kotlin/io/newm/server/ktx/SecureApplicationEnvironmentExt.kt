package io.newm.server.ktx

import com.amazonaws.handlers.AsyncHandler
import com.amazonaws.services.secretsmanager.AWSSecretsManagerAsync
import com.amazonaws.services.secretsmanager.model.GetSecretValueRequest
import com.amazonaws.services.secretsmanager.model.GetSecretValueResult
import com.github.benmanes.caffeine.cache.Caffeine
import io.ktor.server.application.ApplicationEnvironment
import io.ktor.server.config.ApplicationConfig
import io.newm.shared.koin.inject
import io.newm.shared.ktx.getChildFullPath
import io.newm.shared.ktx.getString
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.time.Duration
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

private val secretsManager: AWSSecretsManagerAsync by inject()
private val json: Json by inject()
private val secretsMutex = Mutex()
private val secretsCache =
    Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofMinutes(10)) // approximately 4500 requests per month
        .build<String, Map<String, String>>()

/**
 * If the configured environment variable points to an AWS Secrets Manager arn resource, retrieve the map
 * of key/value pairs from secrets manager and then lookup the path value. Otherwise, just return the set value
 * as-is.
 */
suspend fun ApplicationEnvironment.getSecureConfigString(path: String): String = config.getSecureString(path)

suspend fun ApplicationConfig.getSecureString(path: String): String {
    val configValue = getString(path)
    if (!configValue.startsWith("arn:aws:secretsmanager")) {
        return configValue
    }

    val fullPath = getChildFullPath(path)
    secretsMutex.withLock {
        return secretsCache.getIfPresent(configValue)?.get(fullPath) ?: suspendCoroutine { continuation ->
            secretsManager.getSecretValueAsync(
                GetSecretValueRequest().withSecretId(configValue),
                object : AsyncHandler<GetSecretValueRequest, GetSecretValueResult> {
                    override fun onSuccess(request: GetSecretValueRequest, result: GetSecretValueResult) {
                        val secretsMap: Map<String, String> = json.decodeFromString(result.secretString)
                        secretsCache.put(configValue, secretsMap)
                        secretsMap[fullPath]?.let {
                            continuation.resume(it)
                        } ?: continuation.resumeWithException(IllegalArgumentException("No secret found for $fullPath !"))
                    }

                    override fun onError(exception: Exception) {
                        continuation.resumeWithException(exception)
                    }
                }
            )
        }
    }
}
