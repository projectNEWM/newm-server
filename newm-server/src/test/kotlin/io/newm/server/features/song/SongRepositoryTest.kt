package io.newm.server.features.song

import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.mockk
import io.newm.server.BaseApplicationTests
import io.newm.server.features.song.model.PaymentType
import io.newm.server.features.song.repo.SongRepositoryImpl
import io.newm.shared.ktx.removeCloudinaryResize
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class SongRepositoryTest : BaseApplicationTests() {
    @Test
    fun `test calculateMintPaymentResponse`() =
        runBlocking {
            val songRepository =
                SongRepositoryImpl(
                    mockk(),
                    mockk(),
                    mockk(),
                    mockk {
                        coEvery { isMainnet() } returns true
                    },
                    mockk(),
                    mockk(),
                    mockk(),
                    mockk(),
                    mockk()
                )
            val response =
                songRepository.calculateMintPaymentResponse(
                    minUtxoLovelace = 1288690L,
                    numberOfCollaborators = 3,
                    dspPriceUsdForAdaPaymentType = 14990000L,
                    usdAdaExchangeRate = "750000".toBigInteger(), // $0.75 / 1 ADA
                    dspPriceUsdForNewmPaymentType = 11990000L,
                    usdNewmExchangeRate = 800.toBigInteger(), // $0.0008 / 1 NEWM
                    mintCostBaseLovelace = 2000000L
                )
            println(response)
            assertThat(response.cborHex).isEqualTo("1a0199bd81")
            assertThat(response.adaPrice).isEqualTo("25.852737")
            assertThat(response.usdPrice).isEqualTo("19.389552")
            assertThat(response.dspPriceAda).isEqualTo("19.986667")
            assertThat(response.dspPriceUsd).isEqualTo("14.990000")
            assertThat(response.collabPriceAda).isEqualTo("3.866070")
            assertThat(response.collabPriceUsd).isEqualTo("2.899552")
            assertThat(response.usdAdaExchangeRate).isEqualTo("0.750000")

            requireNotNull(response.mintPaymentOptions)
            assertThat(response.mintPaymentOptions.size).isEqualTo(2)

            assertThat(response.mintPaymentOptions[0].paymentType).isEqualTo(PaymentType.ADA)
            assertThat(response.mintPaymentOptions[0].cborHex).isEqualTo("1a0199bd81")
            assertThat(response.mintPaymentOptions[0].price).isEqualTo("25.852737")
            assertThat(response.mintPaymentOptions[0].priceUsd).isEqualTo("19.389552")
            assertThat(response.mintPaymentOptions[0].dspPrice).isEqualTo("19.986667")
            assertThat(response.mintPaymentOptions[0].dspPriceUsd).isEqualTo("14.990000")
            assertThat(response.mintPaymentOptions[0].mintPrice).isEqualTo("2.000000")
            assertThat(response.mintPaymentOptions[0].mintPriceUsd).isEqualTo("1.500000")
            assertThat(response.mintPaymentOptions[0].collabPrice).isEqualTo("3.866070")
            assertThat(response.mintPaymentOptions[0].collabPriceUsd).isEqualTo("2.899552")
            assertThat(response.mintPaymentOptions[0].collabPricePerArtist).isEqualTo("1.288690")
            assertThat(response.mintPaymentOptions[0].collabPricePerArtistUsd).isEqualTo("0.966517")
            assertThat(response.mintPaymentOptions[0].usdToPaymentTypeExchangeRate).isEqualTo("0.750000")

            assertThat(response.mintPaymentOptions[1].paymentType).isEqualTo(PaymentType.NEWM)
            assertThat(response.mintPaymentOptions[1].cborHex).isEqualTo("821a0022ec32a1581c682fe60c9918842b3323c43b5144bc3d52a23bd2fb81345560d73f63a1444e45574d1b00000004c51de7d1")
            assertThat(response.mintPaymentOptions[1].price).isEqualTo("20486.940625")
            assertThat(response.mintPaymentOptions[1].priceUsd).isEqualTo("16.389552")
            assertThat(response.mintPaymentOptions[1].dspPrice).isEqualTo("14987.500000")
            assertThat(response.mintPaymentOptions[1].dspPriceUsd).isEqualTo("11.990000")
            assertThat(response.mintPaymentOptions[1].mintPrice).isEqualTo("1875.000000")
            assertThat(response.mintPaymentOptions[1].mintPriceUsd).isEqualTo("1.500000")
            assertThat(response.mintPaymentOptions[1].collabPrice).isEqualTo("3624.440625")
            assertThat(response.mintPaymentOptions[1].collabPriceUsd).isEqualTo("2.899552")
            assertThat(response.mintPaymentOptions[1].collabPricePerArtist).isEqualTo("1208.146875")
            assertThat(response.mintPaymentOptions[1].collabPricePerArtistUsd).isEqualTo("0.966517")
            assertThat(response.mintPaymentOptions[1].usdToPaymentTypeExchangeRate).isEqualTo("0.000800")
        }

    @Test
    fun `test removeCloudinaryResize`() {
        val url = "https://res.cloudinary.com/newm/image/upload/c_limit,w_4000,h_4000/v1746555569/1000019463_fg4vow.png"
        val result = url.removeCloudinaryResize()
        assertThat(result).isEqualTo("https://res.cloudinary.com/newm/image/upload/v1746555569/1000019463_fg4vow.png")
    }
}
