package io.newm.server.features.earnings

import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.mockk
import io.newm.server.features.song.model.Song
import io.newm.server.features.song.repo.SongRepository
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID

class ResolveSongIdTest {
    private val testSongId = UUID.randomUUID()
    private val testSong = Song(
        id = testSongId,
        title = "Test Song",
        genres = listOf("Pop"),
        isrc = "US-ABC-12-34567"
    )

    private fun mockSongRepository(returnSong: Song? = testSong): SongRepository =
        mockk {
            coEvery { getByIsrc(any()) } returns returnSong
        }

    // ==================== UUID Tests ====================

    @Test
    fun `resolveSongId returns UUID when given valid UUID`() =
        runBlocking {
            val songRepository = mockSongRepository()
            val uuid = UUID.randomUUID()

            val result = resolveSongId(uuid.toString(), songRepository)

            assertThat(result).isEqualTo(uuid)
        }

    @Test
    fun `resolveSongId returns UUID when given uppercase UUID`() =
        runBlocking {
            val songRepository = mockSongRepository()
            val uuid = UUID.randomUUID()

            val result = resolveSongId(uuid.toString().uppercase(), songRepository)

            assertThat(result).isEqualTo(uuid)
        }

    @Test
    fun `resolveSongId returns UUID when given lowercase UUID`() =
        runBlocking {
            val songRepository = mockSongRepository()
            val uuid = UUID.randomUUID()

            val result = resolveSongId(uuid.toString().lowercase(), songRepository)

            assertThat(result).isEqualTo(uuid)
        }

    // ==================== ISRC with dashes Tests ====================

    @Test
    fun `resolveSongId returns song ID when given ISRC with dashes`() =
        runBlocking {
            val songRepository = mockSongRepository()

            val result = resolveSongId("US-ABC-12-34567", songRepository)

            assertThat(result).isEqualTo(testSongId)
        }

    @Test
    fun `resolveSongId returns song ID when given lowercase ISRC with dashes`() =
        runBlocking {
            val songRepository = mockSongRepository()

            val result = resolveSongId("us-abc-12-34567", songRepository)

            assertThat(result).isEqualTo(testSongId)
        }

    @Test
    fun `resolveSongId returns song ID when given mixed case ISRC with dashes`() =
        runBlocking {
            val songRepository = mockSongRepository()

            val result = resolveSongId("Us-AbC-12-34567", songRepository)

            assertThat(result).isEqualTo(testSongId)
        }

    // ==================== ISRC without dashes Tests ====================

    @Test
    fun `resolveSongId returns song ID when given ISRC without dashes`() =
        runBlocking {
            val songRepository = mockSongRepository()

            val result = resolveSongId("USABC1234567", songRepository)

            assertThat(result).isEqualTo(testSongId)
        }

    @Test
    fun `resolveSongId returns song ID when given lowercase ISRC without dashes`() =
        runBlocking {
            val songRepository = mockSongRepository()

            val result = resolveSongId("usabc1234567", songRepository)

            assertThat(result).isEqualTo(testSongId)
        }

    @Test
    fun `resolveSongId returns song ID when given mixed case ISRC without dashes`() =
        runBlocking {
            val songRepository = mockSongRepository()

            val result = resolveSongId("UsAbC1234567", songRepository)

            assertThat(result).isEqualTo(testSongId)
        }

    // ==================== ISRC with partial dashes Tests ====================

    @Test
    fun `resolveSongId returns song ID when given ISRC with only first dash`() =
        runBlocking {
            val songRepository = mockSongRepository()

            val result = resolveSongId("US-ABC1234567", songRepository)

            assertThat(result).isEqualTo(testSongId)
        }

    @Test
    fun `resolveSongId returns song ID when given ISRC with only last dash`() =
        runBlocking {
            val songRepository = mockSongRepository()

            val result = resolveSongId("USABC12-34567", songRepository)

            assertThat(result).isEqualTo(testSongId)
        }

    // ==================== Failure Cases ====================

    @Test
    fun `resolveSongId throws IllegalArgumentException for invalid format`() =
        runBlocking {
            val songRepository = mockSongRepository()

            val exception = assertThrows<IllegalArgumentException> {
                runBlocking { resolveSongId("invalid-identifier", songRepository) }
            }

            assertThat(exception.message).contains("Invalid identifier")
        }

    @Test
    fun `resolveSongId throws IllegalArgumentException for empty string`() =
        runBlocking {
            val songRepository = mockSongRepository()

            val exception = assertThrows<IllegalArgumentException> {
                runBlocking { resolveSongId("", songRepository) }
            }

            assertThat(exception.message).contains("Invalid identifier")
        }

    @Test
    fun `resolveSongId throws IllegalArgumentException for ISRC with wrong country code length`() =
        runBlocking {
            val songRepository = mockSongRepository()

            val exception = assertThrows<IllegalArgumentException> {
                runBlocking { resolveSongId("USA-ABC-12-34567", songRepository) }
            }

            assertThat(exception.message).contains("Invalid identifier")
        }

    @Test
    fun `resolveSongId throws IllegalArgumentException for ISRC with wrong registrant length`() =
        runBlocking {
            val songRepository = mockSongRepository()

            val exception = assertThrows<IllegalArgumentException> {
                runBlocking { resolveSongId("US-ABCD-12-34567", songRepository) }
            }

            assertThat(exception.message).contains("Invalid identifier")
        }

    @Test
    fun `resolveSongId throws IllegalArgumentException for ISRC with wrong year length`() =
        runBlocking {
            val songRepository = mockSongRepository()

            val exception = assertThrows<IllegalArgumentException> {
                runBlocking { resolveSongId("US-ABC-123-34567", songRepository) }
            }

            assertThat(exception.message).contains("Invalid identifier")
        }

    @Test
    fun `resolveSongId throws IllegalArgumentException for ISRC with wrong designation length`() =
        runBlocking {
            val songRepository = mockSongRepository()

            val exception = assertThrows<IllegalArgumentException> {
                runBlocking { resolveSongId("US-ABC-12-3456", songRepository) }
            }

            assertThat(exception.message).contains("Invalid identifier")
        }

    @Test
    fun `resolveSongId throws NoSuchElementException when song not found by ISRC`() =
        runBlocking {
            val songRepository = mockSongRepository(returnSong = null)

            val exception = assertThrows<NoSuchElementException> {
                runBlocking { resolveSongId("US-ABC-12-34567", songRepository) }
            }

            assertThat(exception.message).contains("No song found with ISRC")
        }

    @Test
    fun `resolveSongId throws IllegalArgumentException for random string`() =
        runBlocking {
            val songRepository = mockSongRepository()

            val exception = assertThrows<IllegalArgumentException> {
                runBlocking { resolveSongId("not-a-valid-anything", songRepository) }
            }

            assertThat(exception.message).contains("Invalid identifier")
        }

    @Test
    fun `resolveSongId throws IllegalArgumentException for numeric only string`() =
        runBlocking {
            val songRepository = mockSongRepository()

            val exception = assertThrows<IllegalArgumentException> {
                runBlocking { resolveSongId("123456789012", songRepository) }
            }

            assertThat(exception.message).contains("Invalid identifier")
        }

    // ==================== ISRC Regex Tests ====================

    @Test
    fun `ISRC_REGEX matches valid ISRC with dashes`() {
        assertThat(ISRC_REGEX.matches("US-ABC-12-34567")).isTrue()
    }

    @Test
    fun `ISRC_REGEX matches valid ISRC without dashes`() {
        assertThat(ISRC_REGEX.matches("USABC1234567")).isTrue()
    }

    @Test
    fun `ISRC_REGEX matches lowercase ISRC`() {
        assertThat(ISRC_REGEX.matches("us-abc-12-34567")).isTrue()
    }

    @Test
    fun `ISRC_REGEX matches ISRC with alphanumeric registrant code`() {
        assertThat(ISRC_REGEX.matches("US-A1B-12-34567")).isTrue()
    }

    @Test
    fun `ISRC_REGEX does not match ISRC with invalid country code`() {
        assertThat(ISRC_REGEX.matches("1S-ABC-12-34567")).isFalse()
    }

    @Test
    fun `ISRC_REGEX does not match ISRC with too short designation`() {
        assertThat(ISRC_REGEX.matches("US-ABC-12-3456")).isFalse()
    }

    @Test
    fun `ISRC_REGEX does not match ISRC with too long designation`() {
        assertThat(ISRC_REGEX.matches("US-ABC-12-345678")).isFalse()
    }
}
