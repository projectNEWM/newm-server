package io.newm.chain.cardano

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class AssetMatchTest {
    @Test
    fun `test asset regex matching`() {
        val assetName = "000643b05465737420426c6f623336"
        val cip68Regex = Regex("^000643b0.*$") // (100)TokenName
        assertThat(assetName.matches(cip68Regex)).isTrue()
    }
}
