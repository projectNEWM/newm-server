package io.newm.server.features.song

import com.google.common.truth.Truth.assertThat
import io.mockk.mockk
import io.newm.server.BaseApplicationTests
import io.newm.server.features.song.repo.SongRepositoryImpl
import io.newm.shared.ktx.removeCloudinaryResize
import org.junit.jupiter.api.Test

class SongRepositoryTest : BaseApplicationTests() {
    @Test
    fun `test calculateMintPaymentResponse`() {
        val songRepository =
            SongRepositoryImpl(mockk(), mockk(), mockk(), mockk(), mockk(), mockk(), mockk(), mockk(), mockk())
        val response =
            songRepository.calculateMintPaymentResponse(
                dspPriceUsd = 14990000L,
                usdAdaExchangeRate = "250000".toBigInteger(),
                minUtxo = 1288690L,
                numberOfCollaborators = 3,
                mintCostBase = 2000000L,
            )
        println(response)
        assertThat(response.cborHex).isEqualTo("1a03fbaf56")
        assertThat(response.adaPrice).isEqualTo("65.826070")
        assertThat(response.usdPrice).isEqualTo("16.456517")
        assertThat(response.dspPriceAda).isEqualTo("59.960000")
        assertThat(response.dspPriceUsd).isEqualTo("14.990000")
        assertThat(response.collabPriceAda).isEqualTo("3.866070")
        assertThat(response.collabPriceUsd).isEqualTo("0.966517")
        assertThat(response.usdAdaExchangeRate).isEqualTo("0.250000")
    }

    @Test
    fun `test removeCloudinaryResize`() {
        val url = "https://res.cloudinary.com/newm/image/upload/c_limit,w_4000,h_4000/v1746555569/1000019463_fg4vow.png"
        val result = url.removeCloudinaryResize()
        assertThat(result).isEqualTo("https://res.cloudinary.com/newm/image/upload/v1746555569/1000019463_fg4vow.png")
    }
}
