package io.newm.chain.util

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

// Reference: https://cips.cardano.org/cip/CIP-19

class PaymentAddressHashTests {
    @Test
    fun testTestnetPaymentAddressWithoutStakeKey() {
        val address = "addr_test1vz2fxv2umyhttkxyxp8x0dlpdt3k6cwng5pxj3jhsydzerspjrlsz"
        val hash = address.paymentAddressToHash()
        assertThat(hash.size).isEqualTo(28)
        val decodedAddress = hash.hashToPaymentAddress(false)
        assertThat(decodedAddress).isEqualTo(address)
    }

    @Test
    fun testTestnetPaymentAddressWithStakeKey() {
        val address = "addr_test1qz2fxv2umyhttkxyxp8x0dlpdt3k6cwng5pxj3jhsydzer3n0d3vllmyqwsx5wktcd8cc3sq835lu7drv2xwl2wywfgs68faae"
        val hash = address.paymentAddressToHash()
        assertThat(hash.size).isEqualTo(56)
        val decodedAddress = hash.hashToPaymentAddress(false)
        assertThat(decodedAddress).isEqualTo(address)
    }

    @Test
    fun testMainnetPaymentAddressWithoutStakeKey() {
        val address = "addr1vx2fxv2umyhttkxyxp8x0dlpdt3k6cwng5pxj3jhsydzers66hrl8"
        val hash = address.paymentAddressToHash()
        assertThat(hash.size).isEqualTo(28)
        val decodedAddress = hash.hashToPaymentAddress(true)
        assertThat(decodedAddress).isEqualTo(address)
    }

    @Test
    fun testMainnetPaymentAddressWithStakeKey() {
        val address = "addr1qx2fxv2umyhttkxyxp8x0dlpdt3k6cwng5pxj3jhsydzer3n0d3vllmyqwsx5wktcd8cc3sq835lu7drv2xwl2wywfgse35a3x"
        val hash = address.paymentAddressToHash()
        assertThat(hash.size).isEqualTo(56)
        val decodedAddress = hash.hashToPaymentAddress(true)
        assertThat(decodedAddress).isEqualTo(address)
    }
}
