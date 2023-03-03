package io.newm.server.ext

import java.util.Properties

fun propertiesFromResource(name: String): Properties = ClassLoader.getSystemResourceAsStream(name).use { stream ->
    Properties().apply { load(stream) }
}
