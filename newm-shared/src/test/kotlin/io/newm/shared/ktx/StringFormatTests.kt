package io.newm.shared.ktx

import com.google.common.truth.Truth
import org.junit.jupiter.api.Test

class StringFormatTests {
    @Test
    fun testStringFormat() {
        val one = "World"
        val two = 1
        val three = 3.1415
        val four = true
        val rawString = "one {one} two {two} three {three} four {four} five {five} ".repeat(3)
        val expectedString = "one $one two $two three $three four $four five {five} ".repeat(3)
        val actualString = rawString.format(
            mapOf(
                "one" to one,
                "two" to two,
                "three" to three,
                "four" to four
            )
        )
        Truth.assertThat(actualString).isEqualTo(expectedString)
    }
}
