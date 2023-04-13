package io.newm.shared.ktx

import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.Test

class JsonPrimitiveValueTests {

    @Test
    fun testStringValues() {
        val values = listOf("hello", "", "true", "1", "1.0")
        for (value in values) {
            val result = JsonPrimitive(value).value
            assertThat(result::class).isEqualTo(String::class)
            assertThat(result).isEqualTo(value)
        }
    }

    @Test
    fun testBooleanValues() {
        val values = listOf(true, false)
        for (value in values) {
            val result = JsonPrimitive(value).value
            assertThat(result::class).isEqualTo(Boolean::class)
            assertThat(result).isEqualTo(value)
        }
    }

    @Test
    fun testLongValues() {
        val values = listOf(0, 1, -1, Long.MIN_VALUE, Long.MAX_VALUE)
        for (value in values) {
            val result = JsonPrimitive(value).value
            assertThat(result::class).isEqualTo(Long::class)
            assertThat(result).isEqualTo(value)
        }
    }

    @Test
    fun testDoubleValues() {
        val values = listOf(0.0, 1.0, -1.0, Double.MIN_VALUE, Double.MAX_VALUE, Double.NaN)
        for (value in values) {
            val result = JsonPrimitive(value).value
            assertThat(result::class).isEqualTo(Double::class)
            assertThat(result).isEqualTo(value)
        }
    }
}
