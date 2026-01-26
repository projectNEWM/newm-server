package io.newm.server.features.distribution.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive

/**
 * Represents an error field from the Eveara API validation response.
 * The message field can be either a simple string or a complex object.
 */
@Serializable
data class ErrorField(
    @SerialName("fields")
    val fields: String? = null,
    @SerialName("message")
    val message: ErrorFieldMessage? = null,
)

/**
 * Represents the message content which can be either a simple string
 * or a complex nested message object from Eveara API.
 */
@Serializable(with = ErrorFieldMessageSerializer::class)
sealed class ErrorFieldMessage {
    /**
     * Simple string message.
     */
    data class SimpleMessage(
        val value: String
    ) : ErrorFieldMessage()

    /**
     * Complex message object containing nested details.
     * Example:
     * {
     *   "message": ["This album is already distributed..."],
     *   "success": false,
     *   "errorFields": "date_original_release"
     * }
     */
    @Serializable
    data class ComplexMessage(
        @SerialName("message")
        val messages: List<String>? = null,
        @SerialName("success")
        val success: Boolean? = null,
        @SerialName("errorFields")
        val errorFields: String? = null,
    ) : ErrorFieldMessage()
}

/**
 * Custom serializer for [ErrorFieldMessage] that handles both string and object formats.
 */
object ErrorFieldMessageSerializer : KSerializer<ErrorFieldMessage> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("ErrorFieldMessage")

    override fun deserialize(decoder: Decoder): ErrorFieldMessage {
        val jsonDecoder = decoder as? JsonDecoder
            ?: throw IllegalArgumentException("This serializer only works with JSON")
        val element = jsonDecoder.decodeJsonElement()

        return when (element) {
            is JsonPrimitive -> ErrorFieldMessage.SimpleMessage(element.content)

            is JsonObject -> jsonDecoder.json.decodeFromJsonElement(
                ErrorFieldMessage.ComplexMessage.serializer(),
                element
            )

            else -> throw IllegalArgumentException("Unsupported ErrorFieldMessage format: $element")
        }
    }

    override fun serialize(
        encoder: Encoder,
        value: ErrorFieldMessage
    ) {
        val jsonEncoder = encoder as? JsonEncoder
            ?: throw IllegalArgumentException("This serializer only works with JSON")

        when (value) {
            is ErrorFieldMessage.SimpleMessage -> jsonEncoder.encodeJsonElement(JsonPrimitive(value.value))

            is ErrorFieldMessage.ComplexMessage -> jsonEncoder.encodeSerializableValue(
                ErrorFieldMessage.ComplexMessage.serializer(),
                value
            )
        }
    }
}
