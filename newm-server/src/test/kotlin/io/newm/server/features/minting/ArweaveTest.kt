package io.newm.server.features.minting

import cats.Monad
import cats.arrow.FunctionK
import co.upvest.arweave4s.adt.Block
import co.upvest.arweave4s.adt.Data
import co.upvest.arweave4s.adt.Transaction
import co.upvest.arweave4s.adt.Winston
import com.google.common.truth.Truth.assertThat
import com.softwaremill.sttp.Response
import com.softwaremill.sttp.SttpBackendOptions
import com.softwaremill.sttp.Uri
import com.softwaremill.sttp.asynchttpclient.future.AsyncHttpClientFutureBackend
import io.newm.chain.util.b64ToByteArray
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import scala.Option
import scala.compat.java8.FutureConverters.toJava
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.io.Source
import java.math.BigDecimal
import java.util.concurrent.Executors
import co.upvest.arweave4s.adt.`Signable$`.`MODULE$` as signableApi
import co.upvest.arweave4s.adt.`Transaction$`.`MODULE$` as Tx
import co.upvest.arweave4s.adt.`Wallet$`.`MODULE$` as Wallet
import co.upvest.arweave4s.api.`address$`.`MODULE$` as addressApi
import co.upvest.arweave4s.api.`block$`.`MODULE$` as blockApi
import co.upvest.arweave4s.api.`package`.`Config$`.`MODULE$` as Config
import co.upvest.arweave4s.api.`package`.`future$`.`MODULE$` as future
import co.upvest.arweave4s.api.`price$`.`MODULE$` as priceApi
import co.upvest.arweave4s.api.`tx$`.`MODULE$` as txApi

class ArweaveTest {

    private val executorService = Executors.newScheduledThreadPool(5)
    private val executionContext = ExecutionContext.fromExecutorService(executorService)
    private val backend = AsyncHttpClientFutureBackend.apply(SttpBackendOptions.Default(), executionContext)
    private val config = Config.apply(
        Uri.apply("https", "arweave.net"),
        backend
    )

    @Test
    @Disabled("No need to spam arweave.net with requests.")
    fun `test Arweave current block`() = runBlocking {
        @Suppress("UNCHECKED_CAST")
        val blockFuture = blockApi.current(config, future.futureJsonHandler(executionContext)) as Future<Block>
        val block = toJava(blockFuture).await()
        println(block)
    }

    @Test
    fun `test load wallet`() {
//        val generatedWallet = Wallet.generate(SecureRandom.getInstanceStrong(), 4096)
//        val walletJson = Wallet.walletToKeyfileEncoder().apply(generatedWallet)
//        println(walletJson.toString())

        // This is a dummy randomly generated wallet from the code above. It's just used to test decoding from json
        val walletJson = """
            {
              "kty" : "RSA",
              "e" : "AQAB",
              "n" : "jKMHySE8Baw87t1i8Z9XlpANI6BKBxyWC290jcPiqnwuWDC5ypQZuGlUqE79Jm-6Q7Ro9JR4XOfHBj02bSUbmMHwBxYWr4ZdRHNtfyYGq4v-gHdhDmSW2qv7fNRJ_fmDfWtR72_te8T2Z3JzbfY103QNWbOtNh3sTZ2z7Fvgq8PG9ZW0yCUN9dXSKueIAY19az_Z0nDI_8aC12NPEuBw4_lZ0XQcBIRMSpqqEMYgB6Kqnzct3yTbQv5TewY7DuylGop73NOQSu9rGC9dI6sUWqAoPGApe74XrbWxKy6MwzVVT2EL8lth7GyEG5-AzWD_3ai_08pGXAn4MPSUQLuXS9-YRmyBxwr8TtFQBWr5HJvC3dCCJk0C6yghjcGQ_XoKKbQ0O0FW5zLSiQH-AlVMnFBKqnOPFEmMYZ_wex1DCjOlppOTnuK_CaWAgkDYvt2d_UaqeOF8XH_4yMmNmPJjEalxv58Oxc06lDxMbvz_CBmJolrVaAGnA_wR8hTBhNjH8fZJYP2wzlfcHgIiQXoLzJq8wJH6tufnQ-2gjBvCpllk5PauD6XY4UPmwyXCy5ZPXELE3U-Ya1z51rjX7-ItvGQEuP4v_PYHYrRqQy5H3uHkig5KFnpNQtKX1WqMSrp-b-mrBzTpoKx7kX-XMdtPUOLl-9QzDazyTHUSh0WrWJk",
              "d" : "Dwqy3vq8gjjRTZUFK6WoxxRCK4DN2iPfEnsJws3kNOrSRCF2BapAc_5kDRphrhY-HFaz8RDeLMWnfk3Wv-a7pGN9yiw9IenjmNolqHdHOFrhThk2msT8c4f4fnnwLgddXnE3kr3sbxP6bMcLfIPcbvDXKrRyfrrFlumaoFmVVpm6cnLgIpX__76uW2NnIw9gjjOGlyD0BpNTVDktcG3r9R4q0qGyC3Og-0u-i8u9CkQGLsBGmjrFbs1n3V0lvOtnn0aXh99i6_jegfgy5Ik8RJMjqo14cmE7BBvORrmUWZ6fML3G-tV2TESw_olZS94nj6woVD0quFZios3N-3x06oJ3xVqgbrFB5cd77CnQXHlR7x8Icvbb0mTPzGiwWaDw6CX03a1guC_4ae9CBCYBVMyAae17_trjuG1DVCf9BqDHul2BKrfN8syyvfrWBLJQrfMWjH8tdo278ja92JQoyrbDaO9qsy4XPCD_QoG1N-fn9HZZzoXmue01Bl5uisUAAYwiCU3JOeCA352RrqfSpgqXYH1PNLymlSZQ9Y-OPgzhHueZN7DfHktjOYTYjebMNDunhEWUvf04HTlwCRWxf9qCWm5977zZSVfXcle2y89AZ_DFHOokLZatQ64afLBJ5qzg3-0vKYTm0-ol8zmxmiWmHrCNPZs943LyofeeBU8",
              "p" : "ulkkSQ6UJuK2MwG_ZUpszSRBKVVjZDuXvItjJBnjfL26U3URv3BqWqjFz1nHClOyQJvCGcQJp3lEKUrMN520_UrDoziWx8U86YFrilBoAhRla1ZT2aBegO0boeOoGj21-uEeZ5nA172VHr0bQp9rjdI8pwI-uTycJEAcYzolib-smg5PXEmZ7UuRQPbAQKsRdDfroTyRE5fxiD3_9SGk6jX9Ww_iXcLsTUOPLjXSfZI_vD5Yirfn_tQMP5s_8jCwdV_YKR0OmWnZp_9NLjmx4_erCiHbPn5A9l7lq_366bqhtRTrlWZyBwDY2sLcdbjSSmhr6w5L9xqXWenwMYr6_w",
              "q" : "wTP208kMz17byVI2ZoTp2hvrq1X1R6uUCnMdnSCIecg0U7nl6jE6L1-naqr5K1OtpFZ0Sbnv2ThNEgB6wCGYIKE45yH1R4bAh9wmKK6Fjo3NQpzR72jP4VXH-TAE5QZNCNxZv-qGYb3_WCnL0CaKvJ_PF-c3ZBcRrBF4p9XjD09Sjs96TwI77m-aTmhJhzHg4xWQo0EmZpF4YelRUEoPuml5tKGCEFeUowUfHEVO36a0QsABuZwIN6sGVL4Cpq-eWRkFGqvSTXU5-VfDEIo4rVmk_q1Kw69Q73TfxVfGIuGJHTXl7w3GdygwRaiMaWkj4mtTS-ILMWVxG71rewukZw",
              "dp" : "WOy0_g_w7uV9ILYSyZlMdlB4r-rJdUlizVyOwOmBv0MiqTTSdVKFziF_08T50JnEFES_yV_dY4fg2PuSnG-rsLt-xGL6wPYEbUGPOf_IwkVNaH1XoiB_OHLtnsFutKcCMmg_BB1BAzK6-GIxogdFYfYTb3cIy4h60mmtokRbKXLIQPPXNykARVasSB33_GQl_hm5XcXzTTBS2DsN68JUWlfNQSlBUpD0BqLYrqqMedPSilgOFJRSJy61cZz6VO6bJkfIdUYic18puJuCDy8FDs5fVoR1hUrmYStD2mFm3SZsiGclTLQIcgMbeLlITv0VjOBrr02xPof39bZHIr05rQ",
              "dq" : "TKyrw2chz1aNETuwXUVYbXfxMoEdo8DjMrZ2xTn5D6O1qJi5NxUsS0rB7WJHGrvNyM-KvQuutV1TsNZB4Og0MJbrN0dRyX4gAIsNFc4VMPrzwADui_3jqkDZ6Pn2p1G11tNkRvbzN-0oXjvQKB5zpuIhVRIs-GrzV39ji_AleZj4YbbTinGPiVP_QCjBLYdNAbI4QaIEdGY9CVaIL74Eue1MILSMIqIvWfcZXqXe5IGLPoZm6NuUo0sbaxwxfydpR2DXlrsj7huc02jSAElqrtlJ0szBRCHak_2lJPNtrcAmI1KVmyazYeywI6T9fIP-esgvOYlO-d6RYfntotp-9w",
              "qi" : "h17h9z_I6_thhkLSfm7k3O52M5Z_sMZESq8F1yz7IzoHs7k1yG6Fxxr0CwkY7HwqK-d9nfyjE-h7BGe71SPXwVP3MgSMvuSHUNXdrg4h0-23UCjC5tY-hJ5lmtn5_9fHTrRC5ArucnCvfBvRXTmqly9s76G1dSpyVPtjnTymS88nqRFm5DzXmjxIm6I9q2v3vwkwuYQLc2tNlGzZDk1Cn6FGTmr2YI9_oR8ISzq8hhrHM1x8dKFM5erFRHY53e_KMHuJyB3zkag5WszKRublODYKM5bpAHps2VL1QmclNPSj1lcyz-f3gFsukCzFo4griVn6RPKT3RJsbtebVlwrtw"
            }            
        """.trimIndent()

        val wallet = Wallet.load(Source.fromString(walletJson)).get()
        println("Address: ${wallet.address()}")
        assertThat(wallet.address().toString()).isEqualTo("PcTQuFMjSR8Pb7uLxV5BmqvVJYLhFrYYjSJilcanGNg")
    }

    @Test
    @Disabled("Won't work as it doesn't use a real arweave wallet.")
    fun `test tx submit`() = runBlocking {
        val pirateIpsum = """
            Schooner ho run a rig yard run a shot across the bow trysail tack careen piracy measured fer yer chains. 
            Reef rope's end nipperkin gangplank sutler aye lad measured fer yer chains handsomely Privateer. 
            Fire ship rutters piracy gally keel swing the lead chandler jib Shiver me timbers gun. 
            Pillage haul wind grapple Gold Road gangplank fathom gun Privateer keel bilge water. 
            Deadlights barkadeer dead men tell no tales loot pink hulk ballast Spanish Main boatswain black jack.
        """.trimIndent()
        // Data to persist on the blockchain.
        val testData = Data(pirateIpsum.toByteArray())

        @Suppress("UNCHECKED_CAST")
        val priceFuture =
            priceApi.dataTransaction(
                testData,
                config,
                future.futureJsonHandlerEncodedStringHandler(executionContext)
            ) as Future<Winston>

        val price = toJava(priceFuture).await()
        println("price: ${BigDecimal(price.amount().toString()).movePointLeft(12)} AR")
//        assertThat(price.amount().toString()).isEqualTo("198697075") // price changes over time so we cannot rely on this test

        // This is a dummy randomly generated wallet from the code above. It's just used to test decoding from json
        val walletJson = """
            {
              "kty" : "RSA",
              "e" : "AQAB",
              "n" : "jKMHySE8Baw87t1i8Z9XlpANI6BKBxyWC290jcPiqnwuWDC5ypQZuGlUqE79Jm-6Q7Ro9JR4XOfHBj02bSUbmMHwBxYWr4ZdRHNtfyYGq4v-gHdhDmSW2qv7fNRJ_fmDfWtR72_te8T2Z3JzbfY103QNWbOtNh3sTZ2z7Fvgq8PG9ZW0yCUN9dXSKueIAY19az_Z0nDI_8aC12NPEuBw4_lZ0XQcBIRMSpqqEMYgB6Kqnzct3yTbQv5TewY7DuylGop73NOQSu9rGC9dI6sUWqAoPGApe74XrbWxKy6MwzVVT2EL8lth7GyEG5-AzWD_3ai_08pGXAn4MPSUQLuXS9-YRmyBxwr8TtFQBWr5HJvC3dCCJk0C6yghjcGQ_XoKKbQ0O0FW5zLSiQH-AlVMnFBKqnOPFEmMYZ_wex1DCjOlppOTnuK_CaWAgkDYvt2d_UaqeOF8XH_4yMmNmPJjEalxv58Oxc06lDxMbvz_CBmJolrVaAGnA_wR8hTBhNjH8fZJYP2wzlfcHgIiQXoLzJq8wJH6tufnQ-2gjBvCpllk5PauD6XY4UPmwyXCy5ZPXELE3U-Ya1z51rjX7-ItvGQEuP4v_PYHYrRqQy5H3uHkig5KFnpNQtKX1WqMSrp-b-mrBzTpoKx7kX-XMdtPUOLl-9QzDazyTHUSh0WrWJk",
              "d" : "Dwqy3vq8gjjRTZUFK6WoxxRCK4DN2iPfEnsJws3kNOrSRCF2BapAc_5kDRphrhY-HFaz8RDeLMWnfk3Wv-a7pGN9yiw9IenjmNolqHdHOFrhThk2msT8c4f4fnnwLgddXnE3kr3sbxP6bMcLfIPcbvDXKrRyfrrFlumaoFmVVpm6cnLgIpX__76uW2NnIw9gjjOGlyD0BpNTVDktcG3r9R4q0qGyC3Og-0u-i8u9CkQGLsBGmjrFbs1n3V0lvOtnn0aXh99i6_jegfgy5Ik8RJMjqo14cmE7BBvORrmUWZ6fML3G-tV2TESw_olZS94nj6woVD0quFZios3N-3x06oJ3xVqgbrFB5cd77CnQXHlR7x8Icvbb0mTPzGiwWaDw6CX03a1guC_4ae9CBCYBVMyAae17_trjuG1DVCf9BqDHul2BKrfN8syyvfrWBLJQrfMWjH8tdo278ja92JQoyrbDaO9qsy4XPCD_QoG1N-fn9HZZzoXmue01Bl5uisUAAYwiCU3JOeCA352RrqfSpgqXYH1PNLymlSZQ9Y-OPgzhHueZN7DfHktjOYTYjebMNDunhEWUvf04HTlwCRWxf9qCWm5977zZSVfXcle2y89AZ_DFHOokLZatQ64afLBJ5qzg3-0vKYTm0-ol8zmxmiWmHrCNPZs943LyofeeBU8",
              "p" : "ulkkSQ6UJuK2MwG_ZUpszSRBKVVjZDuXvItjJBnjfL26U3URv3BqWqjFz1nHClOyQJvCGcQJp3lEKUrMN520_UrDoziWx8U86YFrilBoAhRla1ZT2aBegO0boeOoGj21-uEeZ5nA172VHr0bQp9rjdI8pwI-uTycJEAcYzolib-smg5PXEmZ7UuRQPbAQKsRdDfroTyRE5fxiD3_9SGk6jX9Ww_iXcLsTUOPLjXSfZI_vD5Yirfn_tQMP5s_8jCwdV_YKR0OmWnZp_9NLjmx4_erCiHbPn5A9l7lq_366bqhtRTrlWZyBwDY2sLcdbjSSmhr6w5L9xqXWenwMYr6_w",
              "q" : "wTP208kMz17byVI2ZoTp2hvrq1X1R6uUCnMdnSCIecg0U7nl6jE6L1-naqr5K1OtpFZ0Sbnv2ThNEgB6wCGYIKE45yH1R4bAh9wmKK6Fjo3NQpzR72jP4VXH-TAE5QZNCNxZv-qGYb3_WCnL0CaKvJ_PF-c3ZBcRrBF4p9XjD09Sjs96TwI77m-aTmhJhzHg4xWQo0EmZpF4YelRUEoPuml5tKGCEFeUowUfHEVO36a0QsABuZwIN6sGVL4Cpq-eWRkFGqvSTXU5-VfDEIo4rVmk_q1Kw69Q73TfxVfGIuGJHTXl7w3GdygwRaiMaWkj4mtTS-ILMWVxG71rewukZw",
              "dp" : "WOy0_g_w7uV9ILYSyZlMdlB4r-rJdUlizVyOwOmBv0MiqTTSdVKFziF_08T50JnEFES_yV_dY4fg2PuSnG-rsLt-xGL6wPYEbUGPOf_IwkVNaH1XoiB_OHLtnsFutKcCMmg_BB1BAzK6-GIxogdFYfYTb3cIy4h60mmtokRbKXLIQPPXNykARVasSB33_GQl_hm5XcXzTTBS2DsN68JUWlfNQSlBUpD0BqLYrqqMedPSilgOFJRSJy61cZz6VO6bJkfIdUYic18puJuCDy8FDs5fVoR1hUrmYStD2mFm3SZsiGclTLQIcgMbeLlITv0VjOBrr02xPof39bZHIr05rQ",
              "dq" : "TKyrw2chz1aNETuwXUVYbXfxMoEdo8DjMrZ2xTn5D6O1qJi5NxUsS0rB7WJHGrvNyM-KvQuutV1TsNZB4Og0MJbrN0dRyX4gAIsNFc4VMPrzwADui_3jqkDZ6Pn2p1G11tNkRvbzN-0oXjvQKB5zpuIhVRIs-GrzV39ji_AleZj4YbbTinGPiVP_QCjBLYdNAbI4QaIEdGY9CVaIL74Eue1MILSMIqIvWfcZXqXe5IGLPoZm6NuUo0sbaxwxfydpR2DXlrsj7huc02jSAElqrtlJ0szBRCHak_2lJPNtrcAmI1KVmyazYeywI6T9fIP-esgvOYlO-d6RYfntotp-9w",
              "qi" : "h17h9z_I6_thhkLSfm7k3O52M5Z_sMZESq8F1yz7IzoHs7k1yG6Fxxr0CwkY7HwqK-d9nfyjE-h7BGe71SPXwVP3MgSMvuSHUNXdrg4h0-23UCjC5tY-hJ5lmtn5_9fHTrRC5ArucnCvfBvRXTmqly9s76G1dSpyVPtjnTymS88nqRFm5DzXmjxIm6I9q2v3vwkwuYQLc2tNlGzZDk1Cn6FGTmr2YI9_oR8ISzq8hhrHM1x8dKFM5erFRHY53e_KMHuJyB3zkag5WszKRublODYKM5bpAHps2VL1QmclNPSj1lcyz-f3gFsukCzFo4griVn6RPKT3RJsbtebVlwrtw"
            }
        """.trimIndent()

        val wallet = Wallet.load(Source.fromString(walletJson)).get()
        println("Address: ${wallet.address()}")

        @Suppress("UNCHECKED_CAST")
        val lastTxFuture = addressApi.lastTx(
            wallet.address(),
            config,
            future.futureJsonHandlerEncodedStringHandler(executionContext)
        ) as Future<Option<Transaction.Id>>
        val lastTx = toJava(lastTxFuture).await()
        println("LastTx: $lastTx")

        val transaction = Transaction.apply(
            lastTx,
            wallet.owner(),
            price,
            Option.apply(testData),
            Option.empty(),
            Option.empty(),
            Winston.Zero()
        )
        println("transaction: $transaction")
        val signedTransaction = signableApi.SignableSyntax(transaction).sign(wallet.priv())
        println("Signed<Transaction>: $signedTransaction")

        val txSigned = Tx.SignedTransaction(signedTransaction)

        val txId = txSigned.id()
        println("txId: $txId")

        val function: Function1<Future<Any>, Future<Any>> = object : Function1<Future<Any>, Future<Any>> {
            override fun invoke(p1: Future<Any>): Future<Any> {
                return p1
            }
        }
        val submitFuture = txApi.submit(signedTransaction, config, function)
        val submitResponse = toJava(submitFuture).await() as Response<*>
        println("submitResponse.javaClass: ${submitResponse.javaClass}")
        println("submitResponse: $submitResponse")
        if (submitResponse.isSuccess) {
            val either = submitResponse.body()
            either.fold({
                println("Left: $it")
            }, {
                println("Right: $it")
            })
        } else {
            // we failed!
            println("Error(${submitResponse.code()}): ${submitResponse.statusText()}")
        }

        Unit
    }

    @Test
    fun `test tx status`() = runBlocking {
        val txIdString = "dA1iVwBn9BgmcGnxAYAHrEaLp4ibaDJoFmAQecnrctA"
        val txIdBytes = txIdString.b64ToByteArray()
        val txId = Transaction.Id(txIdBytes)
        println("txId: $txId")

        val evidence = object : Monad<Future<Any>> {
            override fun <A : Any, B : Any> flatMap(
                fa: Future<Any>,
                f: scala.Function1<A, Future<Any>>
            ): Future<Any> {
                return fa
            }

            override fun <A : Any, B : Any> tailRecM(a: A, f: scala.Function1<A, Future<Any>>): Future<Any> {
                // dummy unused function
                return Future.successful(a)
            }

            override fun <A : Any> pure(x: A): Future<Any> {
                // dummy unused function
                return Future.successful(x)
            }
        }

        val function: FunctionK<*, Future<Any>> = object : FunctionK<Future<Any>, Future<Any>> {
            override fun <A : Any> apply(fa: Future<Any>): Future<Any> {
                return fa
            }
        }

        @Suppress("UNCHECKED_CAST")
        val txFuture = txApi.get(txId, evidence, config, function) as Future<Response<*>>
        println("txFuture.javaClass: ${txFuture.javaClass}")
        val txResponse = toJava(txFuture).await()
        println("tx.javaClass: ${txResponse.javaClass}")
        when (txResponse.code()) {
            200 -> println("Success, transaction is on chain!: $txIdString")
            202 -> println("Pending, transaction is not yet on chain!: $txIdString")
            404 -> println("Not Found, transaction could not be found!: $txIdString")
            else -> println("Error, something bad happened!")
        }
//        txResponse.body().fold({
//            println("Left, Error!: $it")
//        }, {
//            println("Right javaClass: ${it.javaClass}")
//            println("Right: $it")
//        })

        Unit
    }

    @Test
    @Disabled
    fun `test wallet balance`() = runBlocking {
        // This is a dummy randomly generated wallet from the code above. It's just used to test decoding from json
        val walletJson = """
            {
              "kty" : "RSA",
              "e" : "AQAB",
              "n" : "jKMHySE8Baw87t1i8Z9XlpANI6BKBxyWC290jcPiqnwuWDC5ypQZuGlUqE79Jm-6Q7Ro9JR4XOfHBj02bSUbmMHwBxYWr4ZdRHNtfyYGq4v-gHdhDmSW2qv7fNRJ_fmDfWtR72_te8T2Z3JzbfY103QNWbOtNh3sTZ2z7Fvgq8PG9ZW0yCUN9dXSKueIAY19az_Z0nDI_8aC12NPEuBw4_lZ0XQcBIRMSpqqEMYgB6Kqnzct3yTbQv5TewY7DuylGop73NOQSu9rGC9dI6sUWqAoPGApe74XrbWxKy6MwzVVT2EL8lth7GyEG5-AzWD_3ai_08pGXAn4MPSUQLuXS9-YRmyBxwr8TtFQBWr5HJvC3dCCJk0C6yghjcGQ_XoKKbQ0O0FW5zLSiQH-AlVMnFBKqnOPFEmMYZ_wex1DCjOlppOTnuK_CaWAgkDYvt2d_UaqeOF8XH_4yMmNmPJjEalxv58Oxc06lDxMbvz_CBmJolrVaAGnA_wR8hTBhNjH8fZJYP2wzlfcHgIiQXoLzJq8wJH6tufnQ-2gjBvCpllk5PauD6XY4UPmwyXCy5ZPXELE3U-Ya1z51rjX7-ItvGQEuP4v_PYHYrRqQy5H3uHkig5KFnpNQtKX1WqMSrp-b-mrBzTpoKx7kX-XMdtPUOLl-9QzDazyTHUSh0WrWJk",
              "d" : "Dwqy3vq8gjjRTZUFK6WoxxRCK4DN2iPfEnsJws3kNOrSRCF2BapAc_5kDRphrhY-HFaz8RDeLMWnfk3Wv-a7pGN9yiw9IenjmNolqHdHOFrhThk2msT8c4f4fnnwLgddXnE3kr3sbxP6bMcLfIPcbvDXKrRyfrrFlumaoFmVVpm6cnLgIpX__76uW2NnIw9gjjOGlyD0BpNTVDktcG3r9R4q0qGyC3Og-0u-i8u9CkQGLsBGmjrFbs1n3V0lvOtnn0aXh99i6_jegfgy5Ik8RJMjqo14cmE7BBvORrmUWZ6fML3G-tV2TESw_olZS94nj6woVD0quFZios3N-3x06oJ3xVqgbrFB5cd77CnQXHlR7x8Icvbb0mTPzGiwWaDw6CX03a1guC_4ae9CBCYBVMyAae17_trjuG1DVCf9BqDHul2BKrfN8syyvfrWBLJQrfMWjH8tdo278ja92JQoyrbDaO9qsy4XPCD_QoG1N-fn9HZZzoXmue01Bl5uisUAAYwiCU3JOeCA352RrqfSpgqXYH1PNLymlSZQ9Y-OPgzhHueZN7DfHktjOYTYjebMNDunhEWUvf04HTlwCRWxf9qCWm5977zZSVfXcle2y89AZ_DFHOokLZatQ64afLBJ5qzg3-0vKYTm0-ol8zmxmiWmHrCNPZs943LyofeeBU8",
              "p" : "ulkkSQ6UJuK2MwG_ZUpszSRBKVVjZDuXvItjJBnjfL26U3URv3BqWqjFz1nHClOyQJvCGcQJp3lEKUrMN520_UrDoziWx8U86YFrilBoAhRla1ZT2aBegO0boeOoGj21-uEeZ5nA172VHr0bQp9rjdI8pwI-uTycJEAcYzolib-smg5PXEmZ7UuRQPbAQKsRdDfroTyRE5fxiD3_9SGk6jX9Ww_iXcLsTUOPLjXSfZI_vD5Yirfn_tQMP5s_8jCwdV_YKR0OmWnZp_9NLjmx4_erCiHbPn5A9l7lq_366bqhtRTrlWZyBwDY2sLcdbjSSmhr6w5L9xqXWenwMYr6_w",
              "q" : "wTP208kMz17byVI2ZoTp2hvrq1X1R6uUCnMdnSCIecg0U7nl6jE6L1-naqr5K1OtpFZ0Sbnv2ThNEgB6wCGYIKE45yH1R4bAh9wmKK6Fjo3NQpzR72jP4VXH-TAE5QZNCNxZv-qGYb3_WCnL0CaKvJ_PF-c3ZBcRrBF4p9XjD09Sjs96TwI77m-aTmhJhzHg4xWQo0EmZpF4YelRUEoPuml5tKGCEFeUowUfHEVO36a0QsABuZwIN6sGVL4Cpq-eWRkFGqvSTXU5-VfDEIo4rVmk_q1Kw69Q73TfxVfGIuGJHTXl7w3GdygwRaiMaWkj4mtTS-ILMWVxG71rewukZw",
              "dp" : "WOy0_g_w7uV9ILYSyZlMdlB4r-rJdUlizVyOwOmBv0MiqTTSdVKFziF_08T50JnEFES_yV_dY4fg2PuSnG-rsLt-xGL6wPYEbUGPOf_IwkVNaH1XoiB_OHLtnsFutKcCMmg_BB1BAzK6-GIxogdFYfYTb3cIy4h60mmtokRbKXLIQPPXNykARVasSB33_GQl_hm5XcXzTTBS2DsN68JUWlfNQSlBUpD0BqLYrqqMedPSilgOFJRSJy61cZz6VO6bJkfIdUYic18puJuCDy8FDs5fVoR1hUrmYStD2mFm3SZsiGclTLQIcgMbeLlITv0VjOBrr02xPof39bZHIr05rQ",
              "dq" : "TKyrw2chz1aNETuwXUVYbXfxMoEdo8DjMrZ2xTn5D6O1qJi5NxUsS0rB7WJHGrvNyM-KvQuutV1TsNZB4Og0MJbrN0dRyX4gAIsNFc4VMPrzwADui_3jqkDZ6Pn2p1G11tNkRvbzN-0oXjvQKB5zpuIhVRIs-GrzV39ji_AleZj4YbbTinGPiVP_QCjBLYdNAbI4QaIEdGY9CVaIL74Eue1MILSMIqIvWfcZXqXe5IGLPoZm6NuUo0sbaxwxfydpR2DXlrsj7huc02jSAElqrtlJ0szBRCHak_2lJPNtrcAmI1KVmyazYeywI6T9fIP-esgvOYlO-d6RYfntotp-9w",
              "qi" : "h17h9z_I6_thhkLSfm7k3O52M5Z_sMZESq8F1yz7IzoHs7k1yG6Fxxr0CwkY7HwqK-d9nfyjE-h7BGe71SPXwVP3MgSMvuSHUNXdrg4h0-23UCjC5tY-hJ5lmtn5_9fHTrRC5ArucnCvfBvRXTmqly9s76G1dSpyVPtjnTymS88nqRFm5DzXmjxIm6I9q2v3vwkwuYQLc2tNlGzZDk1Cn6FGTmr2YI9_oR8ISzq8hhrHM1x8dKFM5erFRHY53e_KMHuJyB3zkag5WszKRublODYKM5bpAHps2VL1QmclNPSj1lcyz-f3gFsukCzFo4griVn6RPKT3RJsbtebVlwrtw"
            }
        """.trimIndent()

        val wallet = Wallet.load(Source.fromString(walletJson)).get()
        println("Address: ${wallet.address()}")

        @Suppress("UNCHECKED_CAST")
        val balanceFuture = addressApi.balance(
            wallet.address(),
            config,
            future.futureJsonHandlerEncodedStringHandler(executionContext)
        ) as Future<Winston>
        val balance = toJava(balanceFuture).await().amount().toString().toBigDecimal().movePointLeft(12)
        println("balance: $balance AR")
        assertThat(balance).isGreaterThan(BigDecimal.ZERO)
    }
}
