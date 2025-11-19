package io.newm.server.features.song

import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.mockk
import io.newm.server.BaseApplicationTests
import io.newm.server.features.song.model.PaymentType
import io.newm.server.features.song.repo.SongRepositoryImpl
import io.newm.shared.ktx.removeCloudinaryResize
import java.math.BigInteger
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class SongRepositoryTest : BaseApplicationTests() {
    private fun buildRepository(): SongRepositoryImpl =
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

    private data class CalcParams(
        val collaborators: Int,
        val minUtxo: Long = 1_288_690L,
        val mintCostBase: Long = 2_000_000L,
        val dspUsdAda: Long = 14_990_000L,
        val usdAdaRate: BigInteger = "750000".toBigInteger(),
        val dspUsdNewm: Long = 11_990_000L,
        val usdNewmRate: BigInteger = 800.toBigInteger()
    )

    private suspend fun SongRepositoryImpl.calculateStud514(params: CalcParams) =
        calculateStud514MintPaymentResponse(
            minUtxoLovelace = params.minUtxo,
            numberOfCollaborators = params.collaborators,
            dspPriceUsdForAdaPaymentType = params.dspUsdAda,
            usdAdaExchangeRate = params.usdAdaRate,
            dspPriceUsdForNewmPaymentType = params.dspUsdNewm,
            usdNewmExchangeRate = params.usdNewmRate,
            mintCostBaseLovelace = params.mintCostBase,
        )

    private suspend fun SongRepositoryImpl.calculate(params: CalcParams) =
        calculateMintPaymentResponse(
            minUtxoLovelace = params.minUtxo,
            numberOfCollaborators = params.collaborators,
            dspPriceUsdForAdaPaymentType = params.dspUsdAda,
            usdAdaExchangeRate = params.usdAdaRate,
            dspPriceUsdForNewmPaymentType = params.dspUsdNewm,
            usdNewmExchangeRate = params.usdNewmRate,
            mintCostBaseLovelace = params.mintCostBase,
        )

    @Test
    fun `test calculateMintPaymentResponse`() =
        runBlocking {
            val songRepository = buildRepository()

            val threeCollabResponse = songRepository.calculate(CalcParams(collaborators = 3))
            val options = requireNotNull(threeCollabResponse.mintPaymentOptions)
            assertThat(options.size).isEqualTo(3)
            val adaOption = options.first { it.paymentType == PaymentType.ADA }
            val newmOption = options.first { it.paymentType == PaymentType.NEWM }
            val paypalOption = options.first { it.paymentType == PaymentType.PAYPAL }

            assertThat(adaOption.cborHex).isEqualTo("1a0199bd81")
            assertThat(adaOption.price).isEqualTo("25.852737")
            assertThat(adaOption.priceUsd).isEqualTo("19.389552")
            assertThat(adaOption.dspPrice).isEqualTo("19.986667")
            assertThat(adaOption.dspPriceUsd).isEqualTo("14.990000")
            assertThat(adaOption.mintPrice).isEqualTo("2.000000")
            assertThat(adaOption.mintPriceUsd).isEqualTo("1.500000")
            assertThat(adaOption.collabPrice).isEqualTo("3.866070")
            assertThat(adaOption.collabPriceUsd).isEqualTo("2.899552")
            assertThat(adaOption.collabPricePerArtist).isEqualTo("1.288690")
            assertThat(adaOption.collabPricePerArtistUsd).isEqualTo("0.966517")
            assertThat(adaOption.usdToPaymentTypeExchangeRate).isEqualTo("0.750000")

            assertThat(newmOption.cborHex).isEqualTo("821a0022ec32a1581c682fe60c9918842b3323c43b5144bc3d52a23bd2fb81345560d73f63a1444e45574d1b00000004c51de7d1")
            assertThat(newmOption.price).isEqualTo("20486.940625")
            assertThat(newmOption.priceUsd).isEqualTo("16.389552")
            assertThat(newmOption.dspPrice).isEqualTo("14987.500000")
            assertThat(newmOption.dspPriceUsd).isEqualTo("11.990000")
            assertThat(newmOption.mintPrice).isEqualTo("1875.000000")
            assertThat(newmOption.mintPriceUsd).isEqualTo("1.500000")
            assertThat(newmOption.collabPrice).isEqualTo("3624.440625")
            assertThat(newmOption.collabPriceUsd).isEqualTo("2.899552")
            assertThat(newmOption.collabPricePerArtist).isEqualTo("1208.146875")
            assertThat(newmOption.collabPricePerArtistUsd).isEqualTo("0.966517")
            assertThat(newmOption.usdToPaymentTypeExchangeRate).isEqualTo("0.000800")

            assertThat(paypalOption.cborHex).isEqualTo("")
            assertThat(paypalOption.price).isEqualTo("19.389552")
            assertThat(paypalOption.priceUsd).isEqualTo("19.389552")
            assertThat(paypalOption.dspPrice).isEqualTo("14.990000")
            assertThat(paypalOption.dspPriceUsd).isEqualTo("14.990000")
            assertThat(paypalOption.mintPrice).isEqualTo("1.500000")
            assertThat(paypalOption.mintPriceUsd).isEqualTo("1.500000")
            assertThat(paypalOption.collabPrice).isEqualTo("2.899552")
            assertThat(paypalOption.collabPriceUsd).isEqualTo("2.899552")
            assertThat(paypalOption.collabPricePerArtist).isEqualTo("0.966517")
            assertThat(paypalOption.collabPricePerArtistUsd).isEqualTo("0.966517")
            assertThat(paypalOption.usdToPaymentTypeExchangeRate).isEqualTo("1.000000")

            val singleCollabResponse = songRepository.calculateStud514(CalcParams(collaborators = 1))
            val singleAdaOption =
                requireNotNull(singleCollabResponse.mintPaymentOptions).first { it.paymentType == PaymentType.ADA }
            assertThat(singleAdaOption.collabPrice).isEqualTo("0.000000")
            assertThat(singleAdaOption.collabPriceUsd).isEqualTo("0.000000")
            assertThat(singleAdaOption.mintPrice).isEqualTo("3.288690")
            assertThat(singleAdaOption.mintPriceUsd).isEqualTo("2.466517")

            val noCollabResponse = songRepository.calculateStud514(CalcParams(collaborators = 0))
            val noCollabAdaOption =
                requireNotNull(noCollabResponse.mintPaymentOptions).first { it.paymentType == PaymentType.ADA }
            assertThat(noCollabAdaOption.collabPrice).isEqualTo("0.000000")
            assertThat(noCollabAdaOption.mintPrice).isEqualTo("2.000000")
        }

    @Test
    fun `test calculateStud514MintPaymentResponse`() =
        runBlocking {
            val songRepository = buildRepository()

            val threeCollabResponse = songRepository.calculateStud514(CalcParams(collaborators = 3))
            val options = requireNotNull(threeCollabResponse.mintPaymentOptions)
            assertThat(options.size).isEqualTo(3)
            val adaOption = options.first { it.paymentType == PaymentType.ADA }
            val newmOption = options.first { it.paymentType == PaymentType.NEWM }
            val paypalOption = options.first { it.paymentType == PaymentType.PAYPAL }

            assertThat(adaOption.cborHex).isEqualTo("1a0199bd81")
            assertThat(adaOption.price).isEqualTo("25.852737")
            assertThat(adaOption.priceUsd).isEqualTo("19.389552")
            assertThat(adaOption.dspPrice).isEqualTo("19.986667")
            assertThat(adaOption.dspPriceUsd).isEqualTo("14.990000")
            assertThat(adaOption.mintPrice).isEqualTo("3.288690")
            assertThat(adaOption.mintPriceUsd).isEqualTo("2.466517")
            assertThat(adaOption.collabPrice).isEqualTo("2.577380")
            assertThat(adaOption.collabPriceUsd).isEqualTo("1.933035")
            assertThat(adaOption.collabPricePerArtist).isEqualTo("1.288690")
            assertThat(adaOption.collabPricePerArtistUsd).isEqualTo("0.966517")
            assertThat(adaOption.usdToPaymentTypeExchangeRate).isEqualTo("0.750000")

            assertThat(newmOption.cborHex).isEqualTo("821a0022ec32a1581c682fe60c9918842b3323c43b5144bc3d52a23bd2fb81345560d73f63a1444e45574d1b00000004c51de7d1")
            assertThat(newmOption.price).isEqualTo("20486.940625")
            assertThat(newmOption.priceUsd).isEqualTo("16.389552")
            assertThat(newmOption.dspPrice).isEqualTo("14987.500000")
            assertThat(newmOption.dspPriceUsd).isEqualTo("11.990000")
            assertThat(newmOption.mintPrice).isEqualTo("3083.146875")
            assertThat(newmOption.mintPriceUsd).isEqualTo("2.466517")
            assertThat(newmOption.collabPrice).isEqualTo("2416.293750")
            assertThat(newmOption.collabPriceUsd).isEqualTo("1.933035")
            assertThat(newmOption.collabPricePerArtist).isEqualTo("1208.146875")
            assertThat(newmOption.collabPricePerArtistUsd).isEqualTo("0.966517")
            assertThat(newmOption.usdToPaymentTypeExchangeRate).isEqualTo("0.000800")

            assertThat(paypalOption.cborHex).isEqualTo("")
            assertThat(paypalOption.price).isEqualTo("19.389552")
            assertThat(paypalOption.priceUsd).isEqualTo("19.389552")
            assertThat(paypalOption.dspPrice).isEqualTo("14.990000")
            assertThat(paypalOption.dspPriceUsd).isEqualTo("14.990000")
            assertThat(paypalOption.mintPrice).isEqualTo("2.466517")
            assertThat(paypalOption.mintPriceUsd).isEqualTo("2.466517")
            assertThat(paypalOption.collabPrice).isEqualTo("1.933035")
            assertThat(paypalOption.collabPriceUsd).isEqualTo("1.933035")
            assertThat(paypalOption.collabPricePerArtist).isEqualTo("0.966517")
            assertThat(paypalOption.collabPricePerArtistUsd).isEqualTo("0.966517")
            assertThat(paypalOption.usdToPaymentTypeExchangeRate).isEqualTo("1.000000")

            val singleCollabResponse = songRepository.calculateStud514(CalcParams(collaborators = 1))
            val singleAdaOption =
                requireNotNull(singleCollabResponse.mintPaymentOptions).first { it.paymentType == PaymentType.ADA }
            assertThat(singleAdaOption.collabPrice).isEqualTo("0.000000")
            assertThat(singleAdaOption.collabPriceUsd).isEqualTo("0.000000")
            assertThat(singleAdaOption.mintPrice).isEqualTo("3.288690")
            assertThat(singleAdaOption.mintPriceUsd).isEqualTo("2.466517")

            val noCollabResponse = songRepository.calculateStud514(CalcParams(collaborators = 0))
            val noCollabAdaOption =
                requireNotNull(noCollabResponse.mintPaymentOptions).first { it.paymentType == PaymentType.ADA }
            assertThat(noCollabAdaOption.collabPrice).isEqualTo("0.000000")
            assertThat(noCollabAdaOption.mintPrice).isEqualTo("2.000000")
        }

    @Test
    fun `test removeCloudinaryResize`() {
        val url = "https://res.cloudinary.com/newm/image/upload/c_limit,w_4000,h_4000/v1746555569/1000019463_fg4vow.png"
        val result = url.removeCloudinaryResize()
        assertThat(result).isEqualTo("https://res.cloudinary.com/newm/image/upload/v1746555569/1000019463_fg4vow.png")
    }
}
