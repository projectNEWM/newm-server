package io.newm.server.features.earnings

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class EarningsTest {
    @Test
    fun `test BigDecimal earnings math`() {
        val totalNewmAmount = 250000000000.toBigDecimal()
        val totalSupply = 100_000_000.toBigDecimal()
        val streamTokenAmount = 50000000.toBigDecimal()
        val royalties = (totalNewmAmount * (streamTokenAmount.setScale(6) / totalSupply.setScale(6))).toLong()
        assertThat(royalties).isEqualTo(125000000000L)
    }
}
