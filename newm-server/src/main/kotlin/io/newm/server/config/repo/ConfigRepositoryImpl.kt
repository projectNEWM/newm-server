package io.newm.server.config.repo

import io.newm.server.config.database.ConfigEntity
import io.newm.shared.ext.existsHavingId
import io.newm.shared.ext.splitAndTrim
import org.jetbrains.exposed.sql.transactions.transaction

internal class ConfigRepositoryImpl : ConfigRepository {

    override suspend fun exists(id: String): Boolean = transaction {
        ConfigEntity.existsHavingId(id)
    }

    override suspend fun getString(id: String): String = transaction {
        ConfigEntity[id].value
    }

    override suspend fun getStrings(id: String): List<String> = getString(id).splitAndTrim()

    override suspend fun getLong(id: String): Long = getString(id).toLong()

    override suspend fun getLongs(id: String): List<Long> = getStrings(id).map(String::toLong)

    override suspend fun getInt(id: String) = getString(id).toInt()

    override suspend fun getInts(id: String): List<Int> = getStrings(id).map(String::toInt)

    override suspend fun getBoolean(id: String): Boolean = getString(id).toBoolean()

    override suspend fun getBooleans(id: String): List<Boolean> = getStrings(id).map(String::toBoolean)
}
