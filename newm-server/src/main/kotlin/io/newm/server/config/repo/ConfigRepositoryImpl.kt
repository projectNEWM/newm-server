package io.newm.server.config.repo

import com.github.benmanes.caffeine.cache.Caffeine
import io.github.oshai.kotlinlogging.KotlinLogging
import io.newm.server.config.database.ConfigEntity
import io.newm.server.config.model.CidrWhitelistEntry
import io.newm.shared.ktx.existsHavingId
import io.newm.shared.ktx.splitAndTrim
import java.time.Duration
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.apache.commons.net.util.SubnetUtils
import org.jetbrains.exposed.sql.transactions.transaction

internal class ConfigRepositoryImpl : ConfigRepository {
    private val log by lazy { KotlinLogging.logger { } }

    private val configCache =
        Caffeine
            .newBuilder()
            .expireAfterWrite(Duration.ofMinutes(5))
            .build<String, String> { key ->
                transaction {
                    ConfigEntity[key].value
                }
            }

    private val ipWhitelistCache =
        Caffeine
            .newBuilder()
            .expireAfterWrite(Duration.ofMinutes(5))
            .build<String, List<CidrWhitelistEntry>> { key ->
                val json = runBlocking { getString(key) }
                if (json.isBlank() || json == "[]") {
                    emptyList()
                } else {
                    Json.decodeFromString(json)
                }
            }

    override suspend fun exists(id: String): Boolean =
        transaction {
            ConfigEntity.existsHavingId(id)
        }

    override suspend fun getString(id: String): String = configCache[id]

    override suspend fun getStrings(id: String): List<String> = getString(id).splitAndTrim()

    override suspend fun getLong(id: String): Long = getString(id).toLong()

    override suspend fun getLongs(id: String): List<Long> = getStrings(id).map(String::toLong)

    override suspend fun getInt(id: String) = getString(id).toInt()

    override suspend fun getInts(id: String): List<Int> = getStrings(id).map(String::toInt)

    override suspend fun getBoolean(id: String): Boolean = getString(id).toBoolean()

    override suspend fun getBooleans(id: String): List<Boolean> = getStrings(id).map(String::toBoolean)

    override suspend fun getDouble(id: String): Double = getString(id).toDouble()

    override suspend fun getDoubles(id: String): List<Double> = getStrings(id).map(String::toDouble)

    override suspend fun getCidrWhitelist(id: String): List<CidrWhitelistEntry> = ipWhitelistCache[id]

    override suspend fun isIpInCidrWhitelist(
        ip: String,
        whitelist: List<CidrWhitelistEntry>
    ): Boolean {
        if (whitelist.isEmpty()) return false
        return whitelist.any { entry ->
            try {
                val subnetUtils = SubnetUtils(entry.cidr)
                // Allow checking if the network address itself is in range
                subnetUtils.isInclusiveHostCount = true
                subnetUtils.info.isInRange(ip)
            } catch (_: IllegalArgumentException) {
                // Invalid CIDR notation, skip this entry
                log.warn { "Invalid CIDR notation in config: ${entry.cidr}" }
                false
            }
        }
    }

    override suspend fun putString(
        id: String,
        value: String
    ): Unit =
        transaction {
            ConfigEntity.new(id) {
                this.value = value
            }
            configCache.invalidate(id)
        }

    override fun invalidateCache() {
        configCache.invalidateAll()
    }
}
