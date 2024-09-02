package io.newm.shared.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.util.UUID

object UUIDSerializer : KSerializer<UUID> {
    override val descriptor = PrimitiveSerialDescriptor("java.util.UUID", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): UUID = UUID.fromString(decoder.decodeString())

    override fun serialize(
        encoder: Encoder,
        value: UUID
    ) = encoder.encodeString(value.toString())
}
