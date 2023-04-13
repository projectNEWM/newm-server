package io.newm.shared.ktx

import io.ktor.server.application.ApplicationEnvironment
import io.ktor.server.config.ApplicationConfig

fun ApplicationEnvironment.getConfigChild(path: String): ApplicationConfig = config.config(path)

fun ApplicationEnvironment.getConfigChildren(path: String): List<ApplicationConfig> = config.getChildren(path)

fun ApplicationEnvironment.getConfigString(path: String): String = config.getString(path)

fun ApplicationEnvironment.getConfigStrings(path: String): List<String> = config.getStrings(path)

fun ApplicationEnvironment.getConfigSplitStrings(path: String, delimiter: String = ","): List<String> =
    config.getSplitStrings(path, delimiter)

fun ApplicationEnvironment.getConfigInt(path: String): Int = config.getInt(path)

fun ApplicationEnvironment.getConfigLong(path: String): Long = config.getLong(path)

fun ApplicationEnvironment.getConfigBoolean(path: String): Boolean = config.getBoolean(path)
