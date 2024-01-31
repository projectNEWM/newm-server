package io.newm.shared.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.math.BigDecimal
import java.math.RoundingMode

object BigDecimalSerializer : KSerializer<BigDecimal> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("BigDecimal", PrimitiveKind.STRING)

    override fun serialize(
        encoder: Encoder,
        value: BigDecimal
    ) {
        encoder.encodeString(value.toPlainString())
    }

    override fun deserialize(decoder: Decoder): BigDecimal {
        val decodedString = decoder.decodeString()
        return if ('/' in decodedString) {
            // decode rational like "3/4"
            decodedString.split('/', limit = 2).let {
                try {
                    BigDecimal(it[0]).divide(BigDecimal(it[1]))
                } catch (e: ArithmeticException) {
                    // if we can't be exact, round to 32 decimal places
                    BigDecimal(it[0]).divide(BigDecimal(it[1]), 32, RoundingMode.HALF_UP)
                }
            }
        } else {
            // decode decimal like "0.75"
            BigDecimal(decodedString)
        }
    }
}
