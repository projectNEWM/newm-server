package io.newm.shared.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.concurrent.getOrSet

// Uses dd-MM-yyyy format
object DMYLocalDateSerializer : KSerializer<LocalDate> {
    private val formatter = ThreadLocal<DateTimeFormatter>()

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("LocalDate", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): LocalDate {
        val formatter = formatter.getOrSet { DateTimeFormatter.ofPattern("dd-MM-yyyy") }
        return LocalDate.parse(decoder.decodeString(), formatter)
    }

    override fun serialize(
        encoder: Encoder,
        value: LocalDate
    ) {
        val formatter = formatter.getOrSet { DateTimeFormatter.ofPattern("dd-MM-yyyy") }
        encoder.encodeString(value.format(formatter))
    }
}
