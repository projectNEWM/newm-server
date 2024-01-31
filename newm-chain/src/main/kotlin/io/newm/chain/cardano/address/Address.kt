package io.newm.chain.cardano.address

import io.newm.chain.util.Bech32
import io.newm.chain.util.Constants.PAYMENT_ADDRESS_PREFIX_MAINNET
import io.newm.chain.util.Constants.PAYMENT_ADDRESS_PREFIX_TESTNET
import io.newm.chain.util.Constants.PAYMENT_ADDRESS_SCRIPT_PREFIX_MAINNET
import io.newm.chain.util.Constants.PAYMENT_ADDRESS_SCRIPT_PREFIX_TESTNET
import io.newm.chain.util.Constants.PAYMENT_STAKE_ADDRESS_KEY_KEY_PREFIX_MAINNET
import io.newm.chain.util.Constants.PAYMENT_STAKE_ADDRESS_KEY_KEY_PREFIX_TESTNET
import io.newm.chain.util.Constants.PAYMENT_STAKE_ADDRESS_KEY_SCRIPT_PREFIX_MAINNET
import io.newm.chain.util.Constants.PAYMENT_STAKE_ADDRESS_KEY_SCRIPT_PREFIX_TESTNET
import io.newm.chain.util.Constants.PAYMENT_STAKE_ADDRESS_SCRIPT_KEY_PREFIX_MAINNET
import io.newm.chain.util.Constants.PAYMENT_STAKE_ADDRESS_SCRIPT_KEY_PREFIX_TESTNET
import io.newm.chain.util.Constants.PAYMENT_STAKE_ADDRESS_SCRIPT_SCRIPT_PREFIX_MAINNET
import io.newm.chain.util.Constants.PAYMENT_STAKE_ADDRESS_SCRIPT_SCRIPT_PREFIX_TESTNET
import io.newm.chain.util.Constants.STAKE_ADDRESS_KEY_PREFIX_MAINNET
import io.newm.chain.util.Constants.STAKE_ADDRESS_KEY_PREFIX_TESTNET

class Address(val address: String) {
    companion object {
        fun fromPaymentAddressCredential(
            paymentAddressCredential: AddressCredential,
            isMainnet: Boolean
        ): Address {
            val address =
                if (isMainnet) {
                    Bech32.encode("addr", ByteArray(1) { PAYMENT_ADDRESS_PREFIX_MAINNET } + paymentAddressCredential.hash)
                } else {
                    Bech32.encode(
                        "addr_test",
                        ByteArray(1) { PAYMENT_ADDRESS_PREFIX_TESTNET } + paymentAddressCredential.hash
                    )
                }
            return Address(address)
        }

        fun fromScriptAddressCredential(
            scriptAddressCredential: AddressCredential,
            isMainnet: Boolean
        ): Address {
            val address =
                if (isMainnet) {
                    Bech32.encode(
                        "addr",
                        ByteArray(1) { PAYMENT_ADDRESS_SCRIPT_PREFIX_MAINNET } + scriptAddressCredential.hash
                    )
                } else {
                    Bech32.encode(
                        "addr_test",
                        ByteArray(1) { PAYMENT_ADDRESS_SCRIPT_PREFIX_TESTNET } + scriptAddressCredential.hash
                    )
                }
            return Address(address)
        }

        fun fromStakeAddressCredential(
            stakeAddressCredential: AddressCredential,
            isMainnet: Boolean
        ): Address {
            val address =
                if (isMainnet) {
                    Bech32.encode("stake", ByteArray(1) { STAKE_ADDRESS_KEY_PREFIX_MAINNET } + stakeAddressCredential.hash)
                } else {
                    Bech32.encode(
                        "stake_test",
                        ByteArray(1) { STAKE_ADDRESS_KEY_PREFIX_TESTNET } + stakeAddressCredential.hash
                    )
                }
            return Address(address)
        }

        fun fromPaymentStakeAddressCredentialsKeyKey(
            paymentAddressCredential: AddressCredential,
            stakeAddressCredential: AddressCredential,
            isMainnet: Boolean
        ): Address {
            val address =
                if (isMainnet) {
                    Bech32.encode(
                        "addr",
                        ByteArray(
                            1
                        ) { PAYMENT_STAKE_ADDRESS_KEY_KEY_PREFIX_MAINNET } + paymentAddressCredential.hash + stakeAddressCredential.hash
                    )
                } else {
                    Bech32.encode(
                        "addr_test",
                        ByteArray(
                            1
                        ) { PAYMENT_STAKE_ADDRESS_KEY_KEY_PREFIX_TESTNET } + paymentAddressCredential.hash + stakeAddressCredential.hash
                    )
                }
            return Address(address)
        }

        fun fromPaymentStakeAddressCredentialsScriptKey(
            paymentAddressCredential: AddressCredential,
            stakeAddressCredential: AddressCredential,
            isMainnet: Boolean
        ): Address {
            val address =
                if (isMainnet) {
                    Bech32.encode(
                        "addr",
                        ByteArray(
                            1
                        ) { PAYMENT_STAKE_ADDRESS_SCRIPT_KEY_PREFIX_MAINNET } + paymentAddressCredential.hash + stakeAddressCredential.hash
                    )
                } else {
                    Bech32.encode(
                        "addr_test",
                        ByteArray(
                            1
                        ) { PAYMENT_STAKE_ADDRESS_SCRIPT_KEY_PREFIX_TESTNET } + paymentAddressCredential.hash + stakeAddressCredential.hash
                    )
                }
            return Address(address)
        }

        fun fromPaymentStakeAddressCredentialsKeyScript(
            paymentAddressCredential: AddressCredential,
            stakeAddressCredential: AddressCredential,
            isMainnet: Boolean
        ): Address {
            val address =
                if (isMainnet) {
                    Bech32.encode(
                        "addr",
                        ByteArray(
                            1
                        ) { PAYMENT_STAKE_ADDRESS_KEY_SCRIPT_PREFIX_MAINNET } + paymentAddressCredential.hash + stakeAddressCredential.hash
                    )
                } else {
                    Bech32.encode(
                        "addr_test",
                        ByteArray(
                            1
                        ) { PAYMENT_STAKE_ADDRESS_KEY_SCRIPT_PREFIX_TESTNET } + paymentAddressCredential.hash + stakeAddressCredential.hash
                    )
                }
            return Address(address)
        }

        fun fromPaymentStakeAddressCredentialsScriptScript(
            paymentAddressCredential: AddressCredential,
            stakeAddressCredential: AddressCredential,
            isMainnet: Boolean
        ): Address {
            val address =
                if (isMainnet) {
                    Bech32.encode(
                        "addr",
                        ByteArray(
                            1
                        ) { PAYMENT_STAKE_ADDRESS_SCRIPT_SCRIPT_PREFIX_MAINNET } + paymentAddressCredential.hash + stakeAddressCredential.hash
                    )
                } else {
                    Bech32.encode(
                        "addr_test",
                        ByteArray(
                            1
                        ) { PAYMENT_STAKE_ADDRESS_SCRIPT_SCRIPT_PREFIX_TESTNET } + paymentAddressCredential.hash + stakeAddressCredential.hash
                    )
                }
            return Address(address)
        }
    }
}
