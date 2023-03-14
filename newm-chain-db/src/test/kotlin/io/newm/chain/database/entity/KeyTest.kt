package io.newm.chain.database.entity

import io.newm.chain.config.Config
import io.newm.chain.util.toHexString
import io.newm.kogmios.protocols.model.CompactGenesis
import io.newm.kogmios.serializers.BigDecimalSerializer
import io.newm.kogmios.serializers.BigIntegerSerializer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.modules.SerializersModule
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.BigInteger

class KeyTest {

    @Test
    fun `test create key`() {
        Config.genesis = genesis
        val key = Key.create()
        println("skey: ${key.skey.toHexString()}")
        println("vkey: ${key.vkey}")
    }

    companion object {
        @OptIn(ExperimentalSerializationApi::class)
        private val json = Json {
            classDiscriminator = "methodname"
            encodeDefaults = true
            explicitNulls = true
            ignoreUnknownKeys = true
            isLenient = true
            serializersModule = SerializersModule {
                contextual(BigInteger::class, BigIntegerSerializer)
                contextual(BigDecimal::class, BigDecimalSerializer)
            }
        }

        @OptIn(ExperimentalSerializationApi::class)
        private val genesis: CompactGenesis =
            json.decodeFromStream(this::class.java.classLoader.getResourceAsStream("compact-genesis-preprod.json")!!)
    }
}
