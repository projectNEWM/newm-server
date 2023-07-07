package io.newm.server.aws

import com.amazonaws.regions.Regions
import com.amazonaws.services.secretsmanager.AWSSecretsManagerAsyncClientBuilder
import com.amazonaws.services.secretsmanager.model.GetSecretValueRequest
import com.typesafe.config.ConfigFactory
import io.ktor.server.config.ApplicationConfig
import io.ktor.server.config.ConfigLoader
import io.ktor.server.config.HoconApplicationConfig
import kotlinx.serialization.json.Json
import java.io.File

class AwsSecretsManagerConfigLoader : ConfigLoader {
    private val secretsManager = AWSSecretsManagerAsyncClientBuilder.standard()
        .withRegion(Regions.fromName("us-west-2"))
        .build()
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    override fun load(path: String?): ApplicationConfig? {
        if ((path == null) || !path.startsWith("arn:aws:secretsmanager")) {
            return null
        }
        val result = secretsManager.getSecretValue(
            GetSecretValueRequest().withSecretId(path)
        )
        val secretsMap: Map<String, String> = json.decodeFromString(result.secretString)
        val configString = convertToNestedHocon(secretsMap)

        val file = File.createTempFile("temp", ".conf")
        file.writeText(configString)
        println(configString)
        val config = ConfigFactory.parseFile(file)
        file.delete()
        return HoconApplicationConfig(config)
    }
}

fun convertToNestedHocon(data: Map<String, Any>): String {
    val stringBuilder = StringBuilder()

    fun processKey(key: String, value: Any, indentLevel: Int) {
        val indent = " ".repeat(indentLevel * 2)
        val parts = key.split('.')
        val currentKey = parts.first()
        val remainingKey = parts.drop(1).joinToString(".")

        stringBuilder.append("$indent$currentKey")
        if (remainingKey.isNotEmpty()) {
            stringBuilder.append(" {\n")
            processKey(remainingKey, value, indentLevel + 1)
            stringBuilder.append("$indent}")
        } else {
            stringBuilder.append(" = \"$value\"")
        }
        stringBuilder.append("\n")
    }

    for ((key, value) in data) {
        processKey(key, value, 0)
    }

    return stringBuilder.toString()
}
