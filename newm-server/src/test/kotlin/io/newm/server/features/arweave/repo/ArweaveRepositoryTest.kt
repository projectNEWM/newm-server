package io.newm.server.features.arweave.repo

import com.google.common.truth.Truth.assertThat
import io.newm.server.ktx.asValidUrl
import java.math.BigDecimal
import java.math.RoundingMode
import org.junit.jupiter.api.Test

class ArweaveRepositoryTest {
    @Test
    fun `test webp regex replacement`() {
        val urlString = "https://res.cloudinary.com/newm/image/upload/v1671486226/welnjdtkmqevkxe0lrxg.png"
        val newUrl =
            urlString.asValidUrl().replace(Regex("\\.(png|jpg|jpeg|bmp|gif|tiff)$", RegexOption.IGNORE_CASE), ".webp")
        assertThat(newUrl).isEqualTo("https://res.cloudinary.com/newm/image/upload/v1671486226/welnjdtkmqevkxe0lrxg.webp")
    }

    @Test
    fun `calculate winc per ar rate from quotes`() {
        val winc = BigDecimal("3435566")
        val ar = BigDecimal("0.000004485092334075")

        val rate = ArweaveRepositoryImpl.calculateWincPerArRate(winc, ar)

        val expectedRate = BigDecimal("765996716254.57114753814278195295662141735703526181")
        assertThat(rate).isEqualTo(expectedRate.setScale(18, RoundingMode.HALF_UP))
    }

    @Test
    fun `convert winc balance to ar using rate`() {
        val wincBalance = BigDecimal("153200000000")
        val rate = BigDecimal("765996716254.57114753814278195295662141735703526181")
            .setScale(18, RoundingMode.HALF_UP)

        val balanceAr = wincBalance.divide(rate, 18, RoundingMode.HALF_UP)

        assertThat(balanceAr).isEqualTo(BigDecimal("0.200000857378461075"))
    }
}
