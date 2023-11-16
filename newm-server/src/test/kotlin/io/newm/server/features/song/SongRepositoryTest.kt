package io.newm.server.features.song

import com.google.common.truth.Truth.assertThat
import io.mockk.mockk
import io.newm.server.BaseApplicationTests
import io.newm.server.features.song.repo.SongRepositoryImpl
import org.junit.jupiter.api.Test

class SongRepositoryTest : BaseApplicationTests() {

    @Test
    fun `test calculateMintPaymentResponse`() {
        val songRepository = SongRepositoryImpl(mockk(), mockk(), mockk(), mockk(), mockk(), mockk(), mockk())
        val response = songRepository.calculateMintPaymentResponse(
            dspPriceUsd = "14990000".toLong(),
            usdAdaExchangeRate = "250000".toBigInteger(),
            mintCostLovelace = "2000000".toLong(),
            sendTokenFee = "1288690".toLong(),
        )
        assertThat(response.cborHex).isEqualTo("1a03c0b180")
        assertThat(response.adaPrice).isEqualTo("61.960000")
        assertThat(response.usdPrice).isEqualTo("15.490000")
        assertThat(response.dspPriceAda).isEqualTo("59.960000")
        assertThat(response.dspPriceUsd).isEqualTo("14.990000")
        assertThat(response.sendTokenFeeAda).isEqualTo("1.288690")
        assertThat(response.sendTokenFeeUsd).isEqualTo("0.322172")
        assertThat(response.usdAdaExchangeRate).isEqualTo("0.250000")
    }
}
