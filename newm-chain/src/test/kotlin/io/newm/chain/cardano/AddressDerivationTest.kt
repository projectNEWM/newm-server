package io.newm.chain.cardano

import com.google.common.truth.Truth.assertThat
import io.newm.chain.cardano.address.Address
import io.newm.chain.cardano.address.AddressCredential
import io.newm.chain.cardano.address.BIP32PublicKey
import io.newm.chain.cardano.address.curve25519.Fe
import io.newm.chain.util.toHexString
import io.newm.chain.util.toULong
import org.junit.jupiter.api.Test
import java.math.BigInteger

@OptIn(ExperimentalUnsignedTypes::class)
class AddressDerivationTest {
    @Test
    fun `test address derivation`() {
        val xpub =
            "xpub1u6ml2ymprr628u8leeyfuerjjjz2gnhpermdwrv5vvkkd6mmxxvfv5p2czthgh9lfx7x0lke8glvjqz57fggn4sfuha2zjq4g7xkfqgq2gwja"
        // "addr_test1qz8fg2e9yn0ga6sav0760cxmx0antql96mfuhqgzcc5swugw2jqqlugnx9qjep9xvcx40z0zfyep55r2t3lav5smyjrs96cusg"
        // m/1852'/1815'/0'/0/0

        val xpubKey = BIP32PublicKey(xpub)

        val pk0 = xpubKey.derive(0u).derive(0u)
        assertThat(
            pk0.bech32XPub
        ).isEqualTo("xpub1nnml7qn2un86kgrvqyvs7lmrhz303aprekuaugu9s82elttfnjxu2ltrhxzjvhn6wlt6xn90j2sx6a2ludjpz03xhzsmp7mq476v5usa4meq2")
        val pkCredential = AddressCredential.fromKey(pk0)
        assertThat(pkCredential.hash.toHexString()).isEqualTo("8e942b2524de8eea1d63fda7e0db33fb3583e5d6d3cb8102c6290771")
        val sk0 = xpubKey.derive(2u).derive(0u)
        assertThat(
            sk0.bech32XPub
        ).isEqualTo("xpub1hlfscunaww6xd80zyg7cxyqkkf643ywfpnvm9tvpz62900kh2zgglvwup02l0ltxm4xy58tkck8k8quq95wqnnrwnk57n83t8jwmgaqwtlfg8")
        val skCredential = AddressCredential.fromKey(sk0)
        assertThat(skCredential.hash.toHexString()).isEqualTo("0e54800ff11331412c84a6660d5789e249321a506a5c7fd6521b2487")

        val mainnetEnterpriseAddress = Address.fromPaymentAddressCredential(pkCredential, isMainnet = true)
        val testnetEnterpriseAddress = Address.fromPaymentAddressCredential(pkCredential, isMainnet = false)
        assertThat(mainnetEnterpriseAddress.address).isEqualTo("addr1vx8fg2e9yn0ga6sav0760cxmx0antql96mfuhqgzcc5swug47gq6q")
        assertThat(testnetEnterpriseAddress.address).isEqualTo("addr_test1vz8fg2e9yn0ga6sav0760cxmx0antql96mfuhqgzcc5swugwkuu49")

        val mainnetAddress =
            Address.fromPaymentStakeAddressCredentialsKeyKey(pkCredential, skCredential, isMainnet = true)
        val testnetAddress =
            Address.fromPaymentStakeAddressCredentialsKeyKey(pkCredential, skCredential, isMainnet = false)
        assertThat(
            mainnetAddress.address
        ).isEqualTo("addr1qx8fg2e9yn0ga6sav0760cxmx0antql96mfuhqgzcc5swugw2jqqlugnx9qjep9xvcx40z0zfyep55r2t3lav5smyjrsxv9uuh")
        assertThat(
            testnetAddress.address
        ).isEqualTo("addr_test1qz8fg2e9yn0ga6sav0760cxmx0antql96mfuhqgzcc5swugw2jqqlugnx9qjep9xvcx40z0zfyep55r2t3lav5smyjrs96cusg")
    }

    @Test
    fun `test address derivation2`() {
        val xpub =
            "xpub1zhm4ktg2qkp9mwuyrat9525wtzyaqszreq9ecjc7zpmshyus9xqhjr5kng46dj8g5p0k4gdk0cn2hx3ldgauf86dyrk9ljqrpljf8mgthp6hs"

        val xpubKey = BIP32PublicKey(xpub)
        val pk0 = xpubKey.derive(0u).derive(0u)
        assertThat(
            pk0.bech32XPub
        ).isEqualTo("xpub17ls9ple4gcrvn9q9qkfc0gg7c6ukmdd9pd6x7z7l3h69e0etacl8atp5vd3vl6kh7vzkma6nneps24dyk8pxls0jsjuah9n729luztqzkf62d")
        val pkCredential = AddressCredential.fromKey(pk0)
        assertThat(pkCredential.hash.toHexString()).isEqualTo("f68602c64043ecb9b0fc32077fdb934c0cf51ce82d57be83146cb60a")
    }

    @Test
    fun `test BigInteger shr`() {
        val bi = BigInteger("93411271048366834790890848459125")
        val c = (bi shr 51).toULong()
        assertThat(c).isEqualTo(41482937550959257uL)
    }

    private fun propBytes(bytes: ByteArray) {
        val f = Fe.fromBytes(bytes)
        val gotBytes = f.toBytes()
        assertThat(gotBytes).isEqualTo(bytes)
    }

    @Test
    fun `test Fe bytes serialization`() {
        propBytes(ByteArray(32) { 0 })
        propBytes(ByteArray(32) { 1 })
        propBytes(ByteArray(32) { 2 })
        propBytes(ByteArray(32) { 0x5f })
        propBytes(
            byteArrayOf(
                0,
                2,
                3,
                4,
                5,
                1,
                2,
                3,
                4,
                5,
                1,
                2,
                3,
                4,
                5,
                1,
                2,
                3,
                4,
                5,
                1,
                2,
                3,
                4,
                5,
                1,
                2,
                3,
                4,
                5,
                1,
                0
            )
        )

        // 2^255-20 FieldElement representation
        val fe25520 =
            Fe(
                ulongArrayOf(
                    0x7FFFFFFFFFFECuL,
                    0x7FFFFFFFFFFFFuL,
                    0x7FFFFFFFFFFFFuL,
                    0x7FFFFFFFFFFFFuL,
                    0x7FFFFFFFFFFFFuL
                )
            )

        // 2^255-19 FieldElement representation
        val fe25519 =
            Fe(
                ulongArrayOf(
                    0x7FFFFFFFFFFEDuL,
                    0x7FFFFFFFFFFFFuL,
                    0x7FFFFFFFFFFFFuL,
                    0x7FFFFFFFFFFFFuL,
                    0x7FFFFFFFFFFFFuL
                )
            )

        // 2^255-18 FieldElement representation
        val fe25518 =
            Fe(
                ulongArrayOf(
                    0x7FFFFFFFFFFEEuL,
                    0x7FFFFFFFFFFFFuL,
                    0x7FFFFFFFFFFFFuL,
                    0x7FFFFFFFFFFFFuL,
                    0x7FFFFFFFFFFFFuL
                )
            )

        assertThat(Fe.ZERO.toBytes()).isEqualTo(fe25519.toBytes())
        assertThat(Fe.ONE.toBytes()).isEqualTo(fe25518.toBytes())
        assertThat((Fe.ZERO - Fe.ONE).toBytes()).isEqualTo(fe25520.toBytes())
    }
}
