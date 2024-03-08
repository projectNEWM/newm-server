package io.newm.shared.ktx

import com.google.common.truth.Truth
import org.junit.jupiter.api.Test

private val names =
    listOf(
        "Joh\\nathan" to "Johnathan",
        "J/ames" to "James",
        "C/hri\\sto/ph\\er" to "Christopher"
    )

class NameValidationTests {
    @Test
    fun testIsValidNameFalse() {
        for (name in names) {
            Truth.assertThat(name.first.isValidName()).isFalse()
        }
    }

    @Test
    fun testIsValidNameTrue() {
        for (name in names) {
            Truth.assertThat(name.second.isValidName()).isTrue()
        }
    }

    @Test
    fun testSanitizeName() {
        for (name in names) {
            Truth.assertThat(name.first.sanitizeName()).isEqualTo(name.second)
        }
    }
}
