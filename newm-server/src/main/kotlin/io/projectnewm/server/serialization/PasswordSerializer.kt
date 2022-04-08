package io.projectnewm.server.serialization

import io.projectnewm.server.features.user.model.Password
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object PasswordSerializer : KSerializer<Password> {
    override val descriptor = PrimitiveSerialDescriptor("Password", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Password = Password(decoder.decodeString())

    override fun serialize(encoder: Encoder, value: Password) = encoder.encodeString(value.value)
}
