package io.newm.txbuildertest

import com.google.common.truth.Truth.assertThat
import com.google.iot.cbor.CborArray
import com.google.iot.cbor.CborByteString
import com.google.iot.cbor.CborInteger
import com.google.iot.cbor.CborMap
import com.google.iot.cbor.CborReader
import com.google.iot.cbor.CborTextString
import com.google.protobuf.ByteString
import io.newm.chain.grpc.RedeemerTag
import io.newm.chain.grpc.nativeAsset
import io.newm.chain.grpc.outputUtxo
import io.newm.chain.grpc.plutusData
import io.newm.chain.grpc.plutusDataList
import io.newm.chain.grpc.redeemer
import io.newm.chain.grpc.signature
import io.newm.chain.grpc.signingKey
import io.newm.chain.grpc.utxo
import io.newm.chain.util.Bech32
import io.newm.chain.util.Blake2b
import io.newm.chain.util.hexToByteArray
import io.newm.chain.util.toHexString
import io.newm.kogmios.StateQueryClient
import io.newm.kogmios.createTxSubmitClient
import io.newm.kogmios.protocols.model.Ada
import io.newm.kogmios.protocols.model.Lovelace
import io.newm.kogmios.protocols.model.ParamsUtxoByAddresses
import io.newm.kogmios.protocols.model.Transaction
import io.newm.kogmios.protocols.model.UtxoOutputValue
import io.newm.kogmios.protocols.model.result.EvaluateTxResult
import io.newm.kogmios.protocols.model.result.UtxoResultItem
import io.newm.txbuilder.TransactionBuilder
import io.newm.txbuilder.TransactionBuilder.Companion.transactionBuilder
import io.newm.txbuilder.ktx.toCborObject
import java.util.UUID
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

@Disabled("Disabled until we get demeter working again.")
class TransactionBuilderTest {
    companion object {
        private const val TEST_HOST = "localhost"
        private const val TEST_PORT = 1337
        private const val TEST_SECURE = false

//        private const val TEST_HOST = "clockwork"
//        private const val TEST_PORT = 1337
//        private const val TEST_SECURE = true
    }

    @Test
    fun `test create challenge transaction`() =
        runBlocking {
            createTxSubmitClient(
                websocketHost = TEST_HOST,
                websocketPort = TEST_PORT,
                secure = TEST_SECURE,
            ).use { client ->
                val connectResult = client.connect()
                assertThat(connectResult).isTrue()
                assertThat(client.isConnected).isTrue()

                val protocolParametersResponse = (client as StateQueryClient).protocolParameters()
                val protocolParams = protocolParametersResponse.result

                val stakeAddress = "stake_test1upfa42cuzftdzkg4pmfx80kqsln2vyymgedsz58fuwa5y6gjft7zv"
                val challengeId = UUID.randomUUID()

                val challengeString = """{"connectTo":"NEWM Mobile $challengeId","stakeAddress":"$stakeAddress"}"""

                val (txId, cborBytes) =
                    transactionBuilder(protocolParams) {
                        // input utxo
                        sourceUtxos {
                            add(
                                utxo {
                                    hash = "6a6b53d93e01a597e825844d2e524479c17cc4cf2285e0ca819177566a9015cc"
                                    ix = 1
                                    lovelace = "50885953403"
                                }
                            )
                        }

                        // dummy change address
                        changeAddress =
                            "addr_test1qqa9e0qfjgge2r39lxrh4dat6c7s2m23t0tysga9m6pacfjnm243cyjk69v32rkjvwlvpplx5cgfk3jmq9gwncamgf5sg8turc"

                        // ensures this tx is expired because of 1 time to live
                        ttlAbsoluteSlot = 1L

                        // require the stake key to sign the transaction
                        requiredSigners {
                            add(Bech32.decode(stakeAddress).bytes.copyOfRange(1, 29))
                        }

                        transactionMetadata =
                            CborMap.create(
                                mapOf(
                                    CborInteger.create(674) to
                                        CborMap.create(
                                            mapOf(
                                                CborTextString.create("msg") to
                                                    CborArray.create().apply {
                                                        challengeString.chunked(64).forEach {
                                                            add(CborTextString.create(it))
                                                        }
                                                    }
                                            )
                                        )
                                )
                            )
                    }

                println("txId: $txId")
                println("cborBytes: ${cborBytes.toHexString()}")
            }
        }

    @Test
    fun `test chaining of smart contracts`() =
        runBlocking {
            createTxSubmitClient(
                websocketHost = TEST_HOST,
                websocketPort = TEST_PORT,
                secure = TEST_SECURE,
            ).use { client ->
                val connectResult = client.connect()
                assertThat(connectResult).isTrue()
                assertThat(client.isConnected).isTrue()

                val protocolParametersResponse = (client as StateQueryClient).protocolParameters()
                val protocolParams = protocolParametersResponse.result

                val json = Json { ignoreUnknownKeys = true }

                val contractAddress = "addr_test1wqgscwtjkdj6warzfhytl3wgk95vdfv0exdnulh5vl94wuc46cwh3"

                // get the funds address from contract_chaining_preprod_test.addr
                val fundsAddress = javaClass.getResource("/contract_chaining_preprod_test.addr")!!.readText()

                // get the vkey for funds address
                val fundsVkey = javaClass.getResource("/contract_chaining_preprod_test.vkey")!!.readText()
                val fundsVkeyJsonElement = json.parseToJsonElement(fundsVkey)
                val fundsVkeyCbor =
                    fundsVkeyJsonElement.jsonObject["cborHex"]!!
                        .jsonPrimitive.content
                        .hexToByteArray()
                val fundsVkeyBytes =
                    (CborReader.createFromByteArray(fundsVkeyCbor).readDataItem() as CborByteString).byteArrayValue()[0]

                // get the skey for funds address
                val fundsSkey = javaClass.getResource("/contract_chaining_preprod_test.skey")!!.readText()
                val fundsSkeyJsonElement = json.parseToJsonElement(fundsSkey)
                val fundsSkeyCbor =
                    fundsSkeyJsonElement.jsonObject["cborHex"]!!
                        .jsonPrimitive.content
                        .hexToByteArray()
                val fundsSkeyBytes =
                    (CborReader.createFromByteArray(fundsSkeyCbor).readDataItem() as CborByteString).byteArrayValue()[0]

                val utxosResponse =
                    (client as StateQueryClient).utxo(
                        params =
                            ParamsUtxoByAddresses(
                                addresses = listOf(fundsAddress),
                            )
                    )
                // ada-only utxos as srcInputs
                var srcUtxos =
                    utxosResponse.result.filter {
                        it.value.assets.isNullOrEmpty() && it.datum == null && it.datumHash == null && it.script == null
                    }
                assertThat(srcUtxos).isNotEmpty()

                // get the contract utxos and choose the first one with 3 ada on it
                val contractUtxosResponse =
                    (client as StateQueryClient).utxo(
                        params =
                            ParamsUtxoByAddresses(
                                addresses = listOf(contractAddress),
                            )
                    )
                var contractUtxo =
                    contractUtxosResponse.result.first {
                        it.value.assets.isNullOrEmpty() && it.value.ada.ada.lovelace == 3_000_000.toBigInteger() && it.datum == "d87980"
                    }

                val calculateTxExecutionUnits: suspend (ByteArray) -> EvaluateTxResult = { cborBytes ->
                    val evaluateResponse = client.evaluate(cborBytes.toHexString())
                    println("evaluateResponse: ${evaluateResponse.result}")
                    evaluateResponse.result
                }

                repeat(2) {
                    val sourceUtxosSorted = sortedSetOf<String>()

                    val (txId, cborBytes) =
                        transactionBuilder(protocolParams, calculateTxExecutionUnits = calculateTxExecutionUnits) {
                            sourceUtxos {
                                addAll(
                                    srcUtxos.map {
                                        utxo {
                                            hash = it.transaction.id
                                            ix = it.index.toLong()
                                            lovelace =
                                                it.value.ada.ada.lovelace
                                                    .toString()
                                        }.also { sourceUtxosSorted.add("${it.hash}${it.ix.toHexString()}") }
                                    }
                                )
                                add(
                                    utxo {
                                        hash = contractUtxo.transaction.id
                                        ix = contractUtxo.index.toLong()
                                        lovelace =
                                            contractUtxo.value.ada.ada.lovelace
                                                .toString()
                                    }.also { sourceUtxosSorted.add("${it.hash}${it.ix.toHexString()}") }
                                )
                            }

                            outputUtxos {
                                add(
                                    outputUtxo {
                                        address = contractAddress
                                        lovelace = "3000000"
                                        datum = "d87980"
                                    }
                                )
                            }

                            changeAddress = fundsAddress

                            referenceInputs {
                                add(
                                    utxo {
                                        hash = "3999f5bc08a427688fbf0c5eb972c6242b5f8c1c296b612436232decdfee03f7"
                                        ix = 1L
                                    }
                                )
                            }

                            collateralUtxos {
                                add(
                                    srcUtxos.first().let {
                                        utxo {
                                            hash = it.transaction.id
                                            ix = it.index.toLong()
                                            lovelace =
                                                it.value.ada.ada.lovelace
                                                    .toString()
                                        }
                                    }
                                )
                            }
                            collateralReturnAddress = fundsAddress

                            redeemers {
                                add(
                                    redeemer {
                                        tag = RedeemerTag.SPEND
                                        index =
                                            sourceUtxosSorted
                                                .indexOf("${contractUtxo.transaction.id}${contractUtxo.index.toHexString()}")
                                                .toLong()
                                        data =
                                            plutusData {
                                                constr = 0
                                                list = plutusDataList { }
                                            }
                                        // will be auto-calculated
                                        // exUnits = exUnits {
                                        //    mem = 166733L
                                        //    steps = 61712050L
                                        // }
                                    }
                                )
                            }

                            signingKeys {
                                add(
                                    signingKey {
                                        skey = ByteString.copyFrom(fundsSkeyBytes)
                                        vkey = ByteString.copyFrom(fundsVkeyBytes)
                                    }
                                )
                            }
                        }

                    println("txId: $txId")
                    println("cborBytes: ${cborBytes.toHexString()}")
                    // submit the tx
                    val submitTxResponse = client.submit(cborBytes.toHexString())
                    println("submitTxResponse: ${submitTxResponse.result}")
                    assertThat(submitTxResponse.result.transaction.id).isEqualTo(txId)

                    // Use the output of this tx as the input into the next one
                    val changeAmount =
                        (
                            (
                                (
                                    (
                                        (
                                            CborReader
                                                .createFromByteArray(
                                                    cborBytes
                                                ).readDataItem() as CborArray
                                        ).elementAt(
                                            0
                                        ) as CborMap
                                    )[
                                        CborInteger.create(
                                            1
                                        )
                                    ] as CborArray
                                ).elementAt(1) as CborMap
                            )[CborInteger.create(1)] as CborInteger
                        ).bigIntegerValue()

                    srcUtxos =
                        listOf(
                            UtxoResultItem(
                                transaction = Transaction(txId),
                                index = 1,
                                address = fundsAddress,
                                value = UtxoOutputValue(Ada(Lovelace(changeAmount))),
                            )
                        )
                    contractUtxo =
                        UtxoResultItem(
                            transaction = Transaction(txId),
                            index = 0,
                            address = contractAddress,
                            value = UtxoOutputValue(Ada(Lovelace(3_000_000.toBigInteger()))),
                            datum = "d87980",
                        )
                }
            }
        }

    @Test
    fun `test evaluateTx`() =
        runBlocking {
            createTxSubmitClient(
                websocketHost = TEST_HOST,
                websocketPort = TEST_PORT,
                secure = TEST_SECURE,
            ).use { client ->
                val connectResult = client.connect()
                assertThat(connectResult).isTrue()
                assertThat(client.isConnected).isTrue()

                val evaluateTransactionResponse =
                    client.evaluate(
                        "84ab0082825820bc0be16d6d67c2832059d733cee2b6cf15b9b382542fb0c8e091bf871601b9ca008258201fa3625ac5dabfbedfd80eedfb5bea37d8e8d66362c22300c2e4c00951449b18040183a300583930a7082a4e23a39da1b3349bba3e482b1afda48594785d6589fcf4a8f27a63728b197dae74030204d2ce9fe98def21cdc722e22b16eb4b296d01821a00463cc8a1581c36a4b27112c109a41086900abc145322b16921c522e4ff8fc9dd6978a15820000643b000094fcfc54fd5b8f7f10a7854a5e9e6905178e616fad492006fe22101028201d8185902fdd87982a7446e616d654b4275672074657374696e6745696d616765583061723a2f2f38595262786a4e5656434e79667a416e4e5249726a734a33716d4c6949455a6c4b7543347053306b416c30496d65646961547970654a696d6167652f77656270566d757369635f6d657461646174615f76657273696f6e024772656c65617365a44c72656c656173655f747970654653696e676c654d72656c656173655f7469746c654b4275672074657374696e674c72656c656173655f646174654a323032332d30392d31384b6469737472696275746f724f68747470733a2f2f6e65776d2e696f4566696c657382a3446e616d65582153747265616d696e6720526f79616c74792053686172652041677265656d656e74496d65646961547970654f6170706c69636174696f6e2f70646643737263583061723a2f2f7754755f695f6f4c3751344872482d516a30326538544d395274707535497858394f417776635f642d4541a4446e616d654b4275672074657374696e67496d65646961547970654a617564696f2f6d70656743737263583061723a2f2f4248376166736e30364562725a31726f702d303155765653484e574475367a43417154424a64583266344d44736f6e67aa4a736f6e675f7469746c654b4275672074657374696e674d736f6e675f6475726174696f6e475054324d3335534c747261636b5f6e756d62657201476172746973747381a1446e616d654876656e68616d6f6e4667656e726573814b416c7465726e617469766549636f70797269676874581fc2a92032303233206c616c616c612c20e284972032303233206c616c616c6151706172656e74616c5f61647669736f72794c4e6f6e2d4578706c69636974486578706c696369744566616c736544697372634f49452d4c4f492d32332d303132363054636f6e747269627574696e675f6172746973747381581a76656e68616d6f6e2c20436f6d706f73657220284d7573696329456c696e6b73a14774776974746572581c68747470733a2f2f747769747465722e636f6d2f76656e68616d6f6e01a2005839000f79998ccd3f948e3217584198a862893b259a52bd3804eb50185424f49434b9dcdeb8003b1a94f12cb60380caf333c6ced11fff87fa909701821a0013a9f2a1581c36a4b27112c109a41086900abc145322b16921c522e4ff8fc9dd6978a15820001bc28000094fcfc54fd5b8f7f10a7854a5e9e6905178e616fad492006fe2211a05f5e100a200581d60f4a78bbff6d5e7e492915986abc495382247af659018451a25cec92c011a00e41a49021a002dc6c0075820519f234c22bbeb8d2cda8b78444a6a51b6c5b2c4c148fb363115876a264e592609a1581c36a4b27112c109a41086900abc145322b16921c522e4ff8fc9dd6978a25820000643b000094fcfc54fd5b8f7f10a7854a5e9e6905178e616fad492006fe221015820001bc28000094fcfc54fd5b8f7f10a7854a5e9e6905178e616fad492006fe2211a05f5e1000b5820c8e18324ccf9abfedcde1de2923366c6b09cfb37dc74e4e6b5448f35c670b5680d81825820949d496d900345508ed6e0b27b2a79a284fd3c37b7a3b7b01cece027fe1577aa020e83581cf4a78bbff6d5e7e492915986abc495382247af659018451a25cec92c581ce10551a295ebb430425ab1ff35cea45be9556b2c7b2aa9468dc1c77d581c4c1017ed05f703f6cf31c6a743f2c69534dbe4846e0d51ed4cb24b8510a200581d604c1017ed05f703f6cf31c6a743f2c69534dbe4846e0d51ed4cb24b85011a000f4240111a003d09001282825820802cda6c70aedb25e1b749e9a6c14e30cbba61491995a7aacadf36e0d00e0e3800825820c33bddd4fd0f0aba6dbfb8c06176237b2fd678d2d33ea74f6b5e4e3f1ddfd8ab01a200838258200000000000000000000000000000000000000000000000000000000000000000584000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000825820000000000000000000000000000000000000000000000000000000000000000058400000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000082582000000000000000000000000000000000000000000000000000000000000000005840000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000581840100d87980821a00d59f801b00000002540be400f5d90103a100a11902a2a1636d736781694e45574d204d696e74"
//                "84ab0082825820bc0be16d6d67c2832059d733cee2b6cf15b9b382542fb0c8e091bf871601b9ca008258201fa3625ac5dabfbedfd80eedfb5bea37d8e8d66362c22300c2e4c00951449b18040183a300583930a7082a4e23a39da1b3349bba3e482b1afda48594785d6589fcf4a8f27a63728b197dae74030204d2ce9fe98def21cdc722e22b16eb4b296d01821a00463cc8a1581c36a4b27112c109a41086900abc145322b16921c522e4ff8fc9dd6978a15820000643b000094fcfc54fd5b8f7f10a7854a5e9e6905178e616fad492006fe22101028201d8185902fdd87982a7446e616d654b4275672074657374696e6745696d616765583061723a2f2f38595262786a4e5656434e79667a416e4e5249726a734a33716d4c6949455a6c4b7543347053306b416c30496d65646961547970654a696d6167652f77656270566d757369635f6d657461646174615f76657273696f6e024772656c65617365a44c72656c656173655f747970654653696e676c654d72656c656173655f7469746c654b4275672074657374696e674c72656c656173655f646174654a323032332d30392d31384b6469737472696275746f724f68747470733a2f2f6e65776d2e696f4566696c657382a3446e616d65582153747265616d696e6720526f79616c74792053686172652041677265656d656e74496d65646961547970654f6170706c69636174696f6e2f70646643737263583061723a2f2f7754755f695f6f4c3751344872482d516a30326538544d395274707535497858394f417776635f642d4541a4446e616d654b4275672074657374696e67496d65646961547970654a617564696f2f6d70656743737263583061723a2f2f4248376166736e30364562725a31726f702d303155765653484e574475367a43417154424a64583266344d44736f6e67aa4a736f6e675f7469746c654b4275672074657374696e674d736f6e675f6475726174696f6e475054324d3335534c747261636b5f6e756d62657201476172746973747381a1446e616d654876656e68616d6f6e4667656e726573814b416c7465726e617469766549636f70797269676874581fc2a92032303233206c616c616c612c20e284972032303233206c616c616c6151706172656e74616c5f61647669736f72794c4e6f6e2d4578706c69636974486578706c696369744566616c736544697372634f49452d4c4f492d32332d303132363054636f6e747269627574696e675f6172746973747381581a76656e68616d6f6e2c20436f6d706f73657220284d7573696329456c696e6b73a14774776974746572581c68747470733a2f2f747769747465722e636f6d2f76656e68616d6f6e01a2005839000f79998ccd3f948e3217584198a862893b259a52bd3804eb50185424f49434b9dcdeb8003b1a94f12cb60380caf333c6ced11fff87fa909701821a0013a9f2a1581c36a4b27112c109a41086900abc145322b16921c522e4ff8fc9dd6978a15820001bc28000094fcfc54fd5b8f7f10a7854a5e9e6905178e616fad492006fe2211a05f5e100a200581d60f4a78bbff6d5e7e492915986abc495382247af659018451a25cec92c011a00e41a49021a002dc6c0075820519f234c22bbeb8d2cda8b78444a6a51b6c5b2c4c148fb363115876a264e592609a1581c36a4b27112c109a41086900abc145322b16921c522e4ff8fc9dd6978a25820000643b000094fcfc54fd5b8f7f10a7854a5e9e6905178e616fad492006fe221015820001bc28000094fcfc54fd5b8f7f10a7854a5e9e6905178e616fad492006fe2211a05f5e1000b5820c8e18324ccf9abfedcde1de2923366c6b09cfb37dc74e4e6b5448f35c670b5680d81825820949d496d900345508ed6e0b27b2a79a284fd3c37b7a3b7b01cece027fe1577aa020e83581cf4a78bbff6d5e7e492915986abc495382247af659018451a25cec92c581ce10551a295ebb430425ab1ff35cea45be9556b2c7b2aa9468dc1c77d581c4c1017ed05f703f6cf31c6a743f2c69534dbe4846e0d51ed4cb24b8510a200581d604c1017ed05f703f6cf31c6a743f2c69534dbe4846e0d51ed4cb24b85011a000f4240111a003d09001282825820802cda6c70aedb25e1b749e9a6c14e30cbba61491995a7aacadf36e0d00e0e3800825820c33bddd4fd0f0aba6dbfb8c06176237b2fd678d2d33ea74f6b5e4e3f1ddfd8ab01a0f5d90103a100a11902a2a1636d736781694e45574d204d696e74"
//                "84ab0082825820bc0be16d6d67c2832059d733cee2b6cf15b9b382542fb0c8e091bf871601b9ca008258201fa3625ac5dabfbedfd80eedfb5bea37d8e8d66362c22300c2e4c00951449b18040183a300583930a7082a4e23a39da1b3349bba3e482b1afda48594785d6589fcf4a8f27a63728b197dae74030204d2ce9fe98def21cdc722e22b16eb4b296d01821a00463cc8a1581c36a4b27112c109a41086900abc145322b16921c522e4ff8fc9dd6978a15820000643b000094fcfc54fd5b8f7f10a7854a5e9e6905178e616fad492006fe22101028201d8185902fdd87982a7446e616d654b4275672074657374696e6745696d616765583061723a2f2f38595262786a4e5656434e79667a416e4e5249726a734a33716d4c6949455a6c4b7543347053306b416c30496d65646961547970654a696d6167652f77656270566d757369635f6d657461646174615f76657273696f6e024772656c65617365a44c72656c656173655f747970654653696e676c654d72656c656173655f7469746c654b4275672074657374696e674c72656c656173655f646174654a323032332d30392d31384b6469737472696275746f724f68747470733a2f2f6e65776d2e696f4566696c657382a3446e616d65582153747265616d696e6720526f79616c74792053686172652041677265656d656e74496d65646961547970654f6170706c69636174696f6e2f70646643737263583061723a2f2f7754755f695f6f4c3751344872482d516a30326538544d395274707535497858394f417776635f642d4541a4446e616d654b4275672074657374696e67496d65646961547970654a617564696f2f6d70656743737263583061723a2f2f4248376166736e30364562725a31726f702d303155765653484e574475367a43417154424a64583266344d44736f6e67aa4a736f6e675f7469746c654b4275672074657374696e674d736f6e675f6475726174696f6e475054324d3335534c747261636b5f6e756d62657201476172746973747381a1446e616d654876656e68616d6f6e4667656e726573814b416c7465726e617469766549636f70797269676874581fc2a92032303233206c616c616c612c20e284972032303233206c616c616c6151706172656e74616c5f61647669736f72794c4e6f6e2d4578706c69636974486578706c696369744566616c736544697372634f49452d4c4f492d32332d303132363054636f6e747269627574696e675f6172746973747381581a76656e68616d6f6e2c20436f6d706f73657220284d7573696329456c696e6b73a14774776974746572581c68747470733a2f2f747769747465722e636f6d2f76656e68616d6f6e01a2005839000f79998ccd3f948e3217584198a862893b259a52bd3804eb50185424f49434b9dcdeb8003b1a94f12cb60380caf333c6ced11fff87fa909701821a0013a9f2a1581c36a4b27112c109a41086900abc145322b16921c522e4ff8fc9dd6978a15820001bc28000094fcfc54fd5b8f7f10a7854a5e9e6905178e616fad492006fe2211a05f5e100a200581d60f4a78bbff6d5e7e492915986abc495382247af659018451a25cec92c011a00e41a49021a002dc6c0075820519f234c22bbeb8d2cda8b78444a6a51b6c5b2c4c148fb363115876a264e592609a1581c36a4b27112c109a41086900abc145322b16921c522e4ff8fc9dd6978a25820000643b000094fcfc54fd5b8f7f10a7854a5e9e6905178e616fad492006fe221015820001bc28000094fcfc54fd5b8f7f10a7854a5e9e6905178e616fad492006fe2211a05f5e1000b5820c8e18324ccf9abfedcde1de2923366c6b09cfb37dc74e4e6b5448f35c670b5680d81825820949d496d900345508ed6e0b27b2a79a284fd3c37b7a3b7b01cece027fe1577aa020e83581cf4a78bbff6d5e7e492915986abc495382247af659018451a25cec92c581ce10551a295ebb430425ab1ff35cea45be9556b2c7b2aa9468dc1c77d581c4c1017ed05f703f6cf31c6a743f2c69534dbe4846e0d51ed4cb24b8510a200581d604c1017ed05f703f6cf31c6a743f2c69534dbe4846e0d51ed4cb24b85011a000f4240111a003d09001282825820802cda6c70aedb25e1b749e9a6c14e30cbba61491995a7aacadf36e0d00e0e3800825820c33bddd4fd0f0aba6dbfb8c06176237b2fd678d2d33ea74f6b5e4e3f1ddfd8ab01a10581840100d87980821a00d59f801b00000002540be400f5d90103a100a11902a2a1636d736781694e45574d204d696e74"
//                "84ab0082825820bc0be16d6d67c2832059d733cee2b6cf15b9b382542fb0c8e091bf871601b9ca008258201fa3625ac5dabfbedfd80eedfb5bea37d8e8d66362c22300c2e4c00951449b18040183a300583930a7082a4e23a39da1b3349bba3e482b1afda48594785d6589fcf4a8f27a63728b197dae74030204d2ce9fe98def21cdc722e22b16eb4b296d01821a00463cc8a1581c36a4b27112c109a41086900abc145322b16921c522e4ff8fc9dd6978a15820000643b000094fcfc54fd5b8f7f10a7854a5e9e6905178e616fad492006fe22101028201d8185902fdd87982a7446e616d654b4275672074657374696e6745696d616765583061723a2f2f38595262786a4e5656434e79667a416e4e5249726a734a33716d4c6949455a6c4b7543347053306b416c30496d65646961547970654a696d6167652f77656270566d757369635f6d657461646174615f76657273696f6e024772656c65617365a44c72656c656173655f747970654653696e676c654d72656c656173655f7469746c654b4275672074657374696e674c72656c656173655f646174654a323032332d30392d31384b6469737472696275746f724f68747470733a2f2f6e65776d2e696f4566696c657382a3446e616d65582153747265616d696e6720526f79616c74792053686172652041677265656d656e74496d65646961547970654f6170706c69636174696f6e2f70646643737263583061723a2f2f7754755f695f6f4c3751344872482d516a30326538544d395274707535497858394f417776635f642d4541a4446e616d654b4275672074657374696e67496d65646961547970654a617564696f2f6d70656743737263583061723a2f2f4248376166736e30364562725a31726f702d303155765653484e574475367a43417154424a64583266344d44736f6e67aa4a736f6e675f7469746c654b4275672074657374696e674d736f6e675f6475726174696f6e475054324d3335534c747261636b5f6e756d62657201476172746973747381a1446e616d654876656e68616d6f6e4667656e726573814b416c7465726e617469766549636f70797269676874581fc2a92032303233206c616c616c612c20e284972032303233206c616c616c6151706172656e74616c5f61647669736f72794c4e6f6e2d4578706c69636974486578706c696369744566616c736544697372634f49452d4c4f492d32332d303132363054636f6e747269627574696e675f6172746973747381581a76656e68616d6f6e2c20436f6d706f73657220284d7573696329456c696e6b73a14774776974746572581c68747470733a2f2f747769747465722e636f6d2f76656e68616d6f6e01a2005839000f79998ccd3f948e3217584198a862893b259a52bd3804eb50185424f49434b9dcdeb8003b1a94f12cb60380caf333c6ced11fff87fa909701821a0013a9f2a1581c36a4b27112c109a41086900abc145322b16921c522e4ff8fc9dd6978a15820001bc28000094fcfc54fd5b8f7f10a7854a5e9e6905178e616fad492006fe2211a05f5e100a200581d60f4a78bbff6d5e7e492915986abc495382247af659018451a25cec92c011a00e41a49021a002dc6c0075820519f234c22bbeb8d2cda8b78444a6a51b6c5b2c4c148fb363115876a264e592609a1581c36a4b27112c109a41086900abc145322b16921c522e4ff8fc9dd6978a25820000643b000094fcfc54fd5b8f7f10a7854a5e9e6905178e616fad492006fe221015820001bc28000094fcfc54fd5b8f7f10a7854a5e9e6905178e616fad492006fe2211a05f5e1000b5820c8e18324ccf9abfedcde1de2923366c6b09cfb37dc74e4e6b5448f35c670b5680d81825820949d496d900345508ed6e0b27b2a79a284fd3c37b7a3b7b01cece027fe1577aa020e83581cf4a78bbff6d5e7e492915986abc495382247af659018451a25cec92c581ce10551a295ebb430425ab1ff35cea45be9556b2c7b2aa9468dc1c77d581c4c1017ed05f703f6cf31c6a743f2c69534dbe4846e0d51ed4cb24b8510a200581d604c1017ed05f703f6cf31c6a743f2c69534dbe4846e0d51ed4cb24b85011a000f4240111a003d09001282825820802cda6c70aedb25e1b749e9a6c14e30cbba61491995a7aacadf36e0d00e0e3800825820c33bddd4fd0f0aba6dbfb8c06176237b2fd678d2d33ea74f6b5e4e3f1ddfd8ab01a200838258200000000000000000000000000000000000000000000000000000000000000000584000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000825820000000000000000000000000000000000000000000000000000000000000000058400000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000082582000000000000000000000000000000000000000000000000000000000000000005840000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000581840100d87980821b00000002540be4001b00000002540be400f5d90103a100a11902a2a1636d736781694e45574d204d696e74"
                    )
                println("evaluateTransactionResponse: $evaluateTransactionResponse")
                assertThat(evaluateTransactionResponse).isNotNull()
                assertThat(evaluateTransactionResponse.result).isInstanceOf(EvaluateTxResult::class.java)
            }
        }

    @Test
    fun `test encoded copyright`() =
        runBlocking {
            val hex = "c2a9206e756c6c206e756c6c2c20e28497206e756c6c206e756c6c"
            val bytes = hex.hexToByteArray()
            val str = String(bytes, Charsets.UTF_8)
            println("str: $str")
        }

    @Test
    fun `test createReferenceScripts`() =
        runBlocking {
            createTxSubmitClient(
                websocketHost = TEST_HOST,
                websocketPort = TEST_PORT,
                secure = TEST_SECURE,
            ).use { client ->
                val connectResult = client.connect()
                assertThat(connectResult).isTrue()
                assertThat(client.isConnected).isTrue()

                val response = (client as StateQueryClient).protocolParameters()
                val protocolParams = response.result

                val (_, cborByteArray) =
                    transactionBuilder(protocolParams) {
                        sourceUtxos {
                            add(
                                utxo {
                                    hash = "949d496d900345508ed6e0b27b2a79a284fd3c37b7a3b7b01cece027fe1577aa"
                                    ix = 0
                                    lovelace = "300000000"
                                }
                            )
                        }

                        changeAddress = "addr_test1vrdqad0dwcg5stk9pzdknkrsurzkc8z9rqp9vyfrnrsgxkc4r8za2"

                        outputUtxos {
                            add(
                                outputUtxo {
                                    address = "addr_test1vqpqvhh4am6lqh99068n3eu2kp6zyx0ng7g8wa3c0z888rgk2dz6k"
                                    // lovelace = "26467710" // if not defined, calculate it manually
                                    scriptRef =
                                        "820259172b59172801000032332232323232323232323232332232323233223232323232323322323232323232323232323232323232323232323232323232323232323232323232323232323232323232323232323232323223232322322323253353232323232323232533500713355059305c4901124c6f636b696e673a4275726e204572726f72003335503f30501200123332001505900105f3503012233002335505b305e491105369676e696e67205478204572726f7200323232330020010393503412233002303235036122330024837804cc00920fe03330024836804cc009206633002482a808cc009209e02330024832804cc00920043300248168cc00920cc03330024825004cc00920d60233002482980ccc009208203330024835004cc00920f40133002483f004cc009200e33002483d008cc00920ae03330024834808cc009209401330024822804cc009208a0233002482700ccc009203a330024836008cc00920ec01001330023032350361223300248098cc00920c20333002483d804cc00920c001330024839004cc0092000330024820808cc009208001330024832808cc00920b4023300248110cc0092080013300248158cc00920aa0333002483780ccc00920880133002483d008cc00920ee0333002482e004cc009208e0333002483b00ccc00920a20233002481d8cc00920980133002483900ccc009203233002482c008cc009203c0013300230323503612233002482b008cc009203833002482900ccc00920ca0333002483200ccc00920103300248028cc00920463300248170cc009201433002482b80ccc00920e40233002483a004cc00920f80333002482d804cc00920f602330024824804cc00920de0333002480b0cc00920f001330024820808cc00920c80233002482c004cc009203e33002482b804cc009209a02330024827804cc00920c60100100132001355062222533500213303e001480108854cd4cc0680180084cc014004cdc000181d899802800801a8039980119aa82d982f24811353696e676c6520496e2f4f7574204572726f720033057330343018500748008cc0cd40192002330023305e4901114e4654204275726e696e67204572726f72005335303b301750071305c498884d40088894cd40104cc170cc16800cc11403ccc13c00520012213063498cc008cd5416cc17924113496e76616c696420446174756d204572726f72003004500633002335505b305e490115496e76616c6964205374617274657220546f6b656e003350395005502d00113355059305c491124c6f636b696e673a4d696e74204572726f72003335503f30501200123332001505900105f3503012233002335505b305e491105369676e696e67205478204572726f7200330145007302e350321223300248170cc00920f002330024834004cc00920da01330024824008cc00920543300248070cc00920ba0333002482480ccc009208e0333002483b00ccc009209c0233002482700ccc009209e0233002480c8cc009209e0233002482b808cc009207833002483480ccc009200833002483b804cc00920ea0233002480e0cc00920c00233002480b8cc00920e403330024831008cc00920ba0100133002335505b305e49011353696e676c6520496e2f4f7574204572726f720033057330343018500748008cc0cd40192002330023305e4901114e4654204d696e74696e67204572726f72005335303b301750071305d498884d40088894cd40104cc170cc16800cc11403ccc170cc168008c8c8cdc5001299a999ab9a3370e00207a0ce0cc2046266042a66a666ae68cdc381101e8338330a82d8999812000811282da441003045010304300f3304f00148008884c1912633002335505b305e490113496e76616c696420446174756d204572726f72003003500633002335505b305e490113496e76616c696420537461727420546f6b656e003350395005502d0013200135505e22533500113305d491194e6f20496e6372656173696e6720446174756d20466f756e640005e22153353332001504a301600250061533353017002130040012153353320015044001213305a33058304300d30430013305a3304d32337000029001182100698210009982c182080698208008980280110980280109802000990009aa82e91299a80089982e2481174e6f20436f6e7374616e7420446174756d20466f756e640005d221533533320015049301500250051533353016002130040012153353320015043001215335333573466e3cd403088800cd400488800c18818454cd4ccd5cd19b873500c222002350012220020620611333573466e3cd4030888004d4004888004188184418441844c01400884c0140084c01000454cd4c0c800c84cd5415c044d4004880044d40512401154e6f20496e70757420746f2056616c69646174652e0015335303100221350012235001222235009223500222222222222233355305c120012235002222253353501822350062232335005233500425335333573466e3c0080041f01ec5400c41ec81ec8cd401081ec94cd4ccd5cd19b8f00200107c07b15003107b1533500321533500221335002233500223350022335002233074002001207e2335002207e23307400200122207e222335004207e2225335333573466e1c01800c204042000454cd4ccd5cd19b87005002081010800113306b00400110800110800110791533500121079107913350680060051005506300a13263203e335738921024c6600042135001220023333573466e1cd55cea80224000466442466002006004646464646464646464646464646666ae68cdc39aab9d500c480008cccccccccccc88888888888848cccccccccccc00403403002c02802402001c01801401000c008cd4094098d5d0a80619a8128131aba1500b33502502735742a014666aa052eb940a0d5d0a804999aa814bae502835742a01066a04a05c6ae85401cccd540a40bdd69aba150063232323333573466e1cd55cea801240004664424660020060046464646666ae68cdc39aab9d5002480008cc8848cc00400c008cd40e5d69aba15002303a357426ae8940088c98c814ccd5ce01d82b82889aab9e5001137540026ae854008c8c8c8cccd5cd19b8735573aa004900011991091980080180119a81cbad35742a00460746ae84d5d1280111931902999ab9c03b057051135573ca00226ea8004d5d09aba2500223263204f33573806e0a609a26aae7940044dd50009aba1500533502575c6ae854010ccd540a40ac8004d5d0a801999aa814bae200135742a004605a6ae84d5d1280111931902599ab9c03304f049135744a00226ae8940044d5d1280089aba25001135744a00226ae8940044d5d1280089aba25001135744a00226ae8940044d55cf280089baa00135742a008603a6ae84d5d1280211931901e99ab9c02504103b3333573466e1d40152002212200223333573466e1d40192000212200123263203d33573804a0820760746666ae68cdc39aab9d5008480008ccc0ecdd71aba15008375a6ae85401cdd71aba135744a00e464c6407666ae7008c0fc0e440f84d403d24010350543500135573ca00226ea80044d55ce9baa001135744a00226aae7940044dd5000911a801111111111111299a999aa982609000a8191299a999ab9a3371e01c0020b60b426a0840022a082008420b620b246a002444400646a002444400446a00244444444444401046a0024444444444440182464c6405866ae700040c0c8004d5411c8894cd40084004884cc014004cdc5001a99a999ab9a3370e004042096094200e2a66a666ae68cdc38010100258250a44101310015335333573466e1c009200404b04a148901320015335333573466e1c009200604b04a148901330015335333573466e1c009200804b04a148901340015335333573466e1c009200a04b04a148901350015335333573466e1c009200c04b04a148901360015335333573466e1c009200e04b04a148901370015335333573466e1c009201004b04a148901380015335333573466e1c009201204b04a148901390015335333573466e1c00801812c1285220101610015335333573466e1c009201604b04a148901620015335333573466e1c009201804b04a148901630015335333573466e1c009201a04b04a148901640015335333573466e1c009201c04b04a148901650015335333573466e1c009201e04b04a1489016600101c480512210130003200135504422225335333573466e1c00c07411c11840044ccc010cdc180180100119a81d19b860030020011232230023758002640026aa088446666aae7c004940e88cd40e4c010d5d080118019aba200202d232323333573466e1cd55cea8012400046644246600200600460146ae854008c014d5d09aba2500223263202933573802205a04e26aae7940044dd50009191919191999ab9a3370e6aae75401120002333322221233330010050040030023232323333573466e1cd55cea8012400046644246600200600460266ae854008cd4034048d5d09aba2500223263202e33573802c06405826aae7940044dd50009aba150043335500875ca00e6ae85400cc8c8c8cccd5cd19b875001480108c84888c008010d5d09aab9e500323333573466e1d4009200223212223001004375c6ae84d55cf280211999ab9a3370ea00690001091100191931901819ab9c01803402e02d02c135573aa00226ea8004d5d0a80119a804bae357426ae8940088c98c80a8cd5ce00901701409aba25001135744a00226aae7940044dd5000899aa800bae75a224464460046eac004c8004d5410488c8cccd55cf8011281c119a81b99aa81a18031aab9d5002300535573ca00460086ae8800c0ac4d5d080089119191999ab9a3370ea002900011a81498029aba135573ca00646666ae68cdc3a801240044a052464c6404e66ae7003c0ac0940904d55cea80089baa001232323333573466e1d400520062321222230040053007357426aae79400c8cccd5cd19b875002480108c848888c008014c024d5d09aab9e500423333573466e1d400d20022321222230010053007357426aae7940148cccd5cd19b875004480008c848888c00c014dd71aba135573ca00c464c6404e66ae7003c0ac09409008c0884d55cea80089baa001232323333573466e1cd55cea80124000466442466002006004600a6ae854008dd69aba135744a004464c6404666ae7002c09c0844d55cf280089baa0012323333573466e1cd55cea800a400046eb8d5d09aab9e500223263202133573801204a03e26ea80048c8c8c8c8c8cccd5cd19b8750014803084888888800c8cccd5cd19b875002480288488888880108cccd5cd19b875003480208cc8848888888cc004024020dd71aba15005375a6ae84d5d1280291999ab9a3370ea00890031199109111111198010048041bae35742a00e6eb8d5d09aba2500723333573466e1d40152004233221222222233006009008300c35742a0126eb8d5d09aba2500923333573466e1d40192002232122222223007008300d357426aae79402c8cccd5cd19b875007480008c848888888c014020c038d5d09aab9e500c23263202a33573802405c05004e04c04a04804604426aae7540104d55cf280189aab9e5002135573ca00226ea80048c8c8c8c8cccd5cd19b875001480088ccc888488ccc00401401000cdd69aba15004375a6ae85400cdd69aba135744a00646666ae68cdc3a80124000464244600400660106ae84d55cf280311931901199ab9c00b027021020135573aa00626ae8940044d55cf280089baa001232323333573466e1d400520022321223001003375c6ae84d55cf280191999ab9a3370ea004900011909118010019bae357426aae7940108c98c8080cd5ce00401200f00e89aab9d50011375400224464646666ae68cdc3a800a40084244400246666ae68cdc3a8012400446424446006008600c6ae84d55cf280211999ab9a3370ea00690001091100111931901099ab9c00902501f01e01d135573aa00226ea80048c8cccd5cd19b8750014800880e08cccd5cd19b8750024800080e08c98c8074cd5ce00281080d80d09aab9d37540029201035054310013232335028335502500233502833550250014800940a540a4c008d4018488cc009209c01330024822804cc00920ae01330024826804cc00920be0100130013500512233002483e80ccc0092074330024834804cc00920820233002483f804cc00920ca0333002483c808cc00920fe0333002481c8cc00920f6033300248178cc00920d8023300248158cc00920c40333002482380ccc00920e4033300248000cc00920ee01330024823004cc00920d0033300248138cc00920dc033300248188cc009208c02330024827808cc00920cc03330024833804cc009209a0300123003300200132001355032225335001150272213350283371600400c6008002640026aa06244a66a002200644266e28008c01000522100123350015022502322323300100300632001355030222533500213301c00100422135002222253335002133009005007213300a0063370001001c426601400c66e0002003888c8cc00400c014c8004d540bc8894cd40084cc06c004010884d400888d400488894ccd40084cc02c01c02484cc030020cdc0005007909980600419b8000a00f48009200023500122350022222222222223333500d2501f2501f2501f233355302b1200150112350012253355335333573466e3cd400888008d4010880080f00ec4ccd5cd19b873500222001350042200103c03b103b1350230031502200d1335021225335002210031001500e1301200122333573466e2000800409c0a08cc0094068004c8004d540948894cd40044008884d400888cc01cccc02000801800400cc8004d5409088894cd40044008884d4008894cd4ccd5cd19b87001480000ac0a84ccc02001c01800c4ccc02001ccd407848ccc00402000c00801800c8d40048880048d40048880088d400488800c4488cd54008d4065405c00448c8c8c8c8ccccccd5d200291999ab9a3370e6aae7540152000233335573ea00a4a01846666aae7d4014940348cccd55cfa8029280711999aab9f35744a00c4a66aa66aa66a601c6ae85402484d4044c0380045403c854cd4c8ccccccd5d200092809128091280911a8099bad0022501201335742a012426a02460040022a0202a01e42a66a601e6ae85402084d4048c008004540405403c9403c04003c0380349402c01c9402894028940289402802c4d5d1280089aba25001135573ca00226ea800526222123330010040030022333333357480024a0064a0064a0064a00646a0086eb800801048488c00800c4488004480044c00800488ccd5cd19b8700200101801722233355300a1200135010500e2350012233355300d1200135013501123500122333500123300a4800000488cc02c0080048cc0280052000001335530091200123500122335500b002333500123355300d1200123500122335500f00235500e0010012233355500901200200123355300d1200123500122335500f00235500d00100133355500400d00200111122233355300412001500a335530081200123500122335500a00235500900133355300412001223500222533533355300d12001323350152233350032200200200135001220011233001225335002101e100101b235001223300a0020050061003133500e004003500b00133553008120012350012232335500b00330010053200135501b225335001135500a003221350022253353300c002008112223300200a0041300600300232001355014221122253350011002221330050023335530071200100500400111212223003004112122230010041122123300100300232001355010221122533500115007221335008300400233553006120010040013200135500f221122253350011350032200122133350052200230040023335530071200100500400111220021221223300100400322333573466e3c008004034030448cc00400802c894cd40084004402848cd400888ccd400c88008008004d40048800448848cc00400c0084894cd4008400454cd4004401c40204488c0080048cc00d240110426164204275726e696e6720496e666f0000423300249110426164204d696e74696e6720496e666f000032253350011004133573800400624400424400222464600200244660066004004003"
                                }
                            )
                            add(
                                outputUtxo {
                                    address = "addr_test1vqpqvhh4am6lqh99068n3eu2kp6zyx0ng7g8wa3c0z888rgk2dz6k"
                                    // lovelace = "24903180" // if not defined, calculate it manually
                                    scriptRef =
                                        "82025915c05915bd010000323322323232332232323232323232332232323232323232323232323232323232323232323232323232323232323232323232323232323232323232323232323232323232323232332232322322323253353232323232335504830534910d4d696e74696e67204572726f72003335530111200135031502f25335001105615335056105510563504512233002335504a3055491154d696e74696e672f4275726e696e67204572726f72003300e33032533530353016500613053498884d40088894cd40104cc0dcc02000ccc0dccd5413cc168cd5413dcc99b8a48915526571756972656420546f6b656e204e616d653a20003301e303d5010303e5010330420023301e303d5010303e5010335504f305a49115496e636f7272656374204d696e7420416d6f756e74003305000148008884c16926300448008cc0c94cd4c0d4c05940184c1512622135002222533500413303730080033305000148004884c16d26300448000cc008cd54128c155241105369676e696e67205478204572726f72003300e3301250063043350471223300248170cc00920f002330024834004cc00920da01330024824008cc00920543300248070cc00920ba0333002482480ccc009208e0333002483b00ccc009209c0233002482700ccc009209e0233002480c8cc009209e0233002482b808cc009207833002483480ccc009200833002483b804cc00920ea0233002480e0cc00920c00233002480b8cc00920e403330024831008cc00920ba0100132323233002001022350491223300230473504b122330024837804cc00920fe03330024836804cc009206633002482a808cc009209e02330024832804cc00920043300248168cc00920cc03330024825004cc00920d60233002482980ccc009208203330024835004cc00920f40133002483f004cc009200e33002483d008cc00920ae03330024834808cc009209401330024822804cc009208a0233002482700ccc009203a330024836008cc00920ec010013300230473504b1223300248098cc00920c20333002483d804cc00920c001330024839004cc0092000330024820808cc009208001330024832808cc00920b4023300248110cc0092080013300248158cc00920aa0333002483780ccc00920880133002483d008cc00920ee0333002482e004cc009208e0333002483b00ccc00920a20233002481d8cc00920980133002483900ccc009203233002482c008cc009203c0013300230473504b12233002482b008cc009203833002482900ccc00920ca0333002483200ccc00920103300248028cc00920463300248170cc009201433002482b80ccc00920e40233002483a004cc00920f80333002482d804cc00920f602330024824804cc00920de0333002480b0cc00920f001330024820808cc00920c80233002482c004cc009203e33002482b804cc009209a02330024827804cc00920c601001001320013550592225335002133034001480108854cd4cc0600180084cc014004cdc0001810899802800801a803198011982aa4913496e76616c696420446174756d204572726f72005335303c323500122222222222200c5006213332001503c001333051303b500c303a500c3039500c1330554901134e6f20496e70757420446174756d20486173680005633002335504a305549115496e76616c6964205374617274657220546f6b656e0032323335530151200135035503323500122333553018120013503850362350012233350012330394800000488cc0e80080048cc0e400520000013355301312001235001223355044002333500123355301712001235001223355048002355019001001223335550140440020012335530171200123500122335504800235501800100133355500f03f002001323233504b335504200233504b33550420014800941314130c114d4124488cc009209c01330024822804cc00920ae01330024826804cc00920be0100130443504812233002483e80ccc0092074330024834804cc00920820233002483f804cc00920ca0333002483c808cc00920fe0333002481c8cc00920f6033300248178cc00920d8023300248158cc00920c40333002482380ccc00920e4033300248000cc00920ee01330024823004cc00920d0033300248138cc00920dc033300248188cc009208c02330024827808cc00920cc03330024833804cc009209a03001335504a23500122001335504a502e330175042500600123355048305349113496e636f727265637420506f6c696379204964003303b00135005223333500123263204f3357389201024c680004f200123263204f3357389201024c680004f23263204f3357389201024c680004f253355335330483322333355002323350342233350170030010023501400133503322230033002001200122337000029001000a4000602024002a00490000a8270a999a99aa82391a800910011a815a8010a82710a99a999000a8280009099aa824a828999000a8258008a82790a827909999000a81c800999827181c28049919b800010033037500930365009133052491154e6f20446174756d2041742056616c696461746f7200053133012503d5001135001220023333573466e1cd55cea80124000466442466002006004646464646464646464646464646666ae68cdc39aab9d500c480008cccccccccccc88888888888848cccccccccccc00403403002c02802402001c01801401000c008cd40a00a4d5d0a80619a8140149aba1500b33502802a35742a014666aa058eb940acd5d0a804999aa8163ae502b35742a01066a0500626ae85401cccd540b00c9d69aba150063232323333573466e1cd55cea801240004664424660020060046464646666ae68cdc39aab9d5002480008cc8848cc00400c008cd40f1d69aba15002303d357426ae8940088c98c8178cd5ce02f82f02e09aab9e5001137540026ae854008c8c8c8cccd5cd19b8735573aa004900011991091980080180119a81e3ad35742a004607a6ae84d5d1280111931902f19ab9c05f05e05c135573ca00226ea8004d5d09aba2500223263205a3357380b60b40b026aae7940044dd50009aba1500533502875c6ae854010ccd540b00b88004d5d0a801999aa8163ae200135742a00460606ae84d5d1280111931902b19ab9c057056054135744a00226ae8940044d5d1280089aba25001135744a00226ae8940044d5d1280089aba25001135744a00226ae8940044d55cf280089baa00135742a00460406ae84d5d1280111931902419ab9c049048046104713502849010350543500135573ca00226ea80044cc80054104004444888ccd54c0384800540f8cd54c01c480048d400488cd540e0008d54024004ccd54c0384800488d4008894cd4ccd54c03448004c8cd40bc88ccd400c88008008004d40048800448cc0040241448d400488cc028008014018400c4cd410801000d40fc004cd54c01c480048d400488c8cd540e400cc004014c8004d54144894cd40044d5402800c884d4008894cd4cc03000802044888cc0080280104c01800c008894cd4008412c40044484888c00c0104484888c004010c8004d5411c8844894cd4004540f0884cd40f4c010008cd54c0184800401000488d40088888888888894cd4ccd54c03c48004cd40c0894cd40088400c4005413494cd4ccd5cd19b8f00e00105405313504f0011504e00421054105232001355045221122253350011350060032213335009005300400233355300712001005004001123500122001123500122002235001222222222222008223500122222222222233355300f12001223500222223500422335002200825335333573466e3c00405c15c1584cd4120cd540fc01401802040214100028c8004d5410088448894cd40044008884cc014008ccd54c01c4800401401000488cdc5001299a999ab9a3370e00201008007e200c266006a66a666ae68cdc380280402001f8a8190999803800802a8192441003200135503e222533500210012213300500133714006a66a666ae68cdc380100502102088040a99a999ab9a3370e00400c084082291101310015335333573466e1c0092004042041148901320015335333573466e1c0092006042041148901330015335333573466e1c0092008042041148901340015335333573466e1c009200a042041148901350015335333573466e1c009200c042041148901360015335333573466e1c009200e042041148901370015335333573466e1c0092010042041148901380015335333573466e1c0092012042041148901390015335333573466e1c00801c1081045220101610015335333573466e1c0092016042041148901620015335333573466e1c0092018042041148901630015335333573466e1c009201a042041148901640015335333573466e1c009201c042041148901650015335333573466e1c009201e042041148901660010314800920144890130003200135503a22225335333573466e1c00c0140f40f040044ccc010cdc180180100119a81719b860030020014800048c88c008dd6000990009aa81c911999aab9f0012502d233502c30043574200460066ae880080c48c8c8cccd5cd19b8735573aa004900011991091980080180118051aba150023005357426ae8940088c98c80c4cd5ce01901881789aab9e5001137540024646464646666ae68cdc39aab9d5004480008cccc888848cccc00401401000c008c8c8c8cccd5cd19b8735573aa004900011981418099aba1500233500d012357426ae8940088c98c80d8cd5ce01b81b01a09aab9e5001137540026ae854010ccd54021d728039aba150033232323333573466e1d4005200423212223002004357426aae79400c8cccd5cd19b875002480088c84888c004010dd71aba135573ca00846666ae68cdc3a801a400042444006464c6407066ae700e40e00d80d40d04d55cea80089baa00135742a00466a012eb8d5d09aba2500223263203233573806606406026ae8940044d5d1280089aab9e500113754002266aa002eb9d6889119118011bab00132001355036223233335573e0044a056466a05466aa042600c6aae754008c014d55cf280118021aba200302f13574200224464646666ae68cdc3a800a400046a064600a6ae84d55cf280191999ab9a3370ea00490011281911931901799ab9c03002f02d02c135573aa00226ea80048c8c8cccd5cd19b875001480188c848888c010014c01cd5d09aab9e500323333573466e1d400920042321222230020053009357426aae7940108cccd5cd19b875003480088c848888c004014c01cd5d09aab9e500523333573466e1d40112000232122223003005375c6ae84d55cf280311931901799ab9c03002f02d02c02b02a135573aa00226ea80048c8c8cccd5cd19b8735573aa004900011991091980080180118029aba15002375a6ae84d5d1280111931901599ab9c02c02b029135573ca00226ea80048c8cccd5cd19b8735573aa002900011bae357426aae7940088c98c80a4cd5ce01501481389baa001232323232323333573466e1d4005200c21222222200323333573466e1d4009200a21222222200423333573466e1d400d2008233221222222233001009008375c6ae854014dd69aba135744a00a46666ae68cdc3a8022400c4664424444444660040120106eb8d5d0a8039bae357426ae89401c8cccd5cd19b875005480108cc8848888888cc018024020c030d5d0a8049bae357426ae8940248cccd5cd19b875006480088c848888888c01c020c034d5d09aab9e500b23333573466e1d401d2000232122222223005008300e357426aae7940308c98c80c8cd5ce01981901801781701681601581509aab9d5004135573ca00626aae7940084d55cf280089baa0012323232323333573466e1d400520022333222122333001005004003375a6ae854010dd69aba15003375a6ae84d5d1280191999ab9a3370ea0049000119091180100198041aba135573ca00c464c6405666ae700b00ac0a40a04d55cea80189aba25001135573ca00226ea80048c8c8cccd5cd19b875001480088c068dd71aba135573ca00646666ae68cdc3a8012400046424460040066eb8d5d09aab9e500423263202833573805205004c04a26aae7540044dd500089119191999ab9a3370ea00290021091100091999ab9a3370ea00490011190911180180218031aba135573ca00846666ae68cdc3a801a400042444004464c6405266ae700a80a409c0980944d55cea80089baa0012323333573466e1d40052002202d23333573466e1d40092000202d23263202533573804c04a04604426aae74dd500089299a80089a8012490350543800221002123263202033573800204044666ae68cdc40010008140148891980080101391299a801080088130919a80111199a801910010010009a800910008910919800801801119801280a800990009aa811111299a80088011109a8011119803999804001003000801990009aa8109111299a80088011109a80111299a999ab9a3370e00290000140138999804003803001899980400399a80c8919980080400180100300191a80091100091a80091100111a800911001891299a999ab9a3371e6a0044440066a00244400603c03a2a66a666ae68cdc39a8011110011a80091100100f00e8999ab9a3371e6a0044440026a00244400203c03a203a203a640026aa03844a66a0022a02e442a66a64646a004446a006446466a00a466a0084a66a666ae68cdc78010008140138a80188139013919a80210139299a999ab9a3371e00400205004e2a006204e2a66a00642a66a0044266a004466a004466a004466a0044660220040024054466a004405446602200400244405444466a0084054444a66a666ae68cdc38030018168160a99a999ab9a3370e00a00405a05826604200800220582058204a2a66a0024204a204a64660146016002a036a016646a0024444008600c004266aa0244a666a6a00244440042a03442a66a664002a0380024266aa02aa03a664002a02e0022a03642a036600c0042600800244666ae68cdc780100080e00d91a800910008891091980080180111091980080180110911800801899aa804100098009a802891198012410c026600490470099801241b00466004902b1980124134066600490630119801241d8066600490510099801241800266004904201198012412c06660049067011980124158046600490610099801241c40266004904b00998012410c0466004906e0099801241a00266004906b0199801241900466004905d01198012416404660049042009980124174046600490730099801241b80666004904801800918019801000990009aa80a11299a8008a80391099a80419b8b00200630040013200135501322533500110032213371400460080029110012335001500250031122002122122330010040031122300200122333573466e1c00800403803448c8c8c8cccd5cd19b8735573aa006900011998049bae35742a0066eb4d5d0a8011bae357426ae8940088c98c801ccd5ce00400380289aba25001135573ca00226ea80052612001490103505431002221233300100400300212122300200311220012330034910f4d696e742f4275726e204572726f72000042330024910f4d696e742f4275726e204572726f72000032253350011004133573800400624400424400222464600200244660066004004003"
                                }
                            )
//                add(
//                    outputUtxo {
//                        address = "addr_test1vzqg9c47qdwj0nczg4tqdhcexz947upzfy8c5c0j9c2cz3sg8s4av"
//                        lovelace = "247953253" // this is the change value. should be calculated
//                    }
//                )
                        }

                        // this should be calculated automatically
                        // fee = 675857

                        // test manual signatures first, later test signing keys
                        signatures {
                            add(
                                signature {
                                    vkey =
                                        ByteString.fromHex("fc993172aa4ff02f2dec9985a6a359fb69a0ce80c85a06b3b195e54e9d0e18ae")
                                    sig =
                                        ByteString.fromHex(
                                            "6bae0b5a169ce71481fa2c37f70cfee13ffa0f78c03b33fe05d209ea79ebad99831edde9e00242ad7ab31058da70cf1cab4163a67c3ad3346d98c1033c959d0b"
                                        )
                                }
                            )
                        }
                    }

                println("cborHex: ${cborByteArray.toHexString()}")

                assertThat(cborByteArray.toHexString()).isEqualTo(
                    "84a30081825820949d496d900345508ed6e0b27b2a79a284fd3c37b7a3b7b01cece027fe1577aa000183a300581d6002065ef5eef5f05ca57e8f38e78ab0742219f34790777638788e738d011a0193dd7e03d818591730820259172b59172801000032332232323232323232323232332232323233223232323232323322323232323232323232323232323232323232323232323232323232323232323232323232323232323232323232323232323223232322322323253353232323232323232533500713355059305c4901124c6f636b696e673a4275726e204572726f72003335503f30501200123332001505900105f3503012233002335505b305e491105369676e696e67205478204572726f7200323232330020010393503412233002303235036122330024837804cc00920fe03330024836804cc009206633002482a808cc009209e02330024832804cc00920043300248168cc00920cc03330024825004cc00920d60233002482980ccc009208203330024835004cc00920f40133002483f004cc009200e33002483d008cc00920ae03330024834808cc009209401330024822804cc009208a0233002482700ccc009203a330024836008cc00920ec01001330023032350361223300248098cc00920c20333002483d804cc00920c001330024839004cc0092000330024820808cc009208001330024832808cc00920b4023300248110cc0092080013300248158cc00920aa0333002483780ccc00920880133002483d008cc00920ee0333002482e004cc009208e0333002483b00ccc00920a20233002481d8cc00920980133002483900ccc009203233002482c008cc009203c0013300230323503612233002482b008cc009203833002482900ccc00920ca0333002483200ccc00920103300248028cc00920463300248170cc009201433002482b80ccc00920e40233002483a004cc00920f80333002482d804cc00920f602330024824804cc00920de0333002480b0cc00920f001330024820808cc00920c80233002482c004cc009203e33002482b804cc009209a02330024827804cc00920c60100100132001355062222533500213303e001480108854cd4cc0680180084cc014004cdc000181d899802800801a8039980119aa82d982f24811353696e676c6520496e2f4f7574204572726f720033057330343018500748008cc0cd40192002330023305e4901114e4654204275726e696e67204572726f72005335303b301750071305c498884d40088894cd40104cc170cc16800cc11403ccc13c00520012213063498cc008cd5416cc17924113496e76616c696420446174756d204572726f72003004500633002335505b305e490115496e76616c6964205374617274657220546f6b656e003350395005502d00113355059305c491124c6f636b696e673a4d696e74204572726f72003335503f30501200123332001505900105f3503012233002335505b305e491105369676e696e67205478204572726f7200330145007302e350321223300248170cc00920f002330024834004cc00920da01330024824008cc00920543300248070cc00920ba0333002482480ccc009208e0333002483b00ccc009209c0233002482700ccc009209e0233002480c8cc009209e0233002482b808cc009207833002483480ccc009200833002483b804cc00920ea0233002480e0cc00920c00233002480b8cc00920e403330024831008cc00920ba0100133002335505b305e49011353696e676c6520496e2f4f7574204572726f720033057330343018500748008cc0cd40192002330023305e4901114e4654204d696e74696e67204572726f72005335303b301750071305d498884d40088894cd40104cc170cc16800cc11403ccc170cc168008c8c8cdc5001299a999ab9a3370e00207a0ce0cc2046266042a66a666ae68cdc381101e8338330a82d8999812000811282da441003045010304300f3304f00148008884c1912633002335505b305e490113496e76616c696420446174756d204572726f72003003500633002335505b305e490113496e76616c696420537461727420546f6b656e003350395005502d0013200135505e22533500113305d491194e6f20496e6372656173696e6720446174756d20466f756e640005e22153353332001504a301600250061533353017002130040012153353320015044001213305a33058304300d30430013305a3304d32337000029001182100698210009982c182080698208008980280110980280109802000990009aa82e91299a80089982e2481174e6f20436f6e7374616e7420446174756d20466f756e640005d221533533320015049301500250051533353016002130040012153353320015043001215335333573466e3cd403088800cd400488800c18818454cd4ccd5cd19b873500c222002350012220020620611333573466e3cd4030888004d4004888004188184418441844c01400884c0140084c01000454cd4c0c800c84cd5415c044d4004880044d40512401154e6f20496e70757420746f2056616c69646174652e0015335303100221350012235001222235009223500222222222222233355305c120012235002222253353501822350062232335005233500425335333573466e3c0080041f01ec5400c41ec81ec8cd401081ec94cd4ccd5cd19b8f00200107c07b15003107b1533500321533500221335002233500223350022335002233074002001207e2335002207e23307400200122207e222335004207e2225335333573466e1c01800c204042000454cd4ccd5cd19b87005002081010800113306b00400110800110800110791533500121079107913350680060051005506300a13263203e335738921024c6600042135001220023333573466e1cd55cea80224000466442466002006004646464646464646464646464646666ae68cdc39aab9d500c480008cccccccccccc88888888888848cccccccccccc00403403002c02802402001c01801401000c008cd4094098d5d0a80619a8128131aba1500b33502502735742a014666aa052eb940a0d5d0a804999aa814bae502835742a01066a04a05c6ae85401cccd540a40bdd69aba150063232323333573466e1cd55cea801240004664424660020060046464646666ae68cdc39aab9d5002480008cc8848cc00400c008cd40e5d69aba15002303a357426ae8940088c98c814ccd5ce01d82b82889aab9e5001137540026ae854008c8c8c8cccd5cd19b8735573aa004900011991091980080180119a81cbad35742a00460746ae84d5d1280111931902999ab9c03b057051135573ca00226ea8004d5d09aba2500223263204f33573806e0a609a26aae7940044dd50009aba1500533502575c6ae854010ccd540a40ac8004d5d0a801999aa814bae200135742a004605a6ae84d5d1280111931902599ab9c03304f049135744a00226ae8940044d5d1280089aba25001135744a00226ae8940044d5d1280089aba25001135744a00226ae8940044d55cf280089baa00135742a008603a6ae84d5d1280211931901e99ab9c02504103b3333573466e1d40152002212200223333573466e1d40192000212200123263203d33573804a0820760746666ae68cdc39aab9d5008480008ccc0ecdd71aba15008375a6ae85401cdd71aba135744a00e464c6407666ae7008c0fc0e440f84d403d24010350543500135573ca00226ea80044d55ce9baa001135744a00226aae7940044dd5000911a801111111111111299a999aa982609000a8191299a999ab9a3371e01c0020b60b426a0840022a082008420b620b246a002444400646a002444400446a00244444444444401046a0024444444444440182464c6405866ae700040c0c8004d5411c8894cd40084004884cc014004cdc5001a99a999ab9a3370e004042096094200e2a66a666ae68cdc38010100258250a44101310015335333573466e1c009200404b04a148901320015335333573466e1c009200604b04a148901330015335333573466e1c009200804b04a148901340015335333573466e1c009200a04b04a148901350015335333573466e1c009200c04b04a148901360015335333573466e1c009200e04b04a148901370015335333573466e1c009201004b04a148901380015335333573466e1c009201204b04a148901390015335333573466e1c00801812c1285220101610015335333573466e1c009201604b04a148901620015335333573466e1c009201804b04a148901630015335333573466e1c009201a04b04a148901640015335333573466e1c009201c04b04a148901650015335333573466e1c009201e04b04a1489016600101c480512210130003200135504422225335333573466e1c00c07411c11840044ccc010cdc180180100119a81d19b860030020011232230023758002640026aa088446666aae7c004940e88cd40e4c010d5d080118019aba200202d232323333573466e1cd55cea8012400046644246600200600460146ae854008c014d5d09aba2500223263202933573802205a04e26aae7940044dd50009191919191999ab9a3370e6aae75401120002333322221233330010050040030023232323333573466e1cd55cea8012400046644246600200600460266ae854008cd4034048d5d09aba2500223263202e33573802c06405826aae7940044dd50009aba150043335500875ca00e6ae85400cc8c8c8cccd5cd19b875001480108c84888c008010d5d09aab9e500323333573466e1d4009200223212223001004375c6ae84d55cf280211999ab9a3370ea00690001091100191931901819ab9c01803402e02d02c135573aa00226ea8004d5d0a80119a804bae357426ae8940088c98c80a8cd5ce00901701409aba25001135744a00226aae7940044dd5000899aa800bae75a224464460046eac004c8004d5410488c8cccd55cf8011281c119a81b99aa81a18031aab9d5002300535573ca00460086ae8800c0ac4d5d080089119191999ab9a3370ea002900011a81498029aba135573ca00646666ae68cdc3a801240044a052464c6404e66ae7003c0ac0940904d55cea80089baa001232323333573466e1d400520062321222230040053007357426aae79400c8cccd5cd19b875002480108c848888c008014c024d5d09aab9e500423333573466e1d400d20022321222230010053007357426aae7940148cccd5cd19b875004480008c848888c00c014dd71aba135573ca00c464c6404e66ae7003c0ac09409008c0884d55cea80089baa001232323333573466e1cd55cea80124000466442466002006004600a6ae854008dd69aba135744a004464c6404666ae7002c09c0844d55cf280089baa0012323333573466e1cd55cea800a400046eb8d5d09aab9e500223263202133573801204a03e26ea80048c8c8c8c8c8cccd5cd19b8750014803084888888800c8cccd5cd19b875002480288488888880108cccd5cd19b875003480208cc8848888888cc004024020dd71aba15005375a6ae84d5d1280291999ab9a3370ea00890031199109111111198010048041bae35742a00e6eb8d5d09aba2500723333573466e1d40152004233221222222233006009008300c35742a0126eb8d5d09aba2500923333573466e1d40192002232122222223007008300d357426aae79402c8cccd5cd19b875007480008c848888888c014020c038d5d09aab9e500c23263202a33573802405c05004e04c04a04804604426aae7540104d55cf280189aab9e5002135573ca00226ea80048c8c8c8c8cccd5cd19b875001480088ccc888488ccc00401401000cdd69aba15004375a6ae85400cdd69aba135744a00646666ae68cdc3a80124000464244600400660106ae84d55cf280311931901199ab9c00b027021020135573aa00626ae8940044d55cf280089baa001232323333573466e1d400520022321223001003375c6ae84d55cf280191999ab9a3370ea004900011909118010019bae357426aae7940108c98c8080cd5ce00401200f00e89aab9d50011375400224464646666ae68cdc3a800a40084244400246666ae68cdc3a8012400446424446006008600c6ae84d55cf280211999ab9a3370ea00690001091100111931901099ab9c00902501f01e01d135573aa00226ea80048c8cccd5cd19b8750014800880e08cccd5cd19b8750024800080e08c98c8074cd5ce00281080d80d09aab9d37540029201035054310013232335028335502500233502833550250014800940a540a4c008d4018488cc009209c01330024822804cc00920ae01330024826804cc00920be0100130013500512233002483e80ccc0092074330024834804cc00920820233002483f804cc00920ca0333002483c808cc00920fe0333002481c8cc00920f6033300248178cc00920d8023300248158cc00920c40333002482380ccc00920e4033300248000cc00920ee01330024823004cc00920d0033300248138cc00920dc033300248188cc009208c02330024827808cc00920cc03330024833804cc009209a0300123003300200132001355032225335001150272213350283371600400c6008002640026aa06244a66a002200644266e28008c01000522100123350015022502322323300100300632001355030222533500213301c00100422135002222253335002133009005007213300a0063370001001c426601400c66e0002003888c8cc00400c014c8004d540bc8894cd40084cc06c004010884d400888d400488894ccd40084cc02c01c02484cc030020cdc0005007909980600419b8000a00f48009200023500122350022222222222223333500d2501f2501f2501f233355302b1200150112350012253355335333573466e3cd400888008d4010880080f00ec4ccd5cd19b873500222001350042200103c03b103b1350230031502200d1335021225335002210031001500e1301200122333573466e2000800409c0a08cc0094068004c8004d540948894cd40044008884d400888cc01cccc02000801800400cc8004d5409088894cd40044008884d4008894cd4ccd5cd19b87001480000ac0a84ccc02001c01800c4ccc02001ccd407848ccc00402000c00801800c8d40048880048d40048880088d400488800c4488cd54008d4065405c00448c8c8c8c8ccccccd5d200291999ab9a3370e6aae7540152000233335573ea00a4a01846666aae7d4014940348cccd55cfa8029280711999aab9f35744a00c4a66aa66aa66a601c6ae85402484d4044c0380045403c854cd4c8ccccccd5d200092809128091280911a8099bad0022501201335742a012426a02460040022a0202a01e42a66a601e6ae85402084d4048c008004540405403c9403c04003c0380349402c01c9402894028940289402802c4d5d1280089aba25001135573ca00226ea800526222123330010040030022333333357480024a0064a0064a0064a00646a0086eb800801048488c00800c4488004480044c00800488ccd5cd19b8700200101801722233355300a1200135010500e2350012233355300d1200135013501123500122333500123300a4800000488cc02c0080048cc0280052000001335530091200123500122335500b002333500123355300d1200123500122335500f00235500e0010012233355500901200200123355300d1200123500122335500f00235500d00100133355500400d00200111122233355300412001500a335530081200123500122335500a00235500900133355300412001223500222533533355300d12001323350152233350032200200200135001220011233001225335002101e100101b235001223300a0020050061003133500e004003500b00133553008120012350012232335500b00330010053200135501b225335001135500a003221350022253353300c002008112223300200a0041300600300232001355014221122253350011002221330050023335530071200100500400111212223003004112122230010041122123300100300232001355010221122533500115007221335008300400233553006120010040013200135500f221122253350011350032200122133350052200230040023335530071200100500400111220021221223300100400322333573466e3c008004034030448cc00400802c894cd40084004402848cd400888ccd400c88008008004d40048800448848cc00400c0084894cd4008400454cd4004401c40204488c0080048cc00d240110426164204275726e696e6720496e666f0000423300249110426164204d696e74696e6720496e666f000032253350011004133573800400624400424400222464600200244660066004004003a300581d6002065ef5eef5f05ca57e8f38e78ab0742219f34790777638788e738d011a017bfe0c03d8185915c582025915c05915bd010000323322323232332232323232323232332232323232323232323232323232323232323232323232323232323232323232323232323232323232323232323232323232323232323232332232322322323253353232323232335504830534910d4d696e74696e67204572726f72003335530111200135031502f25335001105615335056105510563504512233002335504a3055491154d696e74696e672f4275726e696e67204572726f72003300e33032533530353016500613053498884d40088894cd40104cc0dcc02000ccc0dccd5413cc168cd5413dcc99b8a48915526571756972656420546f6b656e204e616d653a20003301e303d5010303e5010330420023301e303d5010303e5010335504f305a49115496e636f7272656374204d696e7420416d6f756e74003305000148008884c16926300448008cc0c94cd4c0d4c05940184c1512622135002222533500413303730080033305000148004884c16d26300448000cc008cd54128c155241105369676e696e67205478204572726f72003300e3301250063043350471223300248170cc00920f002330024834004cc00920da01330024824008cc00920543300248070cc00920ba0333002482480ccc009208e0333002483b00ccc009209c0233002482700ccc009209e0233002480c8cc009209e0233002482b808cc009207833002483480ccc009200833002483b804cc00920ea0233002480e0cc00920c00233002480b8cc00920e403330024831008cc00920ba0100132323233002001022350491223300230473504b122330024837804cc00920fe03330024836804cc009206633002482a808cc009209e02330024832804cc00920043300248168cc00920cc03330024825004cc00920d60233002482980ccc009208203330024835004cc00920f40133002483f004cc009200e33002483d008cc00920ae03330024834808cc009209401330024822804cc009208a0233002482700ccc009203a330024836008cc00920ec010013300230473504b1223300248098cc00920c20333002483d804cc00920c001330024839004cc0092000330024820808cc009208001330024832808cc00920b4023300248110cc0092080013300248158cc00920aa0333002483780ccc00920880133002483d008cc00920ee0333002482e004cc009208e0333002483b00ccc00920a20233002481d8cc00920980133002483900ccc009203233002482c008cc009203c0013300230473504b12233002482b008cc009203833002482900ccc00920ca0333002483200ccc00920103300248028cc00920463300248170cc009201433002482b80ccc00920e40233002483a004cc00920f80333002482d804cc00920f602330024824804cc00920de0333002480b0cc00920f001330024820808cc00920c80233002482c004cc009203e33002482b804cc009209a02330024827804cc00920c601001001320013550592225335002133034001480108854cd4cc0600180084cc014004cdc0001810899802800801a803198011982aa4913496e76616c696420446174756d204572726f72005335303c323500122222222222200c5006213332001503c001333051303b500c303a500c3039500c1330554901134e6f20496e70757420446174756d20486173680005633002335504a305549115496e76616c6964205374617274657220546f6b656e0032323335530151200135035503323500122333553018120013503850362350012233350012330394800000488cc0e80080048cc0e400520000013355301312001235001223355044002333500123355301712001235001223355048002355019001001223335550140440020012335530171200123500122335504800235501800100133355500f03f002001323233504b335504200233504b33550420014800941314130c114d4124488cc009209c01330024822804cc00920ae01330024826804cc00920be0100130443504812233002483e80ccc0092074330024834804cc00920820233002483f804cc00920ca0333002483c808cc00920fe0333002481c8cc00920f6033300248178cc00920d8023300248158cc00920c40333002482380ccc00920e4033300248000cc00920ee01330024823004cc00920d0033300248138cc00920dc033300248188cc009208c02330024827808cc00920cc03330024833804cc009209a03001335504a23500122001335504a502e330175042500600123355048305349113496e636f727265637420506f6c696379204964003303b00135005223333500123263204f3357389201024c680004f200123263204f3357389201024c680004f23263204f3357389201024c680004f253355335330483322333355002323350342233350170030010023501400133503322230033002001200122337000029001000a4000602024002a00490000a8270a999a99aa82391a800910011a815a8010a82710a99a999000a8280009099aa824a828999000a8258008a82790a827909999000a81c800999827181c28049919b800010033037500930365009133052491154e6f20446174756d2041742056616c696461746f7200053133012503d5001135001220023333573466e1cd55cea80124000466442466002006004646464646464646464646464646666ae68cdc39aab9d500c480008cccccccccccc88888888888848cccccccccccc00403403002c02802402001c01801401000c008cd40a00a4d5d0a80619a8140149aba1500b33502802a35742a014666aa058eb940acd5d0a804999aa8163ae502b35742a01066a0500626ae85401cccd540b00c9d69aba150063232323333573466e1cd55cea801240004664424660020060046464646666ae68cdc39aab9d5002480008cc8848cc00400c008cd40f1d69aba15002303d357426ae8940088c98c8178cd5ce02f82f02e09aab9e5001137540026ae854008c8c8c8cccd5cd19b8735573aa004900011991091980080180119a81e3ad35742a004607a6ae84d5d1280111931902f19ab9c05f05e05c135573ca00226ea8004d5d09aba2500223263205a3357380b60b40b026aae7940044dd50009aba1500533502875c6ae854010ccd540b00b88004d5d0a801999aa8163ae200135742a00460606ae84d5d1280111931902b19ab9c057056054135744a00226ae8940044d5d1280089aba25001135744a00226ae8940044d5d1280089aba25001135744a00226ae8940044d55cf280089baa00135742a00460406ae84d5d1280111931902419ab9c049048046104713502849010350543500135573ca00226ea80044cc80054104004444888ccd54c0384800540f8cd54c01c480048d400488cd540e0008d54024004ccd54c0384800488d4008894cd4ccd54c03448004c8cd40bc88ccd400c88008008004d40048800448cc0040241448d400488cc028008014018400c4cd410801000d40fc004cd54c01c480048d400488c8cd540e400cc004014c8004d54144894cd40044d5402800c884d4008894cd4cc03000802044888cc0080280104c01800c008894cd4008412c40044484888c00c0104484888c004010c8004d5411c8844894cd4004540f0884cd40f4c010008cd54c0184800401000488d40088888888888894cd4ccd54c03c48004cd40c0894cd40088400c4005413494cd4ccd5cd19b8f00e00105405313504f0011504e00421054105232001355045221122253350011350060032213335009005300400233355300712001005004001123500122001123500122002235001222222222222008223500122222222222233355300f12001223500222223500422335002200825335333573466e3c00405c15c1584cd4120cd540fc01401802040214100028c8004d5410088448894cd40044008884cc014008ccd54c01c4800401401000488cdc5001299a999ab9a3370e00201008007e200c266006a66a666ae68cdc380280402001f8a8190999803800802a8192441003200135503e222533500210012213300500133714006a66a666ae68cdc380100502102088040a99a999ab9a3370e00400c084082291101310015335333573466e1c0092004042041148901320015335333573466e1c0092006042041148901330015335333573466e1c0092008042041148901340015335333573466e1c009200a042041148901350015335333573466e1c009200c042041148901360015335333573466e1c009200e042041148901370015335333573466e1c0092010042041148901380015335333573466e1c0092012042041148901390015335333573466e1c00801c1081045220101610015335333573466e1c0092016042041148901620015335333573466e1c0092018042041148901630015335333573466e1c009201a042041148901640015335333573466e1c009201c042041148901650015335333573466e1c009201e042041148901660010314800920144890130003200135503a22225335333573466e1c00c0140f40f040044ccc010cdc180180100119a81719b860030020014800048c88c008dd6000990009aa81c911999aab9f0012502d233502c30043574200460066ae880080c48c8c8cccd5cd19b8735573aa004900011991091980080180118051aba150023005357426ae8940088c98c80c4cd5ce01901881789aab9e5001137540024646464646666ae68cdc39aab9d5004480008cccc888848cccc00401401000c008c8c8c8cccd5cd19b8735573aa004900011981418099aba1500233500d012357426ae8940088c98c80d8cd5ce01b81b01a09aab9e5001137540026ae854010ccd54021d728039aba150033232323333573466e1d4005200423212223002004357426aae79400c8cccd5cd19b875002480088c84888c004010dd71aba135573ca00846666ae68cdc3a801a400042444006464c6407066ae700e40e00d80d40d04d55cea80089baa00135742a00466a012eb8d5d09aba2500223263203233573806606406026ae8940044d5d1280089aab9e500113754002266aa002eb9d6889119118011bab00132001355036223233335573e0044a056466a05466aa042600c6aae754008c014d55cf280118021aba200302f13574200224464646666ae68cdc3a800a400046a064600a6ae84d55cf280191999ab9a3370ea00490011281911931901799ab9c03002f02d02c135573aa00226ea80048c8c8cccd5cd19b875001480188c848888c010014c01cd5d09aab9e500323333573466e1d400920042321222230020053009357426aae7940108cccd5cd19b875003480088c848888c004014c01cd5d09aab9e500523333573466e1d40112000232122223003005375c6ae84d55cf280311931901799ab9c03002f02d02c02b02a135573aa00226ea80048c8c8cccd5cd19b8735573aa004900011991091980080180118029aba15002375a6ae84d5d1280111931901599ab9c02c02b029135573ca00226ea80048c8cccd5cd19b8735573aa002900011bae357426aae7940088c98c80a4cd5ce01501481389baa001232323232323333573466e1d4005200c21222222200323333573466e1d4009200a21222222200423333573466e1d400d2008233221222222233001009008375c6ae854014dd69aba135744a00a46666ae68cdc3a8022400c4664424444444660040120106eb8d5d0a8039bae357426ae89401c8cccd5cd19b875005480108cc8848888888cc018024020c030d5d0a8049bae357426ae8940248cccd5cd19b875006480088c848888888c01c020c034d5d09aab9e500b23333573466e1d401d2000232122222223005008300e357426aae7940308c98c80c8cd5ce01981901801781701681601581509aab9d5004135573ca00626aae7940084d55cf280089baa0012323232323333573466e1d400520022333222122333001005004003375a6ae854010dd69aba15003375a6ae84d5d1280191999ab9a3370ea0049000119091180100198041aba135573ca00c464c6405666ae700b00ac0a40a04d55cea80189aba25001135573ca00226ea80048c8c8cccd5cd19b875001480088c068dd71aba135573ca00646666ae68cdc3a8012400046424460040066eb8d5d09aab9e500423263202833573805205004c04a26aae7540044dd500089119191999ab9a3370ea00290021091100091999ab9a3370ea00490011190911180180218031aba135573ca00846666ae68cdc3a801a400042444004464c6405266ae700a80a409c0980944d55cea80089baa0012323333573466e1d40052002202d23333573466e1d40092000202d23263202533573804c04a04604426aae74dd500089299a80089a8012490350543800221002123263202033573800204044666ae68cdc40010008140148891980080101391299a801080088130919a80111199a801910010010009a800910008910919800801801119801280a800990009aa811111299a80088011109a8011119803999804001003000801990009aa8109111299a80088011109a80111299a999ab9a3370e00290000140138999804003803001899980400399a80c8919980080400180100300191a80091100091a80091100111a800911001891299a999ab9a3371e6a0044440066a00244400603c03a2a66a666ae68cdc39a8011110011a80091100100f00e8999ab9a3371e6a0044440026a00244400203c03a203a203a640026aa03844a66a0022a02e442a66a64646a004446a006446466a00a466a0084a66a666ae68cdc78010008140138a80188139013919a80210139299a999ab9a3371e00400205004e2a006204e2a66a00642a66a0044266a004466a004466a004466a0044660220040024054466a004405446602200400244405444466a0084054444a66a666ae68cdc38030018168160a99a999ab9a3370e00a00405a05826604200800220582058204a2a66a0024204a204a64660146016002a036a016646a0024444008600c004266aa0244a666a6a00244440042a03442a66a664002a0380024266aa02aa03a664002a02e0022a03642a036600c0042600800244666ae68cdc780100080e00d91a800910008891091980080180111091980080180110911800801899aa804100098009a802891198012410c026600490470099801241b00466004902b1980124134066600490630119801241d8066600490510099801241800266004904201198012412c06660049067011980124158046600490610099801241c40266004904b00998012410c0466004906e0099801241a00266004906b0199801241900466004905d01198012416404660049042009980124174046600490730099801241b80666004904801800918019801000990009aa80a11299a8008a80391099a80419b8b00200630040013200135501322533500110032213371400460080029110012335001500250031122002122122330010040031122300200122333573466e1c00800403803448c8c8c8cccd5cd19b8735573aa006900011998049bae35742a0066eb4d5d0a8011bae357426ae8940088c98c801ccd5ce00400380289aba25001135573ca00226ea80052612001490103505431002221233300100400300212122300200311220012330034910f4d696e742f4275726e204572726f72000042330024910f4d696e742f4275726e204572726f72000032253350011004133573800400624400424400222464600200244660066004004003a200581d60da0eb5ed7611482ec5089b69d870e0c56c1c45180256112398e0835b011a0ec77dc1021a000a49b5a10081825820fc993172aa4ff02f2dec9985a6a359fb69a0ce80c85a06b3b195e54e9d0e18ae58406bae0b5a169ce71481fa2c37f70cfee13ffa0f78c03b33fe05d209ea79ebad99831edde9e00242ad7ab31058da70cf1cab4163a67c3ad3346d98c1033c959d0bf5f6"
                )
            }
        }

    @Test
    fun `test auxDataHash`() =
        runBlocking {
            // hash from when we ran this through cardano-cli transaction builder
            val targetHash = "019b714a273c4d18f735cc3827f65fff853636aac3c9dfc03ef2f899270b072c".hexToByteArray()

            createTxSubmitClient(
                websocketHost = TEST_HOST,
                websocketPort = TEST_PORT,
                secure = TEST_SECURE,
            ).use { client ->
                val connectResult = client.connect()
                assertThat(connectResult).isTrue()
                assertThat(client.isConnected).isTrue()

                val response = (client as StateQueryClient).protocolParameters()
                val protocolParams = response.result

                val txBuilder = TransactionBuilder(protocolParams)
                txBuilder.transactionMetadata = createTestMetadata("NEWM_8")

                val auxData = txBuilder.createAuxData()
                val auxDataCbor = auxData!!.toCborByteArray()
                val auxDataHash = Blake2b.hash256(auxDataCbor)
                assertThat(auxDataHash).isEqualTo(targetHash)
            }
        }

    @Test
    fun `test non-inline datum`() =
        runBlocking {
            val datum = plutusData {
                cborHex = "d8799f00d87980ff"
                constr = 0
                list = plutusDataList {
                    listItem.add(
                        plutusData {
                            int = 0
                        }
                    )
                    listItem.add(
                        plutusData {
                            constr = 0
                            list = plutusDataList { }
                        }
                    )
                }
            }

            val datumHex = datum.toCborObject().toCborByteArray().toHexString()
            assertThat(datumHex).isEqualTo("d8799f00d87980ff")

            val datumWitnesses = CborArray.create(
                listOf(datum.toCborObject()),
                258,
            )

            // check to make sure we kept the indeterminate array value
            assertThat(datumWitnesses.toCborByteArray().toHexString()).isEqualTo("d9010281d8799f00d87980ff")

            // datum {
            //  cborHex: "d8799f00d87980ff"
            //  constr: 0
            //  list {
            //    list_item {
            //      int: 0
            //    }
            //    list_item {
            //      constr: 0
            //      list {
            //      }
            //    }
            //  }
            // }
        }

    @Test
    @Disabled("Disabled until fixing Unknown transaction input failure")
    fun `test Mint NFT`() =
        runBlocking {
            val targetCborHex =
                "84ab00828258209680eb2595fe0aa16b75805002972b063a998c413ae96a97162bc7b880bf42dc028258209680eb2595fe0aa16b75805002972b063a998c413ae96a97162bc7b880bf42dc030184a300581d7087692773cf94c4542105946daebe7afeb9100efb7a108158a2f9749c011a001430a2028201d8185865d87985581c602a7e54f9c569c770ba6f732af3077d8b06e14b84abc8d0151635d8581c3ab25c853f0f188f43b34b2df5cb98737a4de7e19028ec9d1a414a55464e45574d5f39581c9bb822cdf2c6c79657c0e8ec24308487dbd35750c084e271620d9d8040a200581d609bb822cdf2c6c79657c0e8ec24308487dbd35750c084e271620d9d8001821a000fc8a0a1581c3ab25c853f0f188f43b34b2df5cb98737a4de7e19028ec9d1a414a55a1464e45574d5f3901a300581d704347ac2bcda3f6516082cba79661714b836e68eba49d99429d73eec801821a00152d2ca1581cfd3a69817fe5b9ff39fb2fac2be2c7f2007746e827ee31868fe667cda1454e45574d5f01028201d8185828d87983581c3ab25c853f0f188f43b34b2df5cb98737a4de7e19028ec9d1a414a550a454e45574d5fa200581d608082e2be035d27cf02455606df19308b5f7022490f8a61f22e158146011a0a333436021a000d4d3a075820ba8bc7edf1ef9f45eafe89a050948171d1b54c05af40b3342f1790a99447b46009a1581c3ab25c853f0f188f43b34b2df5cb98737a4de7e19028ec9d1a414a55a1464e45574d5f39010b58203ade94b1f0bfaab5eeeaa8fe3bfb5603e96dca4689cf5bf40afd39f6496185770d81825820949d496d900345508ed6e0b27b2a79a284fd3c37b7a3b7b01cece027fe1577aa020e83581c2eb8686d882a0eddc9c7f68ece8f198f973ce90477b51ca017f2a25d581c4c1017ed05f703f6cf31c6a743f2c69534dbe4846e0d51ed4cb24b85581c8082e2be035d27cf02455606df19308b5f7022490f8a61f22e15814610a200581d608082e2be035d27cf02455606df19308b5f7022490f8a61f22e158146011a00385769111a0013f3d712828258200b4c56fdae7c23748c837e2443dfebebd020239c107aed9b227851ebabe4a829018258200b4c56fdae7c23748c837e2443dfebebd020239c107aed9b227851ebabe4a82902a20083825820d482feaa7d38f393d3a39b0b61d1bcab4213a09e8816ffa48931f26561b402605840b390d79f2c0050de41a4f3df8b2402fa6b1d79b695c0c48a4c6aa6539882452d7cb69e1ec0210556a93033ecc8765894233fdcede5d8b76b1b3495be4ddece0d8258203cc51d63f04e5c509c34f6d6c2d6b51aaed557a66c8069fc06f15b17cc57d7e55840f82e9edd63ce042e595893a46c7d7c18f3915ef1f36e2d34a2b15a39985aac536359bc78180d5df59f4babf986c9ef7bfa06310bedffbfee878867bf704bde0a825820de537d884b9b84c267309e9a07a4fd857da4c81565a5d261ef4041bd08c93ca25840f1222e48345f87a0b7aabc5e42d97645c5bf04fea7358067ef56ce85bee31bd3f89e2aa39e023b78f0f93e223c8deaba15a803f7d5ec7b87e8ab1c0f983b66080582840000d87980821a002c30541a2f4ed58d840100d87983581c3ab25c853f0f188f43b34b2df5cb98737a4de7e19028ec9d1a414a5509454e45574d5f821a004b96c51a4fd1be1ef5d90103a100a11902d1a278383630326137653534663963353639633737306261366637333261663330373764386230366531346238346162633864303135313633356438a1664e45574d5f39b81b6b616c62756d5f7469746c656a496e6469636c6f756473676172746973747381a1646e616d656543c39c534969636f7079726967687469e284972043c39c53496b6469737472696275746f726f68747470733a2f2f6e65776d2e696f686578706c6963697464747275656566696c657382a3696d65646961547970656a617564696f2f6d706567646e616d656a496e6469636c6f75647363737263783061723a2f2f724349374c484954396a5459655f6f78656e386c5a61454232552d42627775354d475159754b7246587277a3696d65646961547970656f6170706c69636174696f6e2f706466646e616d65782153747265616d696e6720526f79616c74792053686172652041677265656d656e7463737263783061723a2f2f5f4d3451787a47557764495a43636b774e624350626f4b4379625235795f55424e633541595457525478636667656e726573826748697020486f706352617065696d616765783061723a2f2f4d356f564a4f4f6f786c426c48437a6c7748594841684b7778726e64316a79726e73415749376a7736584164697372636f515a2d4e57372d32332d3436353033686c616e677561676565656e2d5553656c696e6b73a66866616365626f6f6b783768747470733a2f2f7777772e66616365626f6f6b2e636f6d2f70726f66696c652e7068703f69643d31303030383732323036333936393069696e7374616772616d782068747470733a2f2f696e7374616772616d2e636f6d2f437573692e776f726c64686c696e6b74726565781b68747470733a2f2f6c696e6b74722e65652f43757369576f726c646773706f7469667982783f68747470733a2f2f6f70656e2e73706f746966792e636f6d2f6172746973742f364c4a535743516966316f5a6a4963683261447330303f73693d3833644d757164532d52536d574c3646595130695f30676774776974746572781d68747470733a2f2f747769747465722e636f6d2f43757369576f726c6467796f7574756265783868747470733a2f2f7777772e796f75747562652e636f6d2f6368616e6e656c2f5543636d58364973385739373777544f42306f5a71726f67666c7972696373783061723a2f2f544e484e30763277547355684e70726757764548435f435a764769357867625f6f55384d41577671563059696d65646961547970656a696d6167652f77656270716d657461646174615f6c616e677561676565656e2d55536c6d69785f656e67696e6565726f4e617468616e20437573756d616e6f646d6f6f6469496e73706972696e67766d757369635f6d657461646174615f76657273696f6e01646e616d657243c39c5349202d20496e6469636c6f75647371706172656e74616c5f61647669736f7279684578706c696369746870726f64756365726c4d6368616c65204265617473727265636f7264696e675f656e67696e6565726f4e617468616e20437573756d616e6f6c72656c656173655f646174656a323032322d30332d31376c72656c656173655f747970656653696e676c65667365726965736b43c39c534920574f524c446d736f6e675f6475726174696f6e665054334d37536a736f6e675f7469746c656a496e6469636c6f7564736c747261636b5f6e756d626572016776657273696f6e01"

            createTxSubmitClient(
                websocketHost = TEST_HOST,
                websocketPort = TEST_PORT,
                secure = TEST_SECURE,
            ).use { client ->
                val connectResult = client.connect()
                assertThat(connectResult).isTrue()
                assertThat(client.isConnected).isTrue()

                val response = (client as StateQueryClient).protocolParameters()
                val protocolParams = response.result

                val calculateTxExecutionUnits: suspend (ByteArray) -> EvaluateTxResult = { cborBytes ->
                    val evaluateResponse = client.evaluate(cborBytes.toHexString())
                    println("evaluateResponse: ${evaluateResponse.result}")
                    evaluateResponse.result
                }

                val (_, cborByteArray) =
                    transactionBuilder(protocolParams, calculateTxExecutionUnits = calculateTxExecutionUnits) {
                        sourceUtxos {
                            add(
                                utxo {
                                    hash = "9680eb2595fe0aa16b75805002972b063a998c413ae96a97162bc7b880bf42dc"
                                    ix = 2
                                    lovelace = "1387820"
                                    nativeAssets.add(
                                        nativeAsset {
                                            policy = "fd3a69817fe5b9ff39fb2fac2be2c7f2007746e827ee31868fe667cd"
                                            name = "4e45574d5f"
                                            amount = "1"
                                        }
                                    )
                                }
                            )
                            add(
                                utxo {
                                    hash = "9680eb2595fe0aa16b75805002972b063a998c413ae96a97162bc7b880bf42dc"
                                    ix = 3
                                    lovelace = "174357170"
                                }
                            )
                        }
                        outputUtxos {
                            add(
                                outputUtxo {
                                    address = "addr_test1wzrkjfmne72vg4ppqk2xmt470tltjyqwldappq2c5tuhf8qflnumr"
                                    // lovelace = "1637800" // auto calculated
                                    datum =
                                        plutusData {
                                            // 121_0([_
                                            //    h'602a7e54f9c569c770ba6f732af3077d8b06e14b84abc8d0151635d8',
                                            //    h'3ab25c853f0f188f43b34b2df5cb98737a4de7e19028ec9d1a414a55',
                                            //    h'4e45574d5f39',
                                            //    h'9bb822cdf2c6c79657c0e8ec24308487dbd35750c084e271620d9d80',
                                            //    h'',
                                            // ])
                                            constr = 0
                                            list =
                                                plutusDataList {
                                                    with(listItem) {
                                                        add(
                                                            plutusData {
                                                                bytes =
                                                                    ByteString.fromHex(
                                                                        "602a7e54f9c569c770ba6f732af3077d8b06e14b84abc8d0151635d8"
                                                                    )
                                                            }
                                                        )
                                                        add(
                                                            plutusData {
                                                                bytes =
                                                                    ByteString.fromHex(
                                                                        "3ab25c853f0f188f43b34b2df5cb98737a4de7e19028ec9d1a414a55"
                                                                    )
                                                            }
                                                        )
                                                        add(
                                                            plutusData {
                                                                bytes = ByteString.fromHex("4e45574d5f39")
                                                            }
                                                        )
                                                        add(
                                                            plutusData {
                                                                bytes =
                                                                    ByteString.fromHex(
                                                                        "9bb822cdf2c6c79657c0e8ec24308487dbd35750c084e271620d9d80"
                                                                    )
                                                            }
                                                        )
                                                        add(
                                                            plutusData {
                                                                bytes = ByteString.EMPTY
                                                            }
                                                        )
                                                    }
                                                }
                                        }.toCborObject().toCborByteArray().toHexString()
                                }
                            )

                            add(
                                outputUtxo {
                                    address = "addr_test1vzdmsgkd7trv09jhcr5wcfpssjrah56h2rqgfcn3vgxemqqg4whyl"
                                    // lovelace = "1034400" // auto calculated
                                    nativeAssets.add(
                                        nativeAsset {
                                            policy = "3ab25c853f0f188f43b34b2df5cb98737a4de7e19028ec9d1a414a55"
                                            name = "4e45574d5f39"
                                            amount = "1"
                                        }
                                    )
                                }
                            )

                            add(
                                outputUtxo {
                                    address = "addr_test1wpp50tptek3lv5tqst9609npw99cxmngawjfmx2zn4e7ajqvqrt3u"
                                    lovelace =
                                        "1387820" // back to the contract. must match exactly what we started with
                                    nativeAssets.add(
                                        nativeAsset {
                                            policy = "fd3a69817fe5b9ff39fb2fac2be2c7f2007746e827ee31868fe667cd"
                                            name = "4e45574d5f"
                                            amount = "1"
                                        }
                                    )
                                    datum =
                                        plutusData {
                                            constr = 0
                                            list =
                                                plutusDataList {
                                                    // 121_0([_
                                                    //    h'3ab25c853f0f188f43b34b2df5cb98737a4de7e19028ec9d1a414a55',
                                                    //    10,
                                                    //    h'4e45574d5f',
                                                    // ])
                                                    with(listItem) {
                                                        add(
                                                            plutusData {
                                                                bytes =
                                                                    ByteString.fromHex(
                                                                        "3ab25c853f0f188f43b34b2df5cb98737a4de7e19028ec9d1a414a55"
                                                                    )
                                                            }
                                                        )
                                                        add(
                                                            plutusData {
                                                                int = 10L
                                                            }
                                                        )
                                                        add(
                                                            plutusData {
                                                                bytes = ByteString.fromHex("4e45574d5f")
                                                            }
                                                        )
                                                    }
                                                }
                                        }.toCborObject().toCborByteArray().toHexString()
                                }
                            )

                            //        1: [
                            //            {
                            //                0: h'7087692773cf94c4542105946daebe7afeb9100efb7a108158a2f9749c',
                            //                1: 1637800_2,
                            //                2: [
                            //                    1,
                            //                    24_0(h'd8799f581c602a7e54f9c569c770ba6f732af3077d8b06e14b84abc8d0151635d8581c3ab25c853f0f188f43b34b2df5cb98737a4de7e19028ec9d1a414a55464e45574d5f39581c9bb822cdf2c6c79657c0e8ec24308487dbd35750c084e271620d9d8040ff'),
                            //                ],
                            //            },
                            //            {
                            //                0: h'609bb822cdf2c6c79657c0e8ec24308487dbd35750c084e271620d9d80',
                            //                1: [
                            //                    1034400_2,
                            //                    {
                            //                        h'3ab25c853f0f188f43b34b2df5cb98737a4de7e19028ec9d1a414a55': {h'4e45574d5f39': 1},
                            //                    },
                            //                ],
                            //            },
                            //            {
                            //                0: h'704347ac2bcda3f6516082cba79661714b836e68eba49d99429d73eec8',
                            //                1: [
                            //                    1387820_2,
                            //                    {
                            //                        h'fd3a69817fe5b9ff39fb2fac2be2c7f2007746e827ee31868fe667cd': {h'4e45574d5f': 1},
                            //                    },
                            //                ],
                            //                2: [
                            //                    1,
                            //                    24_0(h'd8799f581c3ab25c853f0f188f43b34b2df5cb98737a4de7e19028ec9d1a414a550a454e45574d5fff'),
                            //                ],
                            //            },
                            //            {
                            //                0: h'608082e2be035d27cf02455606df19308b5f7022490f8a61f22e158146',
                            //                1: 170803904_2,
                            //            },
                            //        ],
                        }

                        //            {
                        //                0: h'608082e2be035d27cf02455606df19308b5f7022490f8a61f22e158146',
                        //                1: 170803904_2,
                        //            },
                        changeAddress = "addr_test1vzqg9c47qdwj0nczg4tqdhcexz947upzfy8c5c0j9c2cz3sg8s4av"

                        // fee = 881066L // calculated

                        // calculated
                        // auxDataHash = "7d0504e5a0b3f4cbae50a097cf4e259965765d75ffbc3a3a9f89df9fccdc01cb".hexToByteArray()

                        // 9: {
                        //   h'3ab25c853f0f188f43b34b2df5cb98737a4de7e19028ec9d1a414a55': {h'4e45574d5f39': 1},
                        // },
                        mintTokens {
                            add(
                                nativeAsset {
                                    policy = "3ab25c853f0f188f43b34b2df5cb98737a4de7e19028ec9d1a414a55"
                                    name = "4e45574d5f39"
                                    amount = "1"
                                }
                            )
                        }

                        // 11: h'ebd0c9b7d2924326c2250d92eb62c8d5cec9848e0bd8d43a3013a73becf528bb',
                        // calculated
                        // scriptDataHash = "ebd0c9b7d2924326c2250d92eb62c8d5cec9848e0bd8d43a3013a73becf528bb".hexToByteArray()

                        // 13: [
                        //  [
                        //     h'949d496d900345508ed6e0b27b2a79a284fd3c37b7a3b7b01cece027fe1577aa',
                        //     2,
                        //   ],
                        // ],
                        collateralUtxos {
                            add(
                                utxo {
                                    hash = "949d496d900345508ed6e0b27b2a79a284fd3c37b7a3b7b01cece027fe1577aa"
                                    ix = 2
                                    lovelace = "5000000"
                                }
                            )
                        }

                        // 14: [
                        //            h'2eb8686d882a0eddc9c7f68ece8f198f973ce90477b51ca017f2a25d',
                        //            h'4c1017ed05f703f6cf31c6a743f2c69534dbe4846e0d51ed4cb24b85',
                        //            h'8082e2be035d27cf02455606df19308b5f7022490f8a61f22e158146',
                        //        ],
                        requiredSigners {
                            add("2eb8686d882a0eddc9c7f68ece8f198f973ce90477b51ca017f2a25d".hexToByteArray())
                            add("4c1017ed05f703f6cf31c6a743f2c69534dbe4846e0d51ed4cb24b85".hexToByteArray())
                            add("8082e2be035d27cf02455606df19308b5f7022490f8a61f22e158146".hexToByteArray())
                        }

                        // 16: {
                        //   0: h'608082e2be035d27cf02455606df19308b5f7022490f8a61f22e158146',
                        //   1: 3678401_2,
                        // },
                        // calculated
//            collateralReturn = outputUtxo {
//                address = "addr_test1vzqg9c47qdwj0nczg4tqdhcexz947upzfy8c5c0j9c2cz3sg8s4av"
//                lovelace = "3678401"
//            }

                        collateralReturnAddress = "addr_test1vzqg9c47qdwj0nczg4tqdhcexz947upzfy8c5c0j9c2cz3sg8s4av"

                        // 17: 1321599_2,
                        // calculated
                        // totalCollateral = 1321599

                        // 18: [
                        //    [
                        //        h'0b4c56fdae7c23748c837e2443dfebebd020239c107aed9b227851ebabe4a829',
                        //        1,
                        //    ],
                        //    [
                        //        h'0b4c56fdae7c23748c837e2443dfebebd020239c107aed9b227851ebabe4a829',
                        //        2,
                        //    ],
                        // ],
                        referenceInputs {
                            add(
                                utxo {
                                    hash = "0b4c56fdae7c23748c837e2443dfebebd020239c107aed9b227851ebabe4a829"
                                    ix = 1
                                }
                            )
                            add(
                                utxo {
                                    hash = "0b4c56fdae7c23748c837e2443dfebebd020239c107aed9b227851ebabe4a829"
                                    ix = 2
                                }
                            )
                        }

                        transactionMetadata = createTestMetadata("NEWM_9")

                        redeemers {
                            add(
                                redeemer {
                                    tag = RedeemerTag.SPEND
                                    index = 0L
                                    data =
                                        plutusData {
                                            constr = 0
                                            list = plutusDataList { }
                                        }
                                    // calculated
//                        exUnits = exUnits {
//                            mem = 2895956L
//                            steps = 793695629L
//                        }
                                }
                            )
                            add(
                                redeemer {
                                    tag = RedeemerTag.MINT
                                    index = 0L
                                    data =
                                        plutusData {
                                            constr = 0
                                            list =
                                                plutusDataList {
                                                    listItem.add(
                                                        plutusData {
                                                            bytes =
                                                                ByteString.fromHex(
                                                                    "3ab25c853f0f188f43b34b2df5cb98737a4de7e19028ec9d1a414a55"
                                                                )
                                                        }
                                                    )
                                                    listItem.add(
                                                        plutusData {
                                                            int = 9L
                                                        }
                                                    )
                                                    listItem.add(
                                                        plutusData {
                                                            bytes = ByteString.fromHex("4e45574d5f")
                                                        }
                                                    )
                                                }
                                        }
                                    // calculated
//                        exUnits = exUnits {
//                            mem = 4953797L
//                            steps = 1339145758L
//                        }
                                }
                            )
                        }

                        signingKeys {
                            // 3 random signing keys for testing
                            add(
                                signingKey {
                                    skey =
                                        ByteString.fromHex("e30565043f3863fa3e9fd22790807e13f04be018d3ec7b2eec7d69a3671cf51c")
                                    vkey =
                                        ByteString.fromHex("d482feaa7d38f393d3a39b0b61d1bcab4213a09e8816ffa48931f26561b40260")
                                }
                            )
                            add(
                                signingKey {
                                    skey =
                                        ByteString.fromHex("6bfaab76198b8da79b1e46f77383534b1a6ddfb36b0ee33d4dd6ff019a3bff75")
                                    vkey =
                                        ByteString.fromHex("3cc51d63f04e5c509c34f6d6c2d6b51aaed557a66c8069fc06f15b17cc57d7e5")
                                }
                            )
                            add(
                                signingKey {
                                    skey =
                                        ByteString.fromHex("a7a0f3643adae688df9d9a4d29d282df4cf8ddda9de324d58155d4f0c363f1de")
                                    vkey =
                                        ByteString.fromHex("de537d884b9b84c267309e9a07a4fd857da4c81565a5d261ef4041bd08c93ca2")
                                }
                            )
                        }
                    }
                println("transaction: ${cborByteArray.toHexString()}")
                assertThat(cborByteArray.toHexString()).isEqualTo(targetCborHex)
            }
        }

    @Test
    fun `test Marketplace Sale Transaction`() =
        runBlocking {
            val targetCborHex = "???" // TODO: figure out correct value for this test after fix

            createTxSubmitClient(
                websocketHost = TEST_HOST,
                websocketPort = TEST_PORT,
                secure = TEST_SECURE,
            ).use { client ->
                val connectResult = client.connect()
                assertThat(connectResult).isTrue()
                assertThat(client.isConnected).isTrue()

                val response = (client as StateQueryClient).protocolParameters()
                val protocolParams = response.result

                val calculateTxExecutionUnits: suspend (ByteArray) -> EvaluateTxResult = { cborBytes ->
                    val evaluateResponse = client.evaluate(cborBytes.toHexString())
                    println("evaluateResponse: ${evaluateResponse.result}")
                    evaluateResponse.result
                }

                val (_, cborByteArray) =
                    transactionBuilder(protocolParams, calculateTxExecutionUnits = calculateTxExecutionUnits) {
                        sourceUtxos {
                            add(
                                utxo {
                                    address =
                                        "addr_test1qqfam8majz3desfm82qvd28yzfrg4etfas4acf4kkggaq6slhc0vy4v22ml5mrq8q40wvj648xvyllld92w6lpk2y97q04cqhl"
                                    hash = "1d9a23d6e2920c2c6686e0fd54cfad66164ec89bb584b367960e48d170958b42"
                                    ix = 1
                                    lovelace = "10140223407"
                                    nativeAssets.add(
                                        // "To The Moon" RFT
                                        nativeAsset {
                                            policy = "0379bbabfe3d39376fcc3b99fb93115e6f32d91f7bf23322cb1128f8"
                                            name = "001bc280004accf7a4dbd382e803b6397abc12478ab2480e2432f8b666ac1c49"
                                            amount = "99000000"
                                        }
                                    )
                                    nativeAssets.add(
                                        // "The World" RFT
                                        nativeAsset {
                                            policy = "0379bbabfe3d39376fcc3b99fb93115e6f32d91f7bf23322cb1128f8"
                                            name = "001bc2800084042ee2f76c8e16d160eccd3d1c56a6f6d9e6f19c63e2ff0e917f"
                                            amount = "49999650"
                                        }
                                    )
                                    nativeAssets.add(
                                        // "Chained" RFT
                                        nativeAsset {
                                            policy = "0379bbabfe3d39376fcc3b99fb93115e6f32d91f7bf23322cb1128f8"
                                            name = "001bc280022c6aa9b521e86f0df5f762c9b908de4d0606a7ce1f0220ae3b22d0"
                                            amount = "100000000"
                                        }
                                    )
                                    nativeAssets.add(
                                        // "The Canyon" RFT
                                        nativeAsset {
                                            policy = "0379bbabfe3d39376fcc3b99fb93115e6f32d91f7bf23322cb1128f8"
                                            name = "001bc28002d6e1cec3fb424e74c18a33831f0b0779d956804d2e58789956677e"
                                            amount = "100000000"
                                        }
                                    )
                                    nativeAssets.add(
                                        // "An Old Man" RFT
                                        nativeAsset {
                                            policy = "0379bbabfe3d39376fcc3b99fb93115e6f32d91f7bf23322cb1128f8"
                                            name = "001bc280035554597f74024e28d4ba340fe5ddac9e7ea1f420120130d77ff675"
                                            amount = "100000000"
                                        }
                                    )
                                    nativeAssets.add(
                                        // tDRIP
                                        nativeAsset {
                                            policy = "698a6ea0ca99f315034072af31eaac6ec11fe8558d3f48e9775aab9d"
                                            name = "7444524950"
                                            amount = "3000000000"
                                        }
                                    )
                                    nativeAssets.add(
                                        // tNEWM
                                        nativeAsset {
                                            policy = "769c4c6e9bc3ba5406b9b89fb7beb6819e638ff2e2de63f008d5bcff"
                                            name = "744e45574d"
                                            amount = "799857553108"
                                        }
                                    )
                                }
                            )
                        }

                        outputUtxos {
                            add(
                                outputUtxo {
                                    address =
                                        "addr_test1xz0l8seu9tv265wu6gkajuarl62zz8uhq75u7gl9n6t8yuetr7epf7y6wzykrhezrfuptq7s4lgefz5m4a3cugsr8eyqej32nc"
                                    lovelace = "4504110"
                                    nativeAssets.add(
                                        // "To The Moon" RFT
                                        nativeAsset {
                                            policy = "0379bbabfe3d39376fcc3b99fb93115e6f32d91f7bf23322cb1128f8"
                                            name = "001bc280004accf7a4dbd382e803b6397abc12478ab2480e2432f8b666ac1c49"
                                            amount = "1000000"
                                        }
                                    )
                                    nativeAssets.add(
                                        // Pointer Token
                                        nativeAsset {
                                            policy = "4e00b9275d13e901116778c495a4d621deb65062a24d3c1a4b91852f"
                                            name = "ca11ab1e01eeb2a5a03e85400368be81301887f66fde1164953f85cc8a877f4a"
                                            amount = "1"
                                        }
                                    )
                                    datum =
                                        "d8799fd8799f581c1fbe1ec2558a56ff4d8c07055ee64b5539984fffed2a9daf86ca217c40ffd8799f581c0379bbabfe3d39376fcc3b99fb93115e6f32d91f7bf23322cb1128f85820001bc280004accf7a4dbd382e803b6397abc12478ab2480e2432f8b666ac1c4901ffd8799f581c769c4c6e9bc3ba5406b9b89fb7beb6819e638ff2e2de63f008d5bcff45744e45574d02ff1a000f4240ff"
                                }
                            )
                        }

                        changeAddress =
                            "addr_test1qqfam8majz3desfm82qvd28yzfrg4etfas4acf4kkggaq6slhc0vy4v22ml5mrq8q40wvj648xvyllld92w6lpk2y97q04cqhl"

                        mintTokens {
                            add(
                                // Pointer Token
                                nativeAsset {
                                    policy = "4e00b9275d13e901116778c495a4d621deb65062a24d3c1a4b91852f"
                                    name = "ca11ab1e01eeb2a5a03e85400368be81301887f66fde1164953f85cc8a877f4a"
                                    amount = "1"
                                }
                            )
                        }

                        referenceInputs {
                            add(
                                // script
                                utxo {
                                    hash = "f44798098faa85f69c57211b749358908c7ad47820bdb50230aae82c0eff3cd0"
                                    ix = 0
                                }
                            )
                            add(
                                utxo {
                                    hash = "d156580705c936bbe9c379eab79774e2da905258ad53700e63d7635cabdca475"
                                    ix = 1
                                }
                            )
                            add(
                                utxo {
                                    hash = "2b09daf2137c654fbfa7117d843c97126f47b9f0f7468bce06453d7ef5e1d92f"
                                    ix = 1
                                }
                            )
                            add(
                                utxo {
                                    hash = "6cfc6a0b76567d5646a1408e3eae6dadbcf178ce62f71f22b399e08966e6973d"
                                    ix = 1
                                }
                            )
                        }

                        collateralUtxos {
                            add(
                                utxo {
                                    address =
                                        "addr_test1qqfam8majz3desfm82qvd28yzfrg4etfas4acf4kkggaq6slhc0vy4v22ml5mrq8q40wvj648xvyllld92w6lpk2y97q04cqhl"
                                    hash = "ef502e880def38ef770ae051ba190774df81dded12df8cbbfd4a393523c580d2"
                                    ix = 5
                                    lovelace = "5000000"
                                }
                            )
                        }

                        collateralReturnAddress =
                            "addr_test1qqfam8majz3desfm82qvd28yzfrg4etfas4acf4kkggaq6slhc0vy4v22ml5mrq8q40wvj648xvyllld92w6lpk2y97q04cqhl"

                        redeemers {
                            add(
                                redeemer {
                                    tag = RedeemerTag.MINT
                                    index = 0L
                                    data = plutusData {
                                        constr = 0
                                        list = plutusDataList { }
                                    }
                                }
                            )
                        }

                        requiredSigners {
                            add("2eb8686d882a0eddc9c7f68ece8f198f973ce90477b51ca017f2a25d".hexToByteArray())
                            add("4c1017ed05f703f6cf31c6a743f2c69534dbe4846e0d51ed4cb24b85".hexToByteArray())
                        }

                        signingKeys {
                            // 2 random signing keys for testing
                            add(
                                signingKey {
                                    skey =
                                        ByteString.fromHex("e30565043f3863fa3e9fd22790807e13f04be018d3ec7b2eec7d69a3671cf51c")
                                    vkey =
                                        ByteString.fromHex("d482feaa7d38f393d3a39b0b61d1bcab4213a09e8816ffa48931f26561b40260")
                                }
                            )
                            add(
                                signingKey {
                                    skey =
                                        ByteString.fromHex("6bfaab76198b8da79b1e46f77383534b1a6ddfb36b0ee33d4dd6ff019a3bff75")
                                    vkey =
                                        ByteString.fromHex("3cc51d63f04e5c509c34f6d6c2d6b51aaed557a66c8069fc06f15b17cc57d7e5")
                                }
                            )
                        }
                    }
                println("transaction: ${cborByteArray.toHexString()}")
                assertThat(cborByteArray.toHexString()).isEqualTo(targetCborHex)
            }
        }

    private fun createTestMetadata(tokenName: String): CborMap =
        CborMap.create(
            mapOf(
                CborInteger.create(721) to
                    CborMap.create(
                        mapOf(
                            CborTextString.create("602a7e54f9c569c770ba6f732af3077d8b06e14b84abc8d0151635d8") to
                                CborMap.create(
                                    mapOf(
                                        CborTextString.create(tokenName) to
                                            CborMap.create(
                                                mapOf(
                                                    CborTextString.create("album_title") to CborTextString.create(
                                                        "Indiclouds"
                                                    ),
                                                    CborTextString.create("artists") to
                                                        CborArray.create(
                                                            listOf(
                                                                CborMap.create(
                                                                    mapOf(
                                                                        CborTextString.create("name") to CborTextString.create(
                                                                            "CÜSI"
                                                                        )
                                                                    )
                                                                )
                                                            )
                                                        ),
                                                    CborTextString.create("copyright") to CborTextString.create(
                                                        "℗ CÜSI"
                                                    ),
                                                    CborTextString.create("distributor") to CborTextString.create(
                                                        "https://newm.io"
                                                    ),
                                                    CborTextString.create("explicit") to CborTextString.create(
                                                        "true"
                                                    ),
                                                    CborTextString.create("files") to
                                                        CborArray.create(
                                                            listOf(
                                                                CborMap.create(
                                                                    mapOf(
                                                                        CborTextString.create(
                                                                            "mediaType"
                                                                        ) to CborTextString.create("audio/mpeg"),
                                                                        CborTextString.create(
                                                                            "name"
                                                                        ) to CborTextString.create("Indiclouds"),
                                                                        CborTextString.create(
                                                                            "src"
                                                                        ) to
                                                                            CborTextString.create(
                                                                                "ar://rCI7LHIT9jTYe_oxen8lZaEB2U-Bbwu5MGQYuKrFXrw"
                                                                            ),
                                                                    )
                                                                ),
                                                                CborMap.create(
                                                                    mapOf(
                                                                        CborTextString.create(
                                                                            "mediaType"
                                                                        ) to CborTextString.create("application/pdf"),
                                                                        CborTextString.create(
                                                                            "name"
                                                                        ) to CborTextString.create("Streaming Royalty Share Agreement"),
                                                                        CborTextString.create(
                                                                            "src"
                                                                        ) to
                                                                            CborTextString.create(
                                                                                "ar://_M4QxzGUwdIZCckwNbCPboKCybR5y_UBNc5AYTWRTxc"
                                                                            ),
                                                                    )
                                                                )
                                                            )
                                                        ),
                                                    CborTextString.create("genres") to
                                                        CborArray.create(
                                                            listOf(
                                                                CborTextString.create("Hip Hop"),
                                                                CborTextString.create("Rap"),
                                                            )
                                                        ),
                                                    CborTextString.create(
                                                        "image"
                                                    ) to CborTextString.create("ar://M5oVJOOoxlBlHCzlwHYHAhKwxrnd1jyrnsAWI7jw6XA"),
                                                    CborTextString.create("isrc") to CborTextString.create("QZ-NW7-23-46503"),
                                                    CborTextString.create("language") to CborTextString.create(
                                                        "en-US"
                                                    ),
                                                    CborTextString.create("links") to
                                                        CborMap.create(
                                                            mapOf(
                                                                CborTextString.create(
                                                                    "facebook"
                                                                ) to
                                                                    CborTextString.create(
                                                                        "https://www.facebook.com/profile.php?id=100087220639690"
                                                                    ),
                                                                CborTextString.create(
                                                                    "instagram"
                                                                ) to CborTextString.create("https://instagram.com/Cusi.world"),
                                                                CborTextString.create(
                                                                    "linktree"
                                                                ) to CborTextString.create("https://linktr.ee/CusiWorld"),
                                                                CborTextString.create("spotify") to
                                                                    CborArray.create(
                                                                        listOf(
                                                                            CborTextString.create(
                                                                                "https://open.spotify.com/artist/6LJSWCQif1oZjIch2aDs00?si=83dMu"
                                                                            ),
                                                                            CborTextString.create("dS-RSmWL6FYQ0i_0g")
                                                                        )
                                                                    ),
                                                                CborTextString.create(
                                                                    "twitter"
                                                                ) to CborTextString.create("https://twitter.com/CusiWorld"),
                                                                CborTextString.create(
                                                                    "youtube"
                                                                ) to
                                                                    CborTextString.create(
                                                                        "https://www.youtube.com/channel/UCcmX6Is8W977wTOB0oZqrog"
                                                                    ),
                                                            )
                                                        ),
                                                    CborTextString.create(
                                                        "lyrics"
                                                    ) to CborTextString.create("ar://TNHN0v2wTsUhNprgWvEHC_CZvGi5xgb_oU8MAWvqV0Y"),
                                                    CborTextString.create("mediaType") to CborTextString.create(
                                                        "image/webp"
                                                    ),
                                                    CborTextString.create("metadata_language") to CborTextString.create(
                                                        "en-US"
                                                    ),
                                                    CborTextString.create("mix_engineer") to CborTextString.create(
                                                        "Nathan Cusumano"
                                                    ),
                                                    CborTextString.create("mood") to CborTextString.create("Inspiring"),
                                                    CborTextString.create("music_metadata_version") to CborInteger.create(
                                                        1
                                                    ),
                                                    CborTextString.create("name") to CborTextString.create("CÜSI - Indiclouds"),
                                                    CborTextString.create("parental_advisory") to CborTextString.create(
                                                        "Explicit"
                                                    ),
                                                    CborTextString.create("producer") to CborTextString.create(
                                                        "Mchale Beats"
                                                    ),
                                                    CborTextString.create("recording_engineer") to CborTextString.create(
                                                        "Nathan Cusumano"
                                                    ),
                                                    CborTextString.create("release_date") to CborTextString.create(
                                                        "2022-03-17"
                                                    ),
                                                    CborTextString.create("release_type") to CborTextString.create(
                                                        "Single"
                                                    ),
                                                    CborTextString.create("series") to CborTextString.create(
                                                        "CÜSI WORLD"
                                                    ),
                                                    CborTextString.create("song_duration") to CborTextString.create(
                                                        "PT3M7S"
                                                    ),
                                                    CborTextString.create("song_title") to CborTextString.create(
                                                        "Indiclouds"
                                                    ),
                                                    CborTextString.create("track_number") to CborInteger.create(
                                                        1
                                                    ),
                                                )
                                            )
                                    )
                                ),
                            CborTextString.create("version") to CborInteger.create(1),
                        )
                    )
            )
        )
}
