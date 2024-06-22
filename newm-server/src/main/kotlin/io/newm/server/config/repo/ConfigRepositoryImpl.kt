package io.newm.server.config.repo

import com.github.benmanes.caffeine.cache.Caffeine
import io.newm.server.config.database.ConfigEntity
import io.newm.shared.ktx.existsHavingId
import io.newm.shared.ktx.splitAndTrim
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Duration

internal class ConfigRepositoryImpl : ConfigRepository {
    private val configCache =
        Caffeine
            .newBuilder()
            .expireAfterWrite(Duration.ofMinutes(5))
            .build<String, String> { key ->
                transaction {
                    ConfigEntity[key].value
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
