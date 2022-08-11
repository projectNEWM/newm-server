package io.newm.server.ext

import io.ktor.server.application.ApplicationEnvironment

fun ApplicationEnvironment.getConfigString(path: String) = config.property(path).getString()

fun ApplicationEnvironment.getConfigStrings(path: String) = config.property(path).getList()

fun ApplicationEnvironment.getConfigSplitStrings(path: String, delimiter: String = ",") =
    getConfigStrings(path).flatMap { it.split(delimiter) }

fun ApplicationEnvironment.getConfigInt(path: String): Int = getConfigString(path).toInt()

fun ApplicationEnvironment.getConfigLong(path: String): Long = getConfigString(path).toLong()

fun ApplicationEnvironment.getConfigBoolean(path: String): Boolean = getConfigString(path).toBoolean()
