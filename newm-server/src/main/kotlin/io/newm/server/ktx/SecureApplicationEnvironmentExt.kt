package io.newm.server.ktx

import com.github.benmanes.caffeine.cache.Caffeine
import io.ktor.server.application.ApplicationEnvironment
import io.ktor.server.config.ApplicationConfig
import io.newm.shared.koin.inject
import io.newm.shared.ktx.getChildFullPath
import io.newm.shared.ktx.getString
import io.newm.shared.ktx.splitAndTrim
import java.time.Duration
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import software.amazon.awssdk.services.secretsmanager.SecretsManagerAsyncClient
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest

private val secretsManager: SecretsManagerAsyncClient by inject()
private val json: Json by inject()
private val secretsMutex = Mutex()
private val secretsCache =
    Caffeine
        .newBuilder()
        .expireAfterWrite(Duration.ofMinutes(10)) // approximately 4500 requests per month
        .build<String, Map<String, String>>()

/**
 * If the configured environment variable points to an AWS Secrets Manager arn resource, retrieve the map
 * of key/value pairs from secrets manager and then lookup the path value. Otherwise, just return the set value
 * as-is.
 */
suspend fun ApplicationEnvironment.getSecureConfigString(path: String): String = config.getSecureString(path)

suspend fun ApplicationEnvironment.getSecureConfigStrings(path: String): List<String> = config.getSecureStrings(path)

suspend fun ApplicationConfig.getSecureString(path: String): String {
    val configValue = getString(path)
    if (!configValue.startsWith("arn:aws:secretsmanager")) {
        return configValue
    }

    val fullPath = getChildFullPath(path)
    secretsMutex.withLock {
        return secretsCache.getIfPresent(configValue)?.get(fullPath) ?: suspendCoroutine { continuation ->
            val request = GetSecretValueRequest.builder().secretId(configValue).build()
            secretsManager.getSecretValue(request).whenComplete { result, throwable ->
                throwable?.let { continuation.resumeWithException(it) } ?: run {
                    val secretsMap: Map<String, String> = json.decodeFromString(result.secretString())
                    secretsCache.put(configValue, secretsMap)
                    secretsMap[fullPath]?.let { continuation.resume(it) }
                        ?: continuation.resumeWithException(IllegalArgumentException("No secret found for $fullPath !"))
                }
            }
        }
    }
}

suspend fun ApplicationConfig.getSecureStrings(path: String): List<String> = getSecureString(path).splitAndTrim()
