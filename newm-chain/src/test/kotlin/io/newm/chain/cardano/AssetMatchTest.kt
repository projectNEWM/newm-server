package io.newm.chain.cardano

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class AssetMatchTest {

    @Test
    fun `test asset regex matching`() {
        val assetName = "28313030295465737420426c6f623336"
        val cip68Regex = Regex("^2831303029.*$") // (100)TokenName
        assertThat(assetName.matches(cip68Regex)).isTrue()
    }
}
