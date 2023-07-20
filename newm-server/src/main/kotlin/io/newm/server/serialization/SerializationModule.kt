package io.newm.server.serialization

import io.newm.shared.serialization.BigDecimalSerializer
import io.newm.shared.serialization.BigIntegerSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import org.koin.dsl.module
import java.math.BigDecimal
import java.math.BigInteger

/**
 * Anything marked as @Contextual will use the contextualSerializersModule to pick a serializer automatically.
 */
private val contextualSerializersModule = SerializersModule {
    contextual(BigInteger::class, BigIntegerSerializer)
    contextual(BigDecimal::class, BigDecimalSerializer)
}

val serializationModule = module {
    single {
        Json {
            ignoreUnknownKeys = true
            explicitNulls = false
            isLenient = true
            serializersModule = contextualSerializersModule
        }
    }
}
