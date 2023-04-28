package io.newm.server.ktx

import com.amazonaws.handlers.AsyncHandler
import com.amazonaws.services.secretsmanager.AWSSecretsManagerAsync
import com.amazonaws.services.secretsmanager.model.GetSecretValueRequest
import com.amazonaws.services.secretsmanager.model.GetSecretValueResult
import com.github.benmanes.caffeine.cache.Caffeine
import io.ktor.server.application.ApplicationEnvironment
import io.newm.shared.koin.inject
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
suspend fun ApplicationEnvironment.getSecureConfigString(path: String): String {
    val configValue = config.getString(path)
    if (!configValue.startsWith("arn:aws:secretsmanager")) {
        return configValue
    }

    secretsMutex.withLock {
        return secretsCache.getIfPresent(configValue)?.get(path) ?: suspendCoroutine { continuation ->
            secretsManager.getSecretValueAsync(
                GetSecretValueRequest().withSecretId(configValue),
                object : AsyncHandler<GetSecretValueRequest, GetSecretValueResult> {
                    override fun onSuccess(request: GetSecretValueRequest, result: GetSecretValueResult) {
                        val secretsMap: Map<String, String> = json.decodeFromString(result.secretString)
                        secretsCache.put(configValue, secretsMap)
                        secretsMap[path]?.let {
                            continuation.resume(it)
                        } ?: continuation.resumeWithException(IllegalArgumentException("No secret found for $path !"))
                    }

                    override fun onError(exception: Exception) {
                        continuation.resumeWithException(exception)
                    }
                }
            )
        }
    }
}

/**
 * If the configured environment variable points to an AWS Secrets Manager arn resource, retrieve the map
 * of key/value pairs from secrets manager and then lookup the path value. Otherwise, just return the set value
 * as-is.
 */
fun ApplicationEnvironment.getSecureConfigStringSync(path: String): String {
    val configValue = config.getString(path)
    if (!configValue.startsWith("arn:aws:secretsmanager")) {
        return configValue
    }

    val found = secretsCache.getIfPresent(configValue)?.get(path)
    if (found != null) {
        return found
    }

    try {
        val result = secretsManager.getSecretValue(GetSecretValueRequest().withSecretId(configValue))
        val secretsMap: Map<String, String> = json.decodeFromString(result.secretString)
        secretsCache.put(configValue, secretsMap)
        return secretsMap[path] ?: throw IllegalArgumentException("No secret found for $path !")
    } catch (e: Exception) {
        throw IllegalArgumentException("No secret found for $path !")
    }
}
