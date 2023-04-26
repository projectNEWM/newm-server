package io.newm.server.config.repo

interface ConfigRepository {
    suspend fun exists(id: String): Boolean
    suspend fun getString(id: String): String
    suspend fun getStrings(id: String): List<String>
    suspend fun getLong(id: String): Long
    suspend fun getLongs(id: String): List<Long>
    suspend fun getInt(id: String): Int
    suspend fun getInts(id: String): List<Int>
    suspend fun getBoolean(id: String): Boolean
    suspend fun getBooleans(id: String): List<Boolean>
    suspend fun putString(id: String, value: String)
}
