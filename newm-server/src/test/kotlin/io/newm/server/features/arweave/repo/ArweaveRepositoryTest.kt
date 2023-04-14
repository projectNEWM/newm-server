package io.newm.server.features.arweave.repo

import com.google.common.truth.Truth.assertThat
import io.newm.server.ktx.asValidUrl
import org.junit.jupiter.api.Test

class ArweaveRepositoryTest {

    @Test
    fun `test webp regex replacement`() {
        val urlString = "https://res.cloudinary.com/newm/image/upload/v1671486226/welnjdtkmqevkxe0lrxg.png"
        val newUrl = urlString.asValidUrl().replace(Regex("\\.(png|jpg|jpeg|bmp|gif|tiff)\$", RegexOption.IGNORE_CASE), ".webp")
        assertThat(newUrl).isEqualTo("https://res.cloudinary.com/newm/image/upload/v1671486226/welnjdtkmqevkxe0lrxg.webp")
    }
}
