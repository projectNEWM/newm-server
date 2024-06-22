package io.newm.chain.cardano.address

import io.newm.chain.util.Blake2b

class AddressCredential(
    val hash: ByteArray
) {
    companion object {
        fun fromKey(key: BIP32PublicKey): AddressCredential {
            val keyHash = Blake2b.hash224(key.pk)
            return AddressCredential(keyHash)
        }
    }
}
