package io.newm.server.features.cardano

import com.google.common.truth.Truth.assertThat
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.newm.server.BaseApplicationTests
import io.newm.server.features.cardano.database.KeyEntity
import io.newm.server.features.cardano.database.KeyTable
import io.newm.server.features.cardano.model.WalletSong
import io.newm.server.features.song.database.SongEntity
import io.newm.server.features.song.database.SongTable
import io.newm.server.features.song.model.AudioEncodingStatus
import io.newm.server.features.song.model.MarketplaceStatus
import io.newm.server.features.song.model.MintingStatus
import io.newm.server.features.song.model.Song
import io.newm.server.features.song.model.SongBarcodeType
import io.newm.server.features.user.database.UserEntity
import io.newm.server.features.user.database.UserTable
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.SqlExpressionBuilder.neq
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class CardanoRoutesTests : BaseApplicationTests() {

    @BeforeEach
    fun beforeEach() {
        transaction {
            SongTable.deleteAll()
            KeyTable.deleteAll()
            UserTable.deleteWhere { id neq testUserId }
        }
    }

    @Test
    fun testGetWalletSongs() = runBlocking {
        // Add Songs directly into database
        addSongToDatabase(
            "beec4fac1e41e603f4a8620d7864c1e2d55a2b9ae5522b675cfa6c52",
            "001bc2800077a94bcd298f0e44f19d372bf030f442e7136bfeeca9e780c96e49",
            0,
        )
        addSongToDatabase(
            "e65559518eef9ebc25d3bacfa3f037d3e8cf0830b879c9a3fc6d7617",
            "001bc28002d5957fafadcfdff4eb3e8bd4e6e7f980e6584ce831161ca0845a89",
            1,
        )
        addSongToDatabase(
            "e65559518eef9ebc25d3bacfa3f037d3e8cf0830b879c9a3fc6d7617",
            "001bc280008195b5efb64f5601adeb9331404cc75d73cb9ef388191383a0d592",
            2,
        )
        addSongToDatabase(
            "e65559518eef9ebc25d3bacfa3f037d3e8cf0830b879c9a3fc6d7617",
            "001bc28000cc84f8f8a93258f86117f6c19a7f5aedb9cbfbb6e9d2f2ced9e5ce",
            3,
        )
        addSongToDatabase(
            "e65559518eef9ebc25d3bacfa3f037d3e8cf0830b879c9a3fc6d7617",
            "001bc28002555c04ac19d878672ef99e0bd8f825102acf33c1e2fc4d4e57afc5",
            4,
        )
        addSongToDatabase(
            "e65559518eef9ebc25d3bacfa3f037d3e8cf0830b879c9a3fc6d7617",
            "001bc2800047f5c9671760f3c337d4c0a94c0bb1ca0eb474984c30eab072388e",
            5,
        )
        addSongToDatabase(
            "e65559518eef9ebc25d3bacfa3f037d3e8cf0830b879c9a3fc6d7617",
            "001bc28002c199236a70e691cab1f7ca6b41127591389cda5c7d17f6d45cb012",
            6,
        )
        addSongToDatabase(
            "e65559518eef9ebc25d3bacfa3f037d3e8cf0830b879c9a3fc6d7617",
            "001bc280003f435ce3926a65f9394fa262ab15d57a331b3c53164a698384742e",
            7,
        )
        addSongToDatabase(
            "e65559518eef9ebc25d3bacfa3f037d3e8cf0830b879c9a3fc6d7617",
            "001bc280025c16873823669e8295a6b7c7f0fa5b923932c6ff7ac6084b547e6d",
            8,
        )
        addSongToDatabase(
            "e65559518eef9ebc25d3bacfa3f037d3e8cf0830b879c9a3fc6d7617",
            "001bc280037fc71d6cf5c0601c867316f0584a5cebe9288bc37a8def20688fb8",
            9,
        )
        addSongToDatabase(
            "e65559518eef9ebc25d3bacfa3f037d3e8cf0830b879c9a3fc6d7617",
            "001bc280021f1c4da0424c87f7c6611bd53eaaa762cf9335d10bf66be2753c54",
            10,
        )
        addSongToDatabase(
            "e65559518eef9ebc25d3bacfa3f037d3e8cf0830b879c9a3fc6d7617",
            "001bc28003e2f09ffc9ebf5b278ac4bbe3b155e52e752477e2f1124679144b4a",
            11,
        )
        addSongToDatabase(
            "e65559518eef9ebc25d3bacfa3f037d3e8cf0830b879c9a3fc6d7617",
            "001bc28002f7f7802b27301d6497ad5532c9ad1f536b3dfdb62ae78e5177b26a",
            12,
        )
        addSongToDatabase(
            "e65559518eef9ebc25d3bacfa3f037d3e8cf0830b879c9a3fc6d7617",
            "001bc2800097917965299e67262e6377bff6e639fd541652fdce551dee341f0f",
            13,
        )
        addSongToDatabase(
            "e65559518eef9ebc25d3bacfa3f037d3e8cf0830b879c9a3fc6d7617",
            "001bc28000abb03a9b0b538ce8178742424d4acf342c2549b906f2c8ba583f17",
            14,
        )
        addSongToDatabase(
            "e65559518eef9ebc25d3bacfa3f037d3e8cf0830b879c9a3fc6d7617",
            "001bc28002733c0f176d852c1aeb1d81b905a6882ca48935d5c4ebac80b4a705",
            15,
        )
        addSongToDatabase(
            "e65559518eef9ebc25d3bacfa3f037d3e8cf0830b879c9a3fc6d7617",
            "001bc28002d6108d5ee8c1fe8d5076f06b0d0cde998b5b4c4b8a14e389322549",
            16,
        )
        addSongToDatabase(
            "e65559518eef9ebc25d3bacfa3f037d3e8cf0830b879c9a3fc6d7617",
            "001bc2800068eae71696870af9aad3c28b65f3e30747961dec58d2d21d123457",
            17,
        )
        addSongToDatabase(
            "e65559518eef9ebc25d3bacfa3f037d3e8cf0830b879c9a3fc6d7617",
            "001bc28002b7c01a7003e7e9b7029ae6fad06c5ac932f16208b77f86017a622a",
            18,
        )
        addSongToDatabase(
            "e65559518eef9ebc25d3bacfa3f037d3e8cf0830b879c9a3fc6d7617",
            "001bc280002d899c14d4e638108afcbf9a0452d3270bba54cbb0c6e06bfcfbb9",
            19,
        )
        addSongToDatabase(
            "e65559518eef9ebc25d3bacfa3f037d3e8cf0830b879c9a3fc6d7617",
            "001bc2800223ea1f7424ada51c9983aa0b1c9186525717b2af8926bc874eba8b",
            20,
        )
        addSongToDatabase(
            "e65559518eef9ebc25d3bacfa3f037d3e8cf0830b879c9a3fc6d7617",
            "001bc28000cbc52ca9af6d5ad9389cb132fab3303ab072e93554a2433d32e9c0",
            21,
        )
        addSongToDatabase(
            "e65559518eef9ebc25d3bacfa3f037d3e8cf0830b879c9a3fc6d7617",
            "001bc280005d07e877aaca585abb958cdacd6e78b3166a0d36ad7dedac6934c5",
            22,
        )
        addSongToDatabase(
            "e65559518eef9ebc25d3bacfa3f037d3e8cf0830b879c9a3fc6d7617",
            "001bc280007f88bac8495bfe357b553e31c81677619ace8b26027e084952c1e4",
            23,
        )
        addSongToDatabase(
            "e65559518eef9ebc25d3bacfa3f037d3e8cf0830b879c9a3fc6d7617",
            "001bc28000d09be5b0057031385c1570694b9fc48db118a8a4afac00edf905b0",
            24,
        )
        addSongToDatabase(
            "e65559518eef9ebc25d3bacfa3f037d3e8cf0830b879c9a3fc6d7617",
            "001bc28000d94c13c910beb0bc27eebe74e68bcacce8c00ac4de37beba4fc5d8",
            25,
        )
        addSongToDatabase(
            "e65559518eef9ebc25d3bacfa3f037d3e8cf0830b879c9a3fc6d7617",
            "001bc28000299ca37b9a0f5a75f085992bfb4879e1b2288be1b1cf58397fdbb1",
            26,
        )
        addSongToDatabase(
            "e65559518eef9ebc25d3bacfa3f037d3e8cf0830b879c9a3fc6d7617",
            "001bc2800279ba8abfb36d906f097e4cd66fdc05232bca5fc0aff5a0ec6f763e",
            27,
        )
        addSongToDatabase(
            "e65559518eef9ebc25d3bacfa3f037d3e8cf0830b879c9a3fc6d7617",
            "001bc2800017a1018de395c1c848b4900841bba26f017e7b0b3fcb4fc1ca7336",
            28,
        )
        addSongToDatabase(
            "e65559518eef9ebc25d3bacfa3f037d3e8cf0830b879c9a3fc6d7617",
            "001bc2800213778ad38b8904c669d0a12f0f22b866d86b1f27a6f23bbe2d4513",
            29,
        )
        addSongToDatabase(
            "e65559518eef9ebc25d3bacfa3f037d3e8cf0830b879c9a3fc6d7617",
            "001bc280038b0644f9c145d45b64272d47679f7d65ed9ad0f0091de6e7dacb6d",
            30,
        )
        addSongToDatabase(
            "e65559518eef9ebc25d3bacfa3f037d3e8cf0830b879c9a3fc6d7617",
            "001bc280031cfd72bbf3ac98873abc8abdf0ba058d23327a43292c39fb8a41db",
            31,
        )
        addSongToDatabase(
            "e65559518eef9ebc25d3bacfa3f037d3e8cf0830b879c9a3fc6d7617",
            "001bc2800089af82e9c7f11645cc671dc366a36aff905f03319d624dcc62201a",
            32,
        )
        addSongToDatabase(
            "e65559518eef9ebc25d3bacfa3f037d3e8cf0830b879c9a3fc6d7617",
            "001bc2800318192309b6cbf9bd72ace1e75cfeecb07ff0aab89b7f51a77c7584",
            33,
        )
        addSongToDatabase(
            "e65559518eef9ebc25d3bacfa3f037d3e8cf0830b879c9a3fc6d7617",
            "001bc28003c52e385178e61415963fe9ef26b7ea9a8fab9e4ac1b6e1c2fa1474",
            34,
        )
        addSongToDatabase(
            "e65559518eef9ebc25d3bacfa3f037d3e8cf0830b879c9a3fc6d7617",
            "001bc28000f88b4548fd9422ddaac180c47c6e8c26d076a7f2ba5c6fbae9b3ea",
            35,
        )
        addSongToDatabase(
            "e65559518eef9ebc25d3bacfa3f037d3e8cf0830b879c9a3fc6d7617",
            "001bc28000aaceb73adb90a208fbbb25edea67a1e59b9f5a9e1813107b01b3d5",
            36,
        )
        addSongToDatabase(
            "e65559518eef9ebc25d3bacfa3f037d3e8cf0830b879c9a3fc6d7617",
            "001bc28002ddaf9fd42e1996d80bb8466757ff378363133d03fed5bfe95dcbfe",
            37,
        )
        addSongToDatabase(
            "e65559518eef9ebc25d3bacfa3f037d3e8cf0830b879c9a3fc6d7617",
            "001bc28000787d65e0196b4a32b2ef0e87c5bfaaf603042474afff866e1769ec",
            38,
        )
        addSongToDatabase(
            "e65559518eef9ebc25d3bacfa3f037d3e8cf0830b879c9a3fc6d7617",
            "001bc28002521428c5d7fd2a8fbbbab6993cbde2584475ccca0cd8faa14b49a2",
            39,
        )

        // Get wallet songs
        val response = client.get("v1/cardano/songs") {
            bearerAuth(testUserToken)
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
            parameter("sortOrder", "desc")
            setBody(
                listOf(
                    "82825820bc43a0453f0681d841798aafa11be39c49215ed9031d6dc29cb79dfeda05239a01825839003a5cbc099211950e25f9877ab7abd63d056d515bd64823a5de83dc2653daab1c1256d159150ed263bec087e6a6109b465b0150e9e3bb4269821a0013a9f2a1581c36a4b27112c109a41086900abc145322b16921c522e4ff8fc9dd6978a15820001bc280005c3595c1532c1059535a65ccbba1b893fd3ee58f3d2b69dae23b701a05f5e100",
                    "828258204dd9215f2c3900f9760793148e4ca37f22d8447e45947c27321134672fd64b2001825839003a5cbc099211950e25f9877ab7abd63d056d515bd64823a5de83dc2653daab1c1256d159150ed263bec087e6a6109b465b0150e9e3bb4269821a0013a9f2a1581ce65559518eef9ebc25d3bacfa3f037d3e8cf0830b879c9a3fc6d7617a15820001bc280008557b67dfef2ddfdc102ed2b6c224bc266c44dc3401ff600e165011a05f5e100",
                    "828258200393ec3e96eebdaeb5b0e3068b3091e96bfeb4362faeee403841f6b2ef8621ff01825839003a5cbc099211950e25f9877ab7abd63d056d515bd64823a5de83dc2653daab1c1256d159150ed263bec087e6a6109b465b0150e9e3bb4269821a0013a9f2a1581ce65559518eef9ebc25d3bacfa3f037d3e8cf0830b879c9a3fc6d7617a15820001bc280026c86f6bd99bb240d28a483d4c5a6ce88a1db952ef7972cc9fdc9ef1a05f5e100",
                    "82825820a6ff665ae54eefcfc71daf938aa557907af18b33554f2a44b5229cbea80ca41101825839003a5cbc099211950e25f9877ab7abd63d056d515bd64823a5de83dc2653daab1c1256d159150ed263bec087e6a6109b465b0150e9e3bb42691a109b8b7b",
                    "82825820d59f8f47461986c62cca38e7e791ce77c545b1551f11bf241ae64f00085545cd01825839003a5cbc099211950e25f9877ab7abd63d056d515bd64823a5de83dc2653daab1c1256d159150ed263bec087e6a6109b465b0150e9e3bb4269821a0013a9f2a1581ce65559518eef9ebc25d3bacfa3f037d3e8cf0830b879c9a3fc6d7617a15820001bc280006b0a3df9a1349c931a7f572602d167532474704b7d281b8750d7611a05f5e100",
                    "82825820d1edcdaf2cea5eb723d7ea99a8fc5042171e7c7c059ea6de422a042b0b466e9803825839003a5cbc099211950e25f9877ab7abd63d056d515bd64823a5de83dc2653daab1c1256d159150ed263bec087e6a6109b465b0150e9e3bb42691a1860d840",
                    "82825820227d6d29e52f7d2ebf292e95e59be208bba477a91d3c7c38ff1c95bcedc687f703825839003a5cbc099211950e25f9877ab7abd63d056d515bd64823a5de83dc2653daab1c1256d159150ed263bec087e6a6109b465b0150e9e3bb42691a1dbe22c0",
                    "828258200f670e19e50924827302846a9aa216a7a68445e8aeabb1d649363b137311c26701825839003a5cbc099211950e25f9877ab7abd63d056d515bd64823a5de83dc2653daab1c1256d159150ed263bec087e6a6109b465b0150e9e3bb4269821a0013a9f2a1581ce65559518eef9ebc25d3bacfa3f037d3e8cf0830b879c9a3fc6d7617a15820001bc28002ddaf9fd42e1996d80bb8466757ff378363133d03fed5bfe95dcbfe1a05f5e100",
                    "82825820821f70e6de26a8b96eb307768e8b246836cdf15dcb900ba857c5b833fdb72ca101825839003a5cbc099211950e25f9877ab7abd63d056d515bd64823a5de83dc2653daab1c1256d159150ed263bec087e6a6109b465b0150e9e3bb4269821a0013a9f2a1581ce65559518eef9ebc25d3bacfa3f037d3e8cf0830b879c9a3fc6d7617a15820001bc28003c52e385178e61415963fe9ef26b7ea9a8fab9e4ac1b6e1c2fa14741a05f5e100",
                    "8282582010bff3d1b85c55a18fb0c9c639aaf4f21866fb1b63d6a78b6208b805dc99b0ac01825839003a5cbc099211950e25f9877ab7abd63d056d515bd64823a5de83dc2653daab1c1256d159150ed263bec087e6a6109b465b0150e9e3bb4269821a0013a9f2a1581ce65559518eef9ebc25d3bacfa3f037d3e8cf0830b879c9a3fc6d7617a15820001bc2800318192309b6cbf9bd72ace1e75cfeecb07ff0aab89b7f51a77c75841a05f5e100",
                    "82825820ca6d9e3d0eb8f82d389e061e035132b2f83523bc6c571265dc4efdabddad512001825839003a5cbc099211950e25f9877ab7abd63d056d515bd64823a5de83dc2653daab1c1256d159150ed263bec087e6a6109b465b0150e9e3bb4269821a0013a9f2a1581ce65559518eef9ebc25d3bacfa3f037d3e8cf0830b879c9a3fc6d7617a15820001bc280031cfd72bbf3ac98873abc8abdf0ba058d23327a43292c39fb8a41db1a05e69ec0",
                    "82825820a3ac150f1f9f7991d81d21a1e4ea467897e9c94214878f70ef277c79e82ac3c401825839003a5cbc099211950e25f9877ab7abd63d056d515bd64823a5de83dc2653daab1c1256d159150ed263bec087e6a6109b465b0150e9e3bb4269821a0013a9f2a1581ce65559518eef9ebc25d3bacfa3f037d3e8cf0830b879c9a3fc6d7617a15820001bc2800089af82e9c7f11645cc671dc366a36aff905f03319d624dcc62201a1a05e69ec0",
                    "828258201f114195e7f557e6ea04938d91611db8b5fe2174a5ca5f544050f5790c2cee2901825839003a5cbc099211950e25f9877ab7abd63d056d515bd64823a5de83dc2653daab1c1256d159150ed263bec087e6a6109b465b0150e9e3bb4269821a0013a9f2a1581ce65559518eef9ebc25d3bacfa3f037d3e8cf0830b879c9a3fc6d7617a15820001bc28000f88b4548fd9422ddaac180c47c6e8c26d076a7f2ba5c6fbae9b3ea1a055d4a80",
                    "828258201725f7176191da6e165e56bbcd350e1dc61f35e9fa68b850745dbbc744240ba701825839003a5cbc099211950e25f9877ab7abd63d056d515bd64823a5de83dc2653daab1c1256d159150ed263bec087e6a6109b465b0150e9e3bb4269821a0013a9f2a1581ce65559518eef9ebc25d3bacfa3f037d3e8cf0830b879c9a3fc6d7617a15820001bc28002f7f7802b27301d6497ad5532c9ad1f536b3dfdb62ae78e5177b26a1a02faf080",
                    "82825820bcc2a8528b89669dcb7821da3ab192031401edea81c53c8bcdc115df995c08bd01825839003a5cbc099211950e25f9877ab7abd63d056d515bd64823a5de83dc2653daab1c1256d159150ed263bec087e6a6109b465b0150e9e3bb4269821a0013a9f2a1581ce65559518eef9ebc25d3bacfa3f037d3e8cf0830b879c9a3fc6d7617a15820001bc2800231cf9da9fec9977ad881794ab1dcb0770a4fda82ad7e3f2e621b711a05f5e100",
                    "8282582020e0a1e44989b21d354eff54e98ee30880fd596c6ccd06498253717b920e617d01825839003a5cbc099211950e25f9877ab7abd63d056d515bd64823a5de83dc2653daab1c1256d159150ed263bec087e6a6109b465b0150e9e3bb4269821a0013a9f2a1581ce65559518eef9ebc25d3bacfa3f037d3e8cf0830b879c9a3fc6d7617a15820001bc280037fc71d6cf5c0601c867316f0584a5cebe9288bc37a8def20688fb81a05f5e100",
                    "828258209814a9e199ac5fdd643c64be24cb96bb1a563f827a6b699c9349a21a2efb428101825839003a5cbc099211950e25f9877ab7abd63d056d515bd64823a5de83dc2653daab1c1256d159150ed263bec087e6a6109b465b0150e9e3bb4269821a0013a9f2a1581ce65559518eef9ebc25d3bacfa3f037d3e8cf0830b879c9a3fc6d7617a15820001bc2800047f5c9671760f3c337d4c0a94c0bb1ca0eb474984c30eab072388e1a05f5e100",
                    "82825820d5c685dc4d4be75babb17df5f056f78ae9060888a26c53a7cfacf8c87c50ad6f01825839003a5cbc099211950e25f9877ab7abd63d056d515bd64823a5de83dc2653daab1c1256d159150ed263bec087e6a6109b465b0150e9e3bb4269821b0000000e1073cd81a4581c698a6ea0ca99f315034072af31eaac6ec11fe8558d3f48e9775aab9da14574445249501b00000019e395113f581c70ff8215aa4bb27ca663de5958569caafc8764c21b16932e9b16b9a6a151706572736f6e616c54657374546f6b656e1a000f4240581c74946c67d2a6afbdfd9450eb9818f202ba26143f821990d7a45b515ca55144726970707932333034313935323234330151447269707079323330343139383031333701514472697070793233303431393830313837015144726970707932333034313938303138380151447269707079323330343139383031383901581c9eeb92a94656b651cbd86e2a913026cd5c511632e4b6f98a9da16367a14974657374546f6b656e1a000f4240",
                    "82825820857c0aff7d67b85058bc01326db1f284d9b7d8d78503535f2638641fb3079b0001825839003a5cbc099211950e25f9877ab7abd63d056d515bd64823a5de83dc2653daab1c1256d159150ed263bec087e6a6109b465b0150e9e3bb42691aeede5f94"
                )
            )
        }
        assertThat(response.status).isEqualTo(HttpStatusCode.OK)
        val body: List<WalletSong> = response.body()
        assertThat(body.size).isEqualTo(9)
    }
}

fun addSongToDatabase(
    nftPolicy: String,
    nftName: String,
    offset: Int = 0,
    ownerId: UUID? = null,
    archived: Boolean = false,
    phrase: String? = null,
    init: (SongEntity.() -> Unit)? = null
): Song {
    val ownerEntityId = ownerId?.let {
        EntityID(it, UserTable)
    } ?: transaction {
        UserEntity.new {
            email = "artist$offset@newm.io"
        }
    }.id

    val paymentKeyId = transaction {
        KeyEntity.new {
            this.address = ""
            this.vkey = ""
            this.skey = ""
        }
    }.id

    fun phraseOrBlank(offset: Int, target: Int) = phrase?.takeIf { offset % 4 == target }.orEmpty()

    return transaction {
        SongEntity.new {
            this.archived = archived
            this.ownerId = ownerEntityId
            title = "title$offset ${phraseOrBlank(offset, 0)} blah blah"
            description = "description$offset ${phraseOrBlank(offset, 1)} blah blah"
            album = "album$offset ${phraseOrBlank(offset, 2)} blah blah"
            this.nftName = nftName
            genres = arrayOf("genre${offset}_0", "genre${offset}_1")
            moods = arrayOf("mood${offset}_0", "mood${offset}_1")
            coverArtUrl = "https://newm.io/cover$offset"
            track = offset
            language = "language$offset"
            compositionCopyrightOwner = "compositionCopyrightOwner$offset"
            compositionCopyrightYear = offset
            phonographicCopyrightOwner = "copyright$phonographicCopyrightOwner"
            phonographicCopyrightYear = 2 * offset
            parentalAdvisory = "parentalAdvisory$offset"
            barcodeType = SongBarcodeType.values()[offset % SongBarcodeType.values().size]
            barcodeNumber = "barcodeNumber$offset"
            isrc = "isrc$offset"
            iswc = "iswc$offset"
            ipis = arrayOf("ipi${offset}_0", "ipi${offset}_1")
            releaseDate = LocalDate.of(2023, 1, offset % 31 + 1)
            publicationDate = LocalDate.of(2023, 1, offset % 31 + 1)
            lyricsUrl = "https://newm.io/lyrics$offset"
            tokenAgreementUrl = "https://newm.io/agreement$offset"
            originalAudioUrl = "https://newm.io/audio$offset"
            clipUrl = "https://newm.io/clip$offset"
            streamUrl = "https://newm.io/stream$offset"
            duration = offset
            nftPolicyId = nftPolicy
            audioEncodingStatus = AudioEncodingStatus.values()[offset % AudioEncodingStatus.values().size]
            mintingStatus = MintingStatus.values()[offset % MintingStatus.values().size]
            marketplaceStatus = MarketplaceStatus.values()[offset % MarketplaceStatus.values().size]
            this.paymentKeyId = paymentKeyId
            if (init != null) {
                this.apply { init() }
            }
        }
    }.toModel()
}
