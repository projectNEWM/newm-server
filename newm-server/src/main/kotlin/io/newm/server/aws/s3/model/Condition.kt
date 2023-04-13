package io.newm.server.aws.s3.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*

@Serializable(with = Condition.Serializer::class)
sealed class Condition {
    abstract val key: String

    object Serializer : KSerializer<Condition> {
        override val descriptor: SerialDescriptor = JsonArray.serializer().descriptor
        override fun serialize(encoder: Encoder, value: Condition) {
            when (value) {
                is StartsWithCondition -> {
                    StartsWithCondition.serializer().serialize(encoder, value)
                }

                is ContentLengthRangeCondition -> {
                    ContentLengthRangeCondition.serializer().serialize(encoder, value)
                }

                is EqualCondition -> {
                    EqualCondition.Serializer.serialize(encoder, value)
                }

                is MapCondition -> {
                    MapCondition.Serializer.serialize(encoder, value)
                }
            }
        }

        override fun deserialize(decoder: Decoder): Condition {
            TODO("Not yet implemented")
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
        override val descriptor: SerialDescriptor = JsonArray.serializer().descriptor

        override fun serialize(encoder: Encoder, value: StartsWithCondition) {
            val jsonArray = buildJsonArray {
                add(JsonPrimitive(value.key))
                add(JsonPrimitive(value.startsWith))
                add(JsonPrimitive(value.value))
            }
            JsonArray.serializer().serialize(encoder, jsonArray)
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
        override val descriptor: SerialDescriptor = JsonArray.serializer().descriptor

        override fun serialize(encoder: Encoder, value: EqualCondition) {
            val jsonArray = buildJsonArray {
                add(JsonPrimitive(value.key))
                add(JsonPrimitive(value.left))
                add(JsonPrimitive(value.right))
            }
            JsonArray.serializer().serialize(encoder, jsonArray)
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
        override val descriptor: SerialDescriptor = JsonArray.serializer().descriptor

        override fun serialize(encoder: Encoder, value: ContentLengthRangeCondition) {
            val jsonArray = buildJsonArray {
                add(JsonPrimitive(value.key))
                add(JsonPrimitive(value.min))
                add(JsonPrimitive(value.max))
            }
            JsonArray.serializer().serialize(encoder, jsonArray)
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
    val value: String,
    override val key: String
) : Condition() {

    object Serializer : KSerializer<MapCondition> {
        override val descriptor: SerialDescriptor = JsonObject.serializer().descriptor

        override fun serialize(encoder: Encoder, value: MapCondition) {
            val jsonObject = buildJsonObject {
                put(value.key, value.value)
            }
            JsonObject.serializer().serialize(encoder, jsonObject)
        }

        override fun deserialize(decoder: Decoder): MapCondition {
            TODO("Not yet implemented")
        }
    }
}
