package io.newm.server.utils

import com.google.common.truth.Truth.assertThat
import io.newm.server.ktx.cborHexToUtxo
import org.junit.jupiter.api.Test

class CborParseTest {
    @Test
    fun testCborParse() {
        val cborHex = "82825820e7118bddb80accfd415894bde15d6d5e06f25b33ee2882347c4c88809c9c4da00182583901eac697f749fed5669b4c4ebe342a37f2d5b8ffd6f87edf8a7af5d7f73ee8ea1e1b4239485f0f07ded1d0085717b4741d34c6221e61534684821a00f70147b844581c057e283baef26fe8879f0a5b56d55893507cee6933c85babcb8db216a1434644411a00061a80581c10a49b996e2402269af553a8a96fb8eb90d79e9eca79e2b4223057b6a1444745524f1a0075180e581c133fac9e153194428eb0919be39837b42b9e977fc7298f3ff1b76ef9a14550554447591a01c9c380581c16fdd33c86af604e837ae57d79d5f0f1156406086db5f16afb3fcf51a14544474f4c441a05f5e100581c1746ee8096f7f1da9c1858a3254db610106e0d9cc42fce300d5e2ed7a1504361726461676f74636869303935383301581c1a71dc14baa0b4fcfb34464adc6656d0e562571e2ac1bc990c9ce5f6a144574f4c461b00000002d1e0e3c6581c1ddcb9c9de95361565392c5bdff64767492d61a96166cb16094e54bea1434f50541a02282e64581c1f7617d89caae552ab750a55c3449c93464bbdcf478ca5b82805628da14c5061426c69747a546f6b656e01581c20cd68533b47565f3c61efb39c30fdace9963bfa4c0060b613448e3ca14650524f5849451a000f4240581c270a4b2c5d896c53ebeb241e111a60b590f2cecc32a204c3567a47c9a15157697a6172645370656c6c7a303039353001581c279c909f348e533da5808898f87f9a14bb2c3dfbbacccd631d927a3fa144534e454b199d68581c2af89e99de1bf692b10230c0b38fe89cc764364d0e15cc7cc47e0ad0a1534372617a79204b69746368656e20566f6c203101581c2afb448ef716bfbed1dcb676102194c3009bee5399e93b90def9db6aa1454249534f4e1aef03be80581c2b28c81dbba6d67e4b5a997c6be1212cba9d60d33f82444ab8b1f218a14442414e4b1a001bf01f581c2c763e2db8337324df122fffa38e0d97ecdf53d139025a0e1b6d6b97a25043564c616e6453315831353559313030015243617264616e6f56696c6c6167653735303301581c2d587111358801114f04df83dc0015de0a740b462b75cce5170fc935a1434347491a0136915e581c31de9a458d8c14762a3eb4a7d99922d1a1678b44dc2f1960770f6c2ca14d57656c636f6d65546f4e46547301581c34a7180c8fa72689516e115c51a97bd512b495b3f993078a431125a2a15054686544656174686f6641727432373501581c3742e0f91c2f140433d4d98d40c0869ea163db5b378f73b1b8173ac7a1581836343835356334316136633362386432313333313034396101581c3807e8461adbf6c8cac0b91f6cafdefb6ad2b0d9bf96d5f0e48b7489a15043617264616e6f50756e6b313034303001581c385d75ef2416d41536407848e4e9668d80d4bce5b63ba45ecfd5b8daa1581836343938393664643233376265613030376333383662386201581c3a617755329db3205ce10e8f2d16032332ede9c71abb855428a66667a149466163654d6172653301581c3cda7e95bd0b46d20a9f2e0ad762a4464ef0a7b82f910a53efccb67da151506c61736d61303253706865726531343801581c4035aaeec62f8a55bb0b8a297ee3af66d2d9bee339efeaa9a222a54aa1514861707079486f6c696461797a3230323201581c4247d5091db82330100904963ab8d0850976c80d3f1b927e052e07bda146546f6b68756e07581c46c04fa6a019010ee0d8aaedb0573d16ce2580d427476073fd7bffc1a1581836343935626639373639616338316334386163346238653601581c47207b53d4a031fa7d023a8ffd154c8db855c7b42201ff44d3847d4fa25243617264616e6f456d7069726573436f696e1903e85543617264616e6f456d7069726573526f6d6535303901581c4ba6b83caaad536a242080253feb598848382507c7426737568ea942a150536861706573416964726f703130363901581c4fd0d998dc0700ca6ef89fafff05fbf523a3a25c1fc8314bf7e4d1c2a14647726f77746814581c544571c086d0e5c5022aca9717dd0f438e21190abb48f37b3ae129f0a14447524f5702581c5ad8deb64bfec21ad2d96e1270b5873d0c4d0f231b928b4c39eb2435a14661646f7369611a21f98280581c5dac8536653edc12f6f5e1045d8164b9f59998d3bdc300fc92843489a1444e4d4b521aef047a5b581c5e688b876cc9f48e129654567b3b88bccd560b62f5d1745c555be84fa24001444e534657181a581c6194158d24d71eca5cc5601c45de123bf78d02c297e851be2608810aa14444454144190118581c63766427b4499dd678cb8b715dec3265dd292279ce7779447e3651e5a1434b4f5a02581c653aae5e966b9ad14448446ff177bab27ede6587e03fa926d2e7286fa1546a7572617373696b636861696e6564303533373501581c682fe60c9918842b3323c43b5144bc3d52a23bd2fb81345560d73f63a1444e45574d1a02cd1cd1581c684ffa75d83ccd4dfe179bd37fe679e74d33cce181a6f473337df098a24868756e6b36333132014868756e6b3639363401581c6954264b15bc92d6d592febeac84f14645e1ed46ca5ebb9acdb5c15fa1455354524950191770581c6ddd6503e0ab63538c77bd51f679b9ed84998af89901b01ce3dace88a14b5369636b4369747933333901581c7652a254a5691f32203c093fcbd74193447c8724cadaf5e7402cf33ba54e53706163654368756e6b36363633014e53706163654368756e6b36363634014e53706163654368756e6b36363732014e53706163654368756e6b36363733014e53706163654368756e6b3636373401581c7b86666669fc119d2a688886004a3fbec201e2d224676176afcc96aba1476565786974313501581c7f376e3d1cf52e6c4350a1a91c8f8d0f0b63baedd443999ebe8fe57aa145424f52475a199c40581c804f5544c1962a40546827cab750a88404dc7108c0f588b72964754fa144565946491a001312dd581c827ee7a1d94d7cd29f822ce5e53865603c2457f451578eff08eb3a90a1564861726d6f6e796f6674686557696c64666c6f77657201581c86d318effcb8b09f4a8c7c4bddb70b9745d9772b35235853f0104624a24b5368617065733236303832014b536861706573343130303501581c8c202450b2b4dc247e79e6e4b6f8991d3b10615f4507cadccb9f1d8ca1581836346264616635306333303165386161363231313161666202581c8d0ae3c5b13b47907b16511a540d47436d12dcc96453c0f59089b451a14542524f4f4d1a004c4b40581c95a427e384527065f2f8946f5e86320d0117839a5e98ea2c0b55fb00a14448554e541a03e0e69a581c99597f46440013917db080428b8874e581345fc0fe87a1a28abf4d84a3454e4954524f18645043617264616e6f526163653030353639015043617264616e6f52616365303036393001581c9c54bb728df9847b0358c84c942500646326a1b581a2b8c43689d220a14c5374726970706572436f696e0f581ca134724e20a26031a75ca1bc8effc60d3beb667c449c95d91587b137a157486f736b696e736f6e323032325768697465626f61726401581cad1c1ed3a0bb6f83630ef052a31b71ba3287bf4bafa8370758f14d31a1445349434b199c40581caea1ceb3625680e2f7a31ea1a64c9c430c34d8588bf4f8e3c7f9b1dda146476f746368691a0d693a40581caf2b842faba95698a08febd5f60c7abd8f975b76f6acbbd1c16f6f0ba149466163654d6172653702581caf2e27f580f7f08e93190a81f72462f153026d06450924726645891ba144445249501b00000008614ff4a6581cb0af30edf2c7f11465853821137e0a6ebc395cab71ee39c24127ffb4a1444e4654431832581cb1eb73a732247342724b85ca10f626b9494c69b6f5d21a2bd4052bf7a150544150316368696c6c696e673230323201581cb4df41688e987343fd5c83c95d88eb21930a9ded7776ec71e1bc2720a14a466163654d617265313001581cb6408f665a71750e622a3f6430f35a1a6d6cde0d0b6c41bc027c0356a14f50726f6a656374426f6f6b776f726d06581cb6a7467ea1deb012808ef4e87b5ff371e85f7142d7b356a40d9b42a0a1581e436f726e75636f70696173205b76696120436861696e506f72742e696f5d1a004c4b40581cc0ee29a85b13209423b10447d3c2e6a50641a15c57770e27cb9d5073a14a57696e675269646572731a000f429b581cc11fdf3fa883388ca6e7f4002b5b6883df8617847b78f6e0f53a7df4a1581b4269706f6c6172466967757261746976655375727265616c69736d01581cc16777d742784b7acd3940e0356596d313e141200fe8e48c8df16ba7a15542697446696e735361776261636b5261726531393801581cd030b626219d81673bd32932d2245e0c71ae5193281f971022b23a78a148436172646f67656f1904ec581cd894897411707efa755a76deb66d26dfd50593f2e70863e1661e98a0a14a7370616365636f696e730c581cdda5fdb1002f7389b33e036b6afee82a8189becb6cba852e8b79b4fba1480014df1047454e531a019c85de581cfc11a9ef431f81b837736be5f53e4da29b9469c983d07f321262ce61a1444652454e1a0042b4fa"
        val utxo = cborHex.cborHexToUtxo()
        assertThat(utxo).isNotNull()
    }
}