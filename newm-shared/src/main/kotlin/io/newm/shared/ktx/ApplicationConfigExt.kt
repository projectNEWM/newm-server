package io.newm.shared.ktx

import io.ktor.server.config.ApplicationConfig
import java.util.Collections.synchronizedMap
import java.util.WeakHashMap

private val fullPathPerConfig = synchronizedMap(WeakHashMap<ApplicationConfig, String>())

fun ApplicationConfig.getChildFullPath(path: String): String = fullPathPerConfig[this]?.let { "$it.$path" } ?: path

fun ApplicationConfig.getChild(path: String): ApplicationConfig = config(path).also { child ->
    fullPathPerConfig[child] = getChildFullPath(path)
}

fun ApplicationConfig.getChildren(path: String): List<ApplicationConfig> {
    val config = getChild(path)
    return config.toMap().map { config.getChild(it.key) }
}

fun ApplicationConfig.getString(path: String): String = property(path).getString()

fun ApplicationConfig.getStrings(path: String): List<String> = property(path).getList()

fun ApplicationConfig.getSplitStrings(path: String, delimiter: String = ","): List<String> =
    getStrings(path).flatMap { it.split(delimiter) }

fun ApplicationConfig.getInt(path: String): Int = getString(path).toInt()

fun ApplicationConfig.getLong(path: String): Long = getString(path).toLong()

fun ApplicationConfig.getBoolean(path: String): Boolean = getString(path).toBoolean()
