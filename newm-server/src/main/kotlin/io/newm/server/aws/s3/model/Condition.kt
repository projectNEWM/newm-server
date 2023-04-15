package io.newm.server.aws.s3.model

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.descriptors.buildSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.encodeCollection
import kotlinx.serialization.json.*

@Serializable(with = Condition.Serializer::class)
sealed class Condition {
    abstract val key: String

    object Serializer : KSerializer<Condition> {
        @OptIn(InternalSerializationApi::class)
        override val descriptor: SerialDescriptor = buildSerialDescriptor("Condition", StructureKind.LIST)

        override fun serialize(encoder: Encoder, value: Condition) {
            when (value) {
                is StartsWithCondition -> encoder.encodeSerializableValue(StartsWithCondition.serializer(), value)
                is ContentLengthRangeCondition -> encoder.encodeSerializableValue(
                    ContentLengthRangeCondition.serializer(),
                    value
                )

                is EqualCondition -> encoder.encodeSerializableValue(EqualCondition.serializer(), value)
                is MapCondition -> encoder.encodeSerializableValue(MapCondition.serializer(), value)
            }
        }

        override fun deserialize(decoder: Decoder): Condition {
            val jsonArray = JsonArray.serializer().deserialize(decoder)
            return when (jsonArray[0].jsonPrimitive.toString()) {
                "starts-with" -> StartsWithCondition(
                    jsonArray[1].jsonPrimitive.toString(),
                    jsonArray[2].jsonPrimitive.toString()
                )

                "content-length-range" -> ContentLengthRangeCondition(
                    jsonArray[1].jsonPrimitive.long,
                    jsonArray[2].jsonPrimitive.long
                )

                "eq" -> EqualCondition(jsonArray[1].jsonPrimitive.toString(), jsonArray[2].jsonPrimitive.toString())
                else -> MapCondition(jsonArray[0].jsonPrimitive.toString(), jsonArray[1].jsonPrimitive.toString())
            }
        }
    }
}

@Serializable(with = StartsWithCondition.Serializer::class)
data class StartsWithCondition(
    val startsWith: String,
    val value: String,
    override val key: String = "starts-with"
) : Condition() {
    object Serializer : KSerializer<StartsWithCondition> {
        @OptIn(InternalSerializationApi::class)
        override val descriptor: SerialDescriptor = buildSerialDescriptor("StartsWithCondition", StructureKind.LIST)

        override fun serialize(encoder: Encoder, value: StartsWithCondition) {
            encoder.encodeCollection(descriptor, 3) {
                encodeStringElement(descriptor, 0, value.key)
                encodeStringElement(descriptor, 1, value.startsWith)
                encodeStringElement(descriptor, 2, value.value)
            }
        }

        override fun deserialize(decoder: Decoder): StartsWithCondition {
            val jsonArray = JsonArray.serializer().deserialize(decoder)
            return StartsWithCondition(jsonArray[1].jsonPrimitive.toString(), jsonArray[2].jsonPrimitive.toString())
        }
    }
}

@Serializable(with = EqualCondition.Serializer::class)
data class EqualCondition(
    val left: String,
    val right: String,
    override val key: String = "eq"
) : Condition() {
    object Serializer : KSerializer<EqualCondition> {
        @OptIn(InternalSerializationApi::class)
        override val descriptor: SerialDescriptor = buildSerialDescriptor("EqualCondition", StructureKind.LIST)

        override fun serialize(encoder: Encoder, value: EqualCondition) {
            encoder.encodeCollection(descriptor, 3) {
                encodeStringElement(descriptor, 0, value.key)
                encodeStringElement(descriptor, 1, value.left)
                encodeStringElement(descriptor, 2, value.right)
            }
        }

        override fun deserialize(decoder: Decoder): EqualCondition {
            val jsonArray = JsonArray.serializer().deserialize(decoder)
            return EqualCondition(jsonArray[1].jsonPrimitive.toString(), jsonArray[2].jsonPrimitive.toString())
        }
    }
}

@Serializable(with = ContentLengthRangeCondition.Serializer::class)
data class ContentLengthRangeCondition(
    val min: Long,
    val max: Long,
    override val key: String = "content-length-range"
) : Condition() {

    object Serializer : KSerializer<ContentLengthRangeCondition> {
        @OptIn(InternalSerializationApi::class)
        override val descriptor: SerialDescriptor =
            buildSerialDescriptor("ContentLengthRangeCondition", StructureKind.LIST)

        override fun serialize(encoder: Encoder, value: ContentLengthRangeCondition) {
            encoder.encodeCollection(descriptor, 3) {
                encodeStringElement(descriptor, 0, value.key)
                encodeLongElement(descriptor, 1, value.min)
                encodeLongElement(descriptor, 2, value.max)
            }
        }

        override fun deserialize(decoder: Decoder): ContentLengthRangeCondition {
            val jsonArray = JsonArray.serializer().deserialize(decoder)

            var min = 0L
            var max = 0L
            if (jsonArray.size > 1) {
                min = jsonArray[1].jsonPrimitive.long
            }
            if (jsonArray.size > 2) {
                max = jsonArray[2].jsonPrimitive.long
            }
            return ContentLengthRangeCondition(min, max)
        }
    }
}

@Serializable(with = MapCondition.Serializer::class)
data class MapCondition(
    override val key: String,
    val value: String
) : Condition() {

    object Serializer : KSerializer<MapCondition> {
        @OptIn(InternalSerializationApi::class)
        override val descriptor: SerialDescriptor = buildSerialDescriptor("MapCondition", StructureKind.MAP)

        override fun serialize(encoder: Encoder, value: MapCondition) {
            val jsonObject = buildJsonObject {
                put(value.key, value.value)
            }
            JsonObject.serializer().serialize(encoder, jsonObject)
        }

        override fun deserialize(decoder: Decoder): MapCondition {
            val jsonObject = JsonObject.serializer().deserialize(decoder)
            val key = jsonObject.keys.first()
            val value = jsonObject[key]!!.jsonPrimitive.toString()
            return MapCondition(key, value)
        }
    }
}
