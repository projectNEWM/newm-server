package io.newm.server.features.marketplace

import com.google.common.truth.Truth.assertThat
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.mockk.coEvery
import io.mockk.mockk
import io.newm.server.BaseApplicationTests
import io.newm.server.config.repo.ConfigRepository
import io.newm.server.config.repo.ConfigRepository.Companion.CONFIG_KEY_NFTCDN_ENABLED
import io.newm.server.features.cardano.repo.CardanoRepository
import io.newm.server.features.collaboration.database.CollaborationEntity
import io.newm.server.features.collaboration.database.CollaborationTable
import io.newm.server.features.marketplace.database.MarketplaceArtistEntity
import io.newm.server.features.marketplace.database.MarketplaceSaleEntity
import io.newm.server.features.marketplace.database.MarketplaceSaleTable
import io.newm.server.features.marketplace.model.Artist
import io.newm.server.features.marketplace.model.Sale
import io.newm.server.features.marketplace.model.SaleStatus
import io.newm.server.features.model.CountResponse
import io.newm.server.features.song.database.ReleaseEntity
import io.newm.server.features.song.database.ReleaseTable
import io.newm.server.features.song.database.SongEntity
import io.newm.server.features.song.database.SongTable
import io.newm.server.features.song.model.MintingStatus
import io.newm.server.features.song.model.ReleaseType
import io.newm.server.features.user.database.UserEntity
import io.newm.server.features.user.database.UserTable
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.SqlExpressionBuilder.neq
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.koin.core.context.GlobalContext.loadKoinModules
import org.koin.dsl.module
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

private const val COST_TOKEN_USD_PRICE = 5000L

class MarketplaceRoutesTests : BaseApplicationTests() {
    @BeforeAll
    fun beforeAllMarketplaceRoutesTests() {
        loadKoinModules(
            module {
                single {
                    mockk<CardanoRepository>(relaxed = true) {
                        coEvery { isMainnet() } returns false
                        coEvery { queryNativeTokenUSDPrice(any(), any()) } returns COST_TOKEN_USD_PRICE
                    }
                }
                single {
                    mockk<ConfigRepository>(relaxed = true) {
                        coEvery { getBoolean(CONFIG_KEY_NFTCDN_ENABLED) } returns false
                    }
                }
            }
        )
    }

    @BeforeEach
    fun beforeEach() {
        transaction {
            MarketplaceSaleTable.deleteAll()
            CollaborationTable.deleteAll()
            SongTable.deleteAll()
            ReleaseTable.deleteAll()
            UserTable.deleteWhere { email neq testUserEmail }
        }
    }

    @Test
    fun testGetSale() =
        runBlocking {
            val sale = addSaleToDatabase()
            val response =
                client.get("v1/marketplace/sales/${sale.id}") {
                    accept(ContentType.Application.Json)
                }
            assertThat(response.status).isEqualTo(HttpStatusCode.OK)
            assertThat(response.body<Sale>()).isEqualTo(sale)
        }

    @Test
    fun testGetAllSales() =
        runBlocking {
            val expectedSales = mutableListOf<Sale>()
            for (offset in 0..30) {
                expectedSales += addSaleToDatabase(offset)
            }

            // Get all sales forcing pagination
            var offset = 0
            val limit = 5
            val actualSales = mutableListOf<Sale>()
            while (true) {
                val response =
                    client.get("v1/marketplace/sales") {
                        accept(ContentType.Application.Json)
                        parameter("offset", offset)
                        parameter("limit", limit)
                    }
                assertThat(response.status).isEqualTo(HttpStatusCode.OK)
                val sales = response.body<List<Sale>>()
                if (sales.isEmpty()) break
                actualSales += sales
                offset += limit
            }
            assertThat(actualSales).isEqualTo(expectedSales)
        }

    @Test
    fun testGetAllSalesInDescendingOrder() =
        runBlocking {
            val allSales = mutableListOf<Sale>()
            for (offset in 0..30) {
                allSales += addSaleToDatabase(offset)
            }
            val expectedSales = allSales.sortedByDescending { it.createdAt }

            // Get all sales forcing pagination
            var offset = 0
            val limit = 5
            val actualSales = mutableListOf<Sale>()
            while (true) {
                val response =
                    client.get("v1/marketplace/sales") {
                        accept(ContentType.Application.Json)
                        parameter("offset", offset)
                        parameter("limit", limit)
                        parameter("sortOrder", "desc")
                    }
                assertThat(response.status).isEqualTo(HttpStatusCode.OK)
                val sales = response.body<List<Sale>>()
                if (sales.isEmpty()) break
                actualSales += sales
                offset += limit
            }
            assertThat(actualSales).isEqualTo(expectedSales)
        }

    @Test
    fun testGetSalesById() =
        runBlocking {
            val allSales = mutableListOf<Sale>()
            for (offset in 0..30) {
                allSales += addSaleToDatabase(offset)
            }

            // filter out 1st and last
            val expectedSales = allSales.subList(1, allSales.size - 1)
            val ids = expectedSales.joinToString { it.id.toString() }

            // Get sales forcing pagination
            var offset = 0
            val limit = 5
            val actualSales = mutableListOf<Sale>()
            while (true) {
                val response =
                    client.get("v1/marketplace/sales") {
                        accept(ContentType.Application.Json)
                        parameter("offset", offset)
                        parameter("limit", limit)
                        parameter("ids", ids)
                    }
                assertThat(response.status).isEqualTo(HttpStatusCode.OK)
                val sales = response.body<List<Sale>>()
                if (sales.isEmpty()) break
                actualSales += sales
                offset += limit
            }
            assertThat(actualSales).isEqualTo(expectedSales)
        }

    @Test
    fun testGetSalesByIdExclusion() =
        runBlocking {
            val allSales = mutableListOf<Sale>()
            for (offset in 0..30) {
                allSales += addSaleToDatabase(offset)
            }

            // filter out 1st and last
            val expectedSales = allSales.subList(1, allSales.size - 1)
            val ids = allSales.filter { it !in expectedSales }.joinToString { "-${it.id}" }

            // Get sales forcing pagination
            var offset = 0
            val limit = 5
            val actualSales = mutableListOf<Sale>()
            while (true) {
                val response =
                    client.get("v1/marketplace/sales") {
                        accept(ContentType.Application.Json)
                        parameter("offset", offset)
                        parameter("limit", limit)
                        parameter("ids", ids)
                    }
                assertThat(response.status).isEqualTo(HttpStatusCode.OK)
                val sales = response.body<List<Sale>>()
                if (sales.isEmpty()) break
                actualSales += sales
                offset += limit
            }
            assertThat(actualSales).isEqualTo(expectedSales)
        }

    @Test
    fun testGetSalesBySongId() =
        runBlocking {
            val allSales = mutableListOf<Sale>()
            for (offset in 0..30) {
                allSales += addSaleToDatabase(offset)
            }

            // filter out 1st and last
            val expectedSales = allSales.subList(1, allSales.size - 1)
            val songIds = expectedSales.joinToString { it.song!!.id.toString() }

            // Get sales forcing pagination
            var offset = 0
            val limit = 5
            val actualSales = mutableListOf<Sale>()
            while (true) {
                val response =
                    client.get("v1/marketplace/sales") {
                        accept(ContentType.Application.Json)
                        parameter("offset", offset)
                        parameter("limit", limit)
                        parameter("songIds", songIds)
                    }
                assertThat(response.status).isEqualTo(HttpStatusCode.OK)
                val sales = response.body<List<Sale>>()
                if (sales.isEmpty()) break
                actualSales += sales
                offset += limit
            }
            assertThat(actualSales).isEqualTo(expectedSales)
        }

    @Test
    fun testGetSalesBySongIdExclusion() =
        runBlocking {
            val allSales = mutableListOf<Sale>()
            for (offset in 0..30) {
                allSales += addSaleToDatabase(offset)
            }

            // filter out 1st and last
            val expectedSales = allSales.subList(1, allSales.size - 1)
            val songIds = allSales.filter { it !in expectedSales }.joinToString { "-${it.song!!.id}" }

            // Get sales forcing pagination
            var offset = 0
            val limit = 5
            val actualSales = mutableListOf<Sale>()
            while (true) {
                val response =
                    client.get("v1/marketplace/sales") {
                        accept(ContentType.Application.Json)
                        parameter("offset", offset)
                        parameter("limit", limit)
                        parameter("songIds", songIds)
                    }
                assertThat(response.status).isEqualTo(HttpStatusCode.OK)
                val sales = response.body<List<Sale>>()
                if (sales.isEmpty()) break
                actualSales += sales
                offset += limit
            }
            assertThat(actualSales).isEqualTo(expectedSales)
        }

    @Test
    fun testGetSalesByArtistId() =
        runBlocking {
            val allSales = mutableListOf<Sale>()
            for (offset in 0..30) {
                allSales += addSaleToDatabase(offset)
            }

            // filter out 1st and last
            val expectedSales = allSales.subList(1, allSales.size - 1)
            val artistIds = expectedSales.joinToString { it.song!!.artistId.toString() }

            // Get sales forcing pagination
            var offset = 0
            val limit = 5
            val actualSales = mutableListOf<Sale>()
            while (true) {
                val response =
                    client.get("v1/marketplace/sales") {
                        accept(ContentType.Application.Json)
                        parameter("offset", offset)
                        parameter("limit", limit)
                        parameter("artistIds", artistIds)
                    }
                assertThat(response.status).isEqualTo(HttpStatusCode.OK)
                val sales = response.body<List<Sale>>()
                if (sales.isEmpty()) break
                actualSales += sales
                offset += limit
            }
            assertThat(actualSales).isEqualTo(expectedSales)
        }

    @Test
    fun testGetSalesByArtistIdExclusion() =
        runBlocking {
            val allSales = mutableListOf<Sale>()
            for (offset in 0..30) {
                allSales += addSaleToDatabase(offset)
            }

            // filter out 1st and last
            val expectedSales = allSales.subList(1, allSales.size - 1)
            val artistIds = allSales.filter { it !in expectedSales }.joinToString { "-${it.song!!.artistId}" }

            // Get sales forcing pagination
            var offset = 0
            val limit = 5
            val actualSales = mutableListOf<Sale>()
            while (true) {
                val response =
                    client.get("v1/marketplace/sales") {
                        accept(ContentType.Application.Json)
                        parameter("offset", offset)
                        parameter("limit", limit)
                        parameter("artistIds", artistIds)
                    }
                assertThat(response.status).isEqualTo(HttpStatusCode.OK)
                val sales = response.body<List<Sale>>()
                if (sales.isEmpty()) break
                actualSales += sales
                offset += limit
            }
            assertThat(actualSales).isEqualTo(expectedSales)
        }

    @Test
    fun testGetSalesByStatus() =
        runBlocking {
            val allSales = mutableListOf<Sale>()
            for (offset in 0..30) {
                allSales += addSaleToDatabase(offset)
            }

            for (expectedSaleStatus in SaleStatus.entries) {
                // filter out
                val expectedSales = allSales.filter { it.status == expectedSaleStatus }

                // Get sales forcing pagination
                var offset = 0
                val limit = 5
                val actualSales = mutableListOf<Sale>()
                while (true) {
                    val response =
                        client.get("v1/marketplace/sales") {
                            accept(ContentType.Application.Json)
                            parameter("offset", offset)
                            parameter("limit", limit)
                            parameter("saleStatuses", expectedSaleStatus)
                        }
                    assertThat(response.status).isEqualTo(HttpStatusCode.OK)
                    val sales = response.body<List<Sale>>()
                    if (sales.isEmpty()) break
                    actualSales += sales
                    offset += limit
                }
                assertThat(actualSales).isEqualTo(expectedSales)
            }
        }

    @Test
    fun testGetSalesByStatusExclusion() =
        runBlocking {
            val allSales = mutableListOf<Sale>()
            for (offset in 0..30) {
                allSales += addSaleToDatabase(offset)
            }

            for (expectedSaleStatus in SaleStatus.entries) {
                // filter out
                val expectedSales = allSales.filter { it.status != expectedSaleStatus }

                // Get sales forcing pagination
                var offset = 0
                val limit = 5
                val actualSales = mutableListOf<Sale>()
                while (true) {
                    val response =
                        client.get("v1/marketplace/sales") {
                            accept(ContentType.Application.Json)
                            parameter("offset", offset)
                            parameter("limit", limit)
                            parameter("saleStatuses", "-$expectedSaleStatus")
                        }
                    assertThat(response.status).isEqualTo(HttpStatusCode.OK)
                    val sales = response.body<List<Sale>>()
                    if (sales.isEmpty()) break
                    actualSales += sales
                    offset += limit
                }
                assertThat(actualSales).isEqualTo(expectedSales)
            }
        }

    @Test
    fun testGetSalesByGenres() =
        runBlocking {
            val allSales = mutableListOf<Sale>()
            for (offset in 0..30) {
                allSales += addSaleToDatabase(offset)
            }

            // filter out 1st and last and take only 1st genre of each
            val expectedSales = allSales.subList(1, allSales.size - 1)
            val genres = expectedSales.joinToString { it.song?.genres!!.first() }

            // Get sales forcing pagination
            var offset = 0
            val limit = 5
            val actualSales = mutableListOf<Sale>()
            while (true) {
                val response =
                    client.get("v1/marketplace/sales") {
                        accept(ContentType.Application.Json)
                        parameter("offset", offset)
                        parameter("limit", limit)
                        parameter("genres", genres)
                    }
                assertThat(response.status).isEqualTo(HttpStatusCode.OK)
                val sales = response.body<List<Sale>>()
                if (sales.isEmpty()) break
                actualSales += sales
                offset += limit
            }
            assertThat(actualSales).isEqualTo(expectedSales)
        }

    @Test
    fun testGetSalesByGenresExclusion() =
        runBlocking {
            val allSales = mutableListOf<Sale>()
            for (offset in 0..30) {
                allSales += addSaleToDatabase(offset)
            }

            // filter out 1st and last and take only 1st genre of each
            val expectedSales = allSales.subList(1, allSales.size - 1)
            val genres = allSales.filter { it !in expectedSales }.joinToString { "-${it.song!!.genres!!.first()}" }

            // Get sales forcing pagination
            var offset = 0
            val limit = 5
            val actualSales = mutableListOf<Sale>()
            while (true) {
                val response =
                    client.get("v1/marketplace/sales") {
                        accept(ContentType.Application.Json)
                        parameter("offset", offset)
                        parameter("limit", limit)
                        parameter("genres", genres)
                    }
                assertThat(response.status).isEqualTo(HttpStatusCode.OK)
                val sales = response.body<List<Sale>>()
                if (sales.isEmpty()) break
                actualSales += sales
                offset += limit
            }
            assertThat(actualSales).isEqualTo(expectedSales)
        }

    @Test
    fun testGetSalesByMoods() =
        runBlocking {
            val allSales = mutableListOf<Sale>()
            for (offset in 0..30) {
                allSales += addSaleToDatabase(offset)
            }

            // filter out 1st and last and take only 1st mood of each
            val expectedSales = allSales.subList(1, allSales.size - 1)
            val moods = expectedSales.joinToString { it.song?.moods!!.first() }

            // Get sales forcing pagination
            var offset = 0
            val limit = 5
            val actualSales = mutableListOf<Sale>()
            while (true) {
                val response =
                    client.get("v1/marketplace/sales") {
                        accept(ContentType.Application.Json)
                        parameter("offset", offset)
                        parameter("limit", limit)
                        parameter("moods", moods)
                    }
                assertThat(response.status).isEqualTo(HttpStatusCode.OK)
                val sales = response.body<List<Sale>>()
                if (sales.isEmpty()) break
                actualSales += sales
                offset += limit
            }
            assertThat(actualSales).isEqualTo(expectedSales)
        }

    @Test
    fun testGetSalesByMoodsExclusion() =
        runBlocking {
            val allSales = mutableListOf<Sale>()
            for (offset in 0..30) {
                allSales += addSaleToDatabase(offset)
            }

            // filter out 1st and last and take only 1st mood of each
            val expectedSales = allSales.subList(1, allSales.size - 1)
            val moods = allSales.filter { it !in expectedSales }.joinToString { "-${it.song!!.moods!!.first()}" }

            // Get sales forcing pagination
            var offset = 0
            val limit = 5
            val actualSales = mutableListOf<Sale>()
            while (true) {
                val response =
                    client.get("v1/marketplace/sales") {
                        accept(ContentType.Application.Json)
                        parameter("offset", offset)
                        parameter("limit", limit)
                        parameter("moods", moods)
                    }
                assertThat(response.status).isEqualTo(HttpStatusCode.OK)
                val sales = response.body<List<Sale>>()
                if (sales.isEmpty()) break
                actualSales += sales
                offset += limit
            }
            assertThat(actualSales).isEqualTo(expectedSales)
        }

    @Test
    fun testGetSalesByOlderThan() =
        runBlocking {
            val allSales = mutableListOf<Sale>()
            for (offset in 0..30) {
                allSales += addSaleToDatabase(offset)
            }

            // filter out newest one
            val expectedSales = allSales.subList(0, allSales.size - 1)
            val olderThan = allSales.last().createdAt

            // Get sales forcing pagination
            var offset = 0
            val limit = 5
            val actualSales = mutableListOf<Sale>()
            while (true) {
                val response =
                    client.get("v1/marketplace/sales") {
                        accept(ContentType.Application.Json)
                        parameter("offset", offset)
                        parameter("limit", limit)
                        parameter("olderThan", olderThan)
                    }
                assertThat(response.status).isEqualTo(HttpStatusCode.OK)
                val sales = response.body<List<Sale>>()
                if (sales.isEmpty()) break
                actualSales += sales
                offset += limit
            }
            assertThat(actualSales).isEqualTo(expectedSales)
        }

    @Test
    fun testGetSalesByNewerThan() =
        runBlocking {
            val allSales = mutableListOf<Sale>()
            for (offset in 0..30) {
                allSales += addSaleToDatabase(offset)
            }

            // filter out oldest one
            val expectedSales = allSales.subList(1, allSales.size)
            val newerThan = allSales.first().createdAt

            // Get sales forcing pagination
            var offset = 0
            val limit = 5
            val actualSales = mutableListOf<Sale>()
            while (true) {
                val response =
                    client.get("v1/marketplace/sales") {
                        accept(ContentType.Application.Json)
                        parameter("offset", offset)
                        parameter("limit", limit)
                        parameter("newerThan", newerThan)
                    }
                assertThat(response.status).isEqualTo(HttpStatusCode.OK)
                val sales = response.body<List<Sale>>()
                if (sales.isEmpty()) break
                actualSales += sales
                offset += limit
            }
            assertThat(actualSales).isEqualTo(expectedSales)
        }

    @Test
    fun testGetSalesByPhrase() =
        runBlocking {
            val phrase = "ABCDE"
            val allSales = mutableListOf<Sale>()
            for (offset in 0..30) {
                allSales += addSaleToDatabase(offset, phrase = phrase.takeIf { offset % 2 == 0 })
            }

            // filter out for phrase
            val expectedSales =
                allSales.filter { sale ->
                    val stageOrFullName = transaction { UserEntity[sale.song!!.artistId].stageOrFullName }
                    phrase in sale.song?.title!! || phrase in stageOrFullName
                }

            // Get sales forcing pagination
            var offset = 0
            val limit = 5
            val actualSales = mutableListOf<Sale>()
            while (true) {
                val response =
                    client.get("v1/marketplace/sales") {
                        accept(ContentType.Application.Json)
                        parameter("offset", offset)
                        parameter("limit", limit)
                        parameter("phrase", phrase)
                    }
                assertThat(response.status).isEqualTo(HttpStatusCode.OK)
                val sales = response.body<List<Sale>>()
                if (sales.isEmpty()) break
                actualSales += sales
                offset += limit
            }
            assertThat(actualSales).isEqualTo(expectedSales)
        }

    @Test
    fun testGetSaleCount() =
        runBlocking {
            var count = 0L
            while (true) {
                val response =
                    client.get("v1/marketplace/sales/count") {
                        accept(ContentType.Application.Json)
                    }
                assertThat(response.status).isEqualTo(HttpStatusCode.OK)
                val actualCount = response.body<CountResponse>().count
                assertThat(actualCount).isEqualTo(count)

                if (++count == 10L) break

                addSaleToDatabase(count.toInt())
            }
        }

    @Test
    fun testGetArtist() =
        runBlocking {
            val artist = addArtistToDatabase()
            val response =
                client.get("v1/marketplace/artists/${artist.id}") {
                    accept(ContentType.Application.Json)
                }
            assertThat(response.status).isEqualTo(HttpStatusCode.OK)
            assertThat(response.body<Artist>()).isEqualTo(artist)
        }

    @Test
    fun testGetAllArtists() =
        runBlocking {
            val expectedArtists = mutableListOf<Artist>()
            for (offset in 0..30) {
                expectedArtists += addArtistToDatabase(offset)
            }

            // Get all sales forcing pagination
            var offset = 0
            val limit = 5
            val actualArtists = mutableListOf<Artist>()
            while (true) {
                val response =
                    client.get("v1/marketplace/artists") {
                        accept(ContentType.Application.Json)
                        parameter("offset", offset)
                        parameter("limit", limit)
                    }
                assertThat(response.status).isEqualTo(HttpStatusCode.OK)
                val artists = response.body<List<Artist>>()
                if (artists.isEmpty()) break
                actualArtists += artists
                offset += limit
            }
            assertThat(actualArtists).isEqualTo(expectedArtists)
            println(actualArtists)
        }

    @Test
    fun testGetAllArtistsInDescendingOrder() =
        runBlocking {
            val allArtists = mutableListOf<Artist>()
            for (offset in 0..30) {
                allArtists += addArtistToDatabase(offset)
            }
            val expectedArtists = allArtists.sortedByDescending { it.createdAt }

            // Get all artists forcing pagination
            var offset = 0
            val limit = 5
            val actualArtists = mutableListOf<Artist>()
            while (true) {
                val response =
                    client.get("v1/marketplace/artists") {
                        accept(ContentType.Application.Json)
                        parameter("offset", offset)
                        parameter("limit", limit)
                        parameter("sortOrder", "desc")
                    }
                assertThat(response.status).isEqualTo(HttpStatusCode.OK)
                val artists = response.body<List<Artist>>()
                if (artists.isEmpty()) break
                actualArtists += artists
                offset += limit
            }
            assertThat(actualArtists).isEqualTo(expectedArtists)
        }

    @Test
    fun testGetArtistsById() =
        runBlocking {
            val allArtists = mutableListOf<Artist>()
            for (offset in 0..30) {
                allArtists += addArtistToDatabase(offset)
            }

            // filter out 1st and last
            val expectedArtists = allArtists.subList(1, allArtists.size - 1)
            val ids = expectedArtists.joinToString { it.id.toString() }

            // Get artists forcing pagination
            var offset = 0
            val limit = 5
            val actualArtists = mutableListOf<Artist>()
            while (true) {
                val response =
                    client.get("v1/marketplace/artists") {
                        accept(ContentType.Application.Json)
                        parameter("offset", offset)
                        parameter("limit", limit)
                        parameter("ids", ids)
                    }
                assertThat(response.status).isEqualTo(HttpStatusCode.OK)
                val artists = response.body<List<Artist>>()
                if (artists.isEmpty()) break
                actualArtists += artists
                offset += limit
            }
            assertThat(actualArtists).isEqualTo(expectedArtists)
        }

    @Test
    fun testGetArtistsByIdExclusion() =
        runBlocking {
            val allArtists = mutableListOf<Artist>()
            for (offset in 0..30) {
                allArtists += addArtistToDatabase(offset)
            }

            // filter out 1st and last
            val expectedArtists = allArtists.subList(1, allArtists.size - 1)
            val ids = allArtists.filter { it !in expectedArtists }.joinToString { "-${it.id}" }

            // Get artists forcing pagination
            var offset = 0
            val limit = 5
            val actualArtists = mutableListOf<Artist>()
            while (true) {
                val response =
                    client.get("v1/marketplace/artists") {
                        accept(ContentType.Application.Json)
                        parameter("offset", offset)
                        parameter("limit", limit)
                        parameter("ids", ids)
                    }
                assertThat(response.status).isEqualTo(HttpStatusCode.OK)
                val artists = response.body<List<Artist>>()
                if (artists.isEmpty()) break
                actualArtists += artists
                offset += limit
            }
            assertThat(actualArtists).isEqualTo(expectedArtists)
        }

    @Test
    fun testGetArtistsByGenres() =
        runBlocking {
            val allArtists = mutableListOf<Artist>()
            for (offset in 0..30) {
                allArtists += addArtistToDatabase(offset)
            }

            // filter out 1st and last and take only 1st genre of each
            val expectedArtists = allArtists.subList(1, allArtists.size - 1)
            val genres = expectedArtists.joinToString { it.genre!! }

            // Get artists forcing pagination
            var offset = 0
            val limit = 5
            val actualArtists = mutableListOf<Artist>()
            while (true) {
                val response =
                    client.get("v1/marketplace/artists") {
                        accept(ContentType.Application.Json)
                        parameter("offset", offset)
                        parameter("limit", limit)
                        parameter("genres", genres)
                    }
                assertThat(response.status).isEqualTo(HttpStatusCode.OK)
                val artists = response.body<List<Artist>>()
                if (artists.isEmpty()) break
                actualArtists += artists
                offset += limit
            }
            assertThat(actualArtists).isEqualTo(expectedArtists)
        }

    @Test
    fun testGetArtistsByGenresExclusion() =
        runBlocking {
            val allArtists = mutableListOf<Artist>()
            for (offset in 0..30) {
                allArtists += addArtistToDatabase(offset)
            }

            // filter out 1st and last and take only 1st genre of each
            val expectedArtists = allArtists.subList(1, allArtists.size - 1)
            val genres = allArtists.filter { it !in expectedArtists }.joinToString { "-${it.genre}" }

            // Get artists forcing pagination
            var offset = 0
            val limit = 5
            val actualArtists = mutableListOf<Artist>()
            while (true) {
                val response =
                    client.get("v1/marketplace/artists") {
                        accept(ContentType.Application.Json)
                        parameter("offset", offset)
                        parameter("limit", limit)
                        parameter("genres", genres)
                    }
                assertThat(response.status).isEqualTo(HttpStatusCode.OK)
                val artists = response.body<List<Artist>>()
                if (artists.isEmpty()) break
                actualArtists += artists
                offset += limit
            }
            assertThat(actualArtists).isEqualTo(expectedArtists)
        }

    @Test
    fun testGetArtistsByOlderThan() =
        runBlocking {
            val allArtists = mutableListOf<Artist>()
            for (offset in 0..30) {
                allArtists += addArtistToDatabase(offset)
            }

            // filter out newest one
            val expectedArtists = allArtists.subList(0, allArtists.size - 1)
            val olderThan = allArtists.last().createdAt

            // Get artists forcing pagination
            var offset = 0
            val limit = 5
            val actualArtists = mutableListOf<Artist>()
            while (true) {
                val response =
                    client.get("v1/marketplace/artists") {
                        accept(ContentType.Application.Json)
                        parameter("offset", offset)
                        parameter("limit", limit)
                        parameter("olderThan", olderThan)
                    }
                assertThat(response.status).isEqualTo(HttpStatusCode.OK)
                val artists = response.body<List<Artist>>()
                if (artists.isEmpty()) break
                actualArtists += artists
                offset += limit
            }
            assertThat(actualArtists).isEqualTo(expectedArtists)
        }

    @Test
    fun testGetArtistsByNewerThan() =
        runBlocking {
            val allArtists = mutableListOf<Artist>()
            for (offset in 0..30) {
                allArtists += addArtistToDatabase(offset)
            }

            // filter out oldest one
            val expectedArtists = allArtists.subList(1, allArtists.size)
            val newerThan = allArtists.first().createdAt

            // Get artists forcing pagination
            var offset = 0
            val limit = 5
            val actualArtists = mutableListOf<Artist>()
            while (true) {
                val response =
                    client.get("v1/marketplace/artists") {
                        accept(ContentType.Application.Json)
                        parameter("offset", offset)
                        parameter("limit", limit)
                        parameter("newerThan", newerThan)
                    }
                assertThat(response.status).isEqualTo(HttpStatusCode.OK)
                val artists = response.body<List<Artist>>()
                if (artists.isEmpty()) break
                actualArtists += artists
                offset += limit
            }
            assertThat(actualArtists).isEqualTo(expectedArtists)
        }

    @Test
    fun testGetArtistCount() =
        runBlocking {
            var count = 0L
            while (true) {
                val response =
                    client.get("v1/marketplace/artists/count") {
                        accept(ContentType.Application.Json)
                    }
                assertThat(response.status).isEqualTo(HttpStatusCode.OK)
                val actualCount = response.body<CountResponse>().count
                assertThat(actualCount).isEqualTo(count)

                if (++count == 10L) break

                addArtistToDatabase(count.toInt())
            }
        }

    private fun addSaleToDatabase(
        offset: Int = 0,
        phrase: String? = null,
    ): Sale {
        fun phraseOrEmpty(target: Int) = phrase?.takeIf { offset % 4 == target }.orEmpty()

        val artist =
            transaction {
                UserEntity.new {
                    email = "artist$offset@newm.io"
                    if (offset % 4 == 0) {
                        nickname = "nickname$offset ${phraseOrEmpty(0)} blah blah"
                    }
                    firstName = "firstName$offset ${phraseOrEmpty(1)} blah blah"
                    lastName = "lastName$offset ${phraseOrEmpty(2)} blah blah"
                    pictureUrl = "pictureUrl$offset"
                }
            }

        val song =
            transaction {
                val title = "title$offset ${phraseOrEmpty(3)} blah blah"
                val releaseId =
                    ReleaseEntity
                        .new {
                            ownerId = artist.id
                            this.title = title
                            arweaveCoverArtUrl = "ar://coverArtUrl$offset"
                            releaseType = ReleaseType.SINGLE
                        }.id.value
                SongEntity.new {
                    ownerId = artist.id
                    this.title = title
                    description = "description$offset"
                    parentalAdvisory = "parentalAdvisory$offset"
                    this.releaseId = EntityID(releaseId, ReleaseTable)
                    genres = arrayOf("genre${offset}_0", "genre${offset}_1")
                    moods = arrayOf("mood${offset}_0", "mood${offset}_1")
                    arweaveClipUrl = "ar://clipUrl$offset"
                    arweaveTokenAgreementUrl = "ar://tokenAgreementUrl$offset"
                    nftPolicyId = "nftPolicyId$offset"
                    nftName = "nftName$offset"
                }
            }

        transaction {
            CollaborationEntity.new {
                songId = song.id
                email = artist.email
                role = "Artist"
                royaltyRate = 100f
            }
        }

        return transaction {
            MarketplaceSaleEntity
                .new {
                    createdAt = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS)
                    status = SaleStatus.entries[offset % SaleStatus.entries.size]
                    songId = song.id
                    ownerAddress = "ownerAddress$offset"
                    pointerPolicyId = "pointerPolicyId$offset"
                    pointerAssetName = "pointerAssetName$offset"
                    bundlePolicyId = "bundlePolicyId$offset"
                    bundleAssetName = "bundleAssetName$offset"
                    bundleAmount = offset + 1L
                    costPolicyId = "costPolicyId$offset"
                    costAssetName = "costAssetName$offset"
                    costAmount = offset.toLong()
                    maxBundleSize = offset + 100L
                    totalBundleQuantity = offset + 1000L
                    availableBundleQuantity = offset + 1000L
                }.toModel(
                    isMainnet = false,
                    isNftCdnEnabled = false,
                    costAmountUsd = (offset.toBigInteger() * COST_TOKEN_USD_PRICE.toBigInteger())
                        .toBigDecimal(12)
                        .toPlainString()
                )
        }
    }

    private fun addArtistToDatabase(offset: Int = 0): Artist {
        val artist =
            transaction {
                MarketplaceArtistEntity.new {
                    email = "artist$offset@newm.io"
                    nickname = "nickname$offset"
                    genre = "genre$offset"
                    location = "location$offset"
                    biography = "biography$offset"
                    pictureUrl = "pictureUrl$offset"
                    bannerUrl = "bannerUrl$offset"
                    websiteUrl = "websiteUrl$offset"
                    websiteUrl = "websiteUrl$offset"
                    instagramUrl = "instagramUrl$offset"
                    spotifyProfile = "spotifyProfile$offset"
                    soundCloudProfile = "soundCloudProfile$offset"
                    appleMusicProfile = "appleMusicProfile$offset"
                }
            }
        for (i in 0..offset + 3) {
            transaction {
                val song =
                    SongEntity.new {
                        ownerId = artist.id
                        title = "title$offset"
                        genres = arrayOf("genre${offset}_0", "genre${offset}_1")
                        mintingStatus = MintingStatus.Released
                    }
                MarketplaceSaleEntity.new {
                    createdAt = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS)
                    status = SaleStatus.entries[offset % SaleStatus.entries.size]
                    songId = song.id
                    ownerAddress = "ownerAddress$offset"
                    pointerPolicyId = "pointerPolicyId$offset"
                    pointerAssetName = "pointerAssetName$offset"
                    bundlePolicyId = "bundlePolicyId$offset"
                    bundleAssetName = "bundleAssetName$offset"
                    bundleAmount = offset + 1L
                    costPolicyId = "costPolicyId$offset"
                    costAssetName = "costAssetName$offset"
                    costAmount = offset.toLong()
                    maxBundleSize = offset + 100L
                    totalBundleQuantity = offset + 1000L
                    availableBundleQuantity = offset + 1000L
                }
            }
        }
        return transaction { artist.toModel() }
    }
}
