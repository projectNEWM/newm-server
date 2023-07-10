package io.newm.chain.cardano.address.curve25519

@OptIn(ExperimentalUnsignedTypes::class)
data class GePrecomp(internal val yPlusX: Fe, internal val yMinusX: Fe, internal val xy2d: Fe) {

    companion object {
        private val GE_BASE: Array<Array<GePrecomp>> = arrayOf(
            arrayOf(
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x493c6f58c3b85uL,
                            0xdf7181c325f7uL,
                            0xf50b0b3e4cb7uL,
                            0x5329385a44c32uL,
                            0x7cf9d3a33d4buL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x3905d740913euL,
                            0xba2817d673a2uL,
                            0x23e2827f4e67cuL,
                            0x133d2e0c21a34uL,
                            0x44fd2f9298f81uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x11205877aaa68uL,
                            0x479955893d579uL,
                            0x50d66309b67a0uL,
                            0x2d42d0dbee5eeuL,
                            0x6f117b689f0c6uL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x4e7fc933c71d7uL,
                            0x2cf41feb6b244uL,
                            0x7581c0a7d1a76uL,
                            0x7172d534d32f0uL,
                            0x590c063fa87d2uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x1a56042b4d5a8uL,
                            0x189cc159ed153uL,
                            0x5b8deaa3cae04uL,
                            0x2aaf04f11b5d8uL,
                            0x6bb595a669c92uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x2a8b3a59b7a5fuL,
                            0x3abb359ef087fuL,
                            0x4f5a8c4db05afuL,
                            0x5b9a807d04205uL,
                            0x701af5b13ea50uL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x5b0a84cee9730uL,
                            0x61d10c97155e4uL,
                            0x4059cc8096a10uL,
                            0x47a608da8014fuL,
                            0x7a164e1b9a80fuL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x11fe8a4fcd265uL,
                            0x7bcb8374faaccuL,
                            0x52f5af4ef4d4fuL,
                            0x5314098f98d10uL,
                            0x2ab91587555bduL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x6933f0dd0d889uL,
                            0x44386bb4c4295uL,
                            0x3cb6d3162508cuL,
                            0x26368b872a2c6uL,
                            0x5a2826af12b9buL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x351b98efc099fuL,
                            0x68fbfa4a7050euL,
                            0x42a49959d971buL,
                            0x393e51a469efduL,
                            0x680e910321e58uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x6050a056818bfuL,
                            0x62acc1f5532bfuL,
                            0x28141ccc9fa25uL,
                            0x24d61f471e683uL,
                            0x27933f4c7445auL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x3fbe9c476ff09uL,
                            0xaf6b982e4b42uL,
                            0xad1251ba78e5uL,
                            0x715aeedee7c88uL,
                            0x7f9d0cbf63553uL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x2bc4408a5bb33uL,
                            0x78ebdda05442uL,
                            0x2ffb112354123uL,
                            0x375ee8df5862duL,
                            0x2945ccf146e20uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x182c3a447d6bauL,
                            0x22964e536eff2uL,
                            0x192821f540053uL,
                            0x2f9f19e788e5cuL,
                            0x154a7e73eb1b5uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x3dbf1812a8285uL,
                            0xfa17ba3f9797uL,
                            0x6f69cb49c3820uL,
                            0x34d5a0db3858duL,
                            0x43aabe696b3bbuL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x4eeeb77157131uL,
                            0x1201915f10741uL,
                            0x1669cda6c9c56uL,
                            0x45ec032db346duL,
                            0x51e57bb6a2cc3uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x6b67b7d8ca4uL,
                            0x84fa44e72933uL,
                            0x1154ee55d6f8auL,
                            0x4425d842e7390uL,
                            0x38b64c41ae417uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x4326702ea4b71uL,
                            0x6834376030b5uL,
                            0xef0512f9c380uL,
                            0xf1a9f2512584uL,
                            0x10b8e91a9f0d6uL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x25cd0944ea3bfuL,
                            0x75673b81a4d63uL,
                            0x150b925d1c0d4uL,
                            0x13f38d9294114uL,
                            0x461bea69283c9uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x72c9aaa3221b1uL,
                            0x267774474f74duL,
                            0x64b0e9b28085uL,
                            0x3f04ef53b27c9uL,
                            0x1d6edd5d2e531uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x36dc801b8b3a2uL,
                            0xe0a7d4935e30uL,
                            0x1deb7cecc0d7duL,
                            0x53a94e20dd2cuL,
                            0x7a9fbb1c6a0f9uL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x7596604dd3e8fuL,
                            0x6fc510e058b36uL,
                            0x3670c8db2cc0duL,
                            0x297d899ce332fuL,
                            0x915e76061bceuL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x75dedf39234d9uL,
                            0x1c36ab1f3c54uL,
                            0xf08fee58f5dauL,
                            0xe19613a0d637uL,
                            0x3a9024a1320e0uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x1f5d9c9a2911auL,
                            0x7117994fafcf8uL,
                            0x2d8a8cae28dc5uL,
                            0x74ab1b2090c87uL,
                            0x26907c5c2ecc4uL
                        )
                    )
                ),
            ),
            arrayOf(
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x4dd0e632f9c1duL,
                            0x2ced12622a5d9uL,
                            0x18de9614742dauL,
                            0x79ca96fdbb5d4uL,
                            0x6dd37d49a00eeuL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x3635449aa515euL,
                            0x3e178d0475dabuL,
                            0x50b4712a19712uL,
                            0x2dcc2860ff4aduL,
                            0x30d76d6f03d31uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x444172106e4c7uL,
                            0x1251afed2d88uL,
                            0x534fc9bed4f5auL,
                            0x5d85a39cf5234uL,
                            0x10c697112e864uL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x62aa08358c805uL,
                            0x46f440848e194uL,
                            0x447b771a8f52buL,
                            0x377ba3269d31duL,
                            0x3bf9baf55080uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x3c4277dbe5fdeuL,
                            0x5a335afd44c92uL,
                            0xc1164099753euL,
                            0x70487006fe423uL,
                            0x25e61cabed66fuL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x3e128cc586604uL,
                            0x5968b2e8fc7e2uL,
                            0x49a3d5bd61cfuL,
                            0x116505b1ef6e6uL,
                            0x566d78634586euL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x54285c65a2fd0uL,
                            0x55e62ccf87420uL,
                            0x46bb961b19044uL,
                            0x1153405712039uL,
                            0x14fba5f34793buL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x7a49f9cc10834uL,
                            0x2b513788a22c6uL,
                            0x5ff4b6ef2395buL,
                            0x2ec8e5af607bfuL,
                            0x33975bca5ecc3uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x746166985f7d4uL,
                            0x9939000ae79auL,
                            0x5844c7964f97auL,
                            0x13617e1f95b3duL,
                            0x14829cea83fc5uL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x70b2f4e71ecb8uL,
                            0x728148efc643cuL,
                            0x753e03995b76uL,
                            0x5bf5fb2ab6767uL,
                            0x5fc3bc4535d7uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x37b8497dd95c2uL,
                            0x61549d6b4ffe8uL,
                            0x217a22db1d138uL,
                            0xb9cf062eb09euL,
                            0x2fd9c71e5f758uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0xb3ae52afdedduL,
                            0x19da76619e497uL,
                            0x6fa0654d2558euL,
                            0x78219d25e41d4uL,
                            0x373767475c651uL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x95cb14246590uL,
                            0x2d82aa6ac68uL,
                            0x442f183bc4851uL,
                            0x6464f1c0a0644uL,
                            0x6bf5905730907uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x299fd40d1add9uL,
                            0x5f2de9a04e5f7uL,
                            0x7c0eebacc1c59uL,
                            0x4cca1b1f8290auL,
                            0x1fbea56c3b18fuL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x778f1e1415b8auL,
                            0x6f75874efc1f4uL,
                            0x28a694019027fuL,
                            0x52b37a96bdc4duL,
                            0x2521cf67a635uL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x46720772f5ee4uL,
                            0x632c0f359d622uL,
                            0x2b2092ba3e252uL,
                            0x662257c112680uL,
                            0x1753d9f7cd6uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x7ee0b0a9d5294uL,
                            0x381fbeb4cca27uL,
                            0x7841f3a3e639duL,
                            0x676ea30c3445fuL,
                            0x3fa00a7e71382uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x1232d963ddb34uL,
                            0x35692e70b078duL,
                            0x247ca14777a1fuL,
                            0x6db556be8fcd0uL,
                            0x12b5fe2fa048euL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x37c26ad6f1e92uL,
                            0x46a0971227be5uL,
                            0x4722f0d2d9b4cuL,
                            0x3dc46204ee03auL,
                            0x6f7e93c20796cuL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0xfbc496fce34duL,
                            0x575be6b7dae3euL,
                            0x4a31585cee609uL,
                            0x37e9023930ffuL,
                            0x749b76f96fb12uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x2f604aea6ae05uL,
                            0x637dc939323ebuL,
                            0x3fdad9b048d47uL,
                            0xa8b0d4045af7uL,
                            0xfcec10f01e02uL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x2d29dc4244e45uL,
                            0x6927b1bc147beuL,
                            0x308534ac0839uL,
                            0x4853664033f41uL,
                            0x413779166feabuL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x558a649fe1e44uL,
                            0x44635aeefcc89uL,
                            0x1ff434887f2bauL,
                            0xf981220e2d44uL,
                            0x4901aa7183c51uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x1b7548c1af8f0uL,
                            0x7848c53368116uL,
                            0x1b64e7383de9uL,
                            0x109fbb0587c8fuL,
                            0x41bb887b726d1uL
                        )
                    )
                ),
            ),
            arrayOf(
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x34c597c6691aeuL,
                            0x7a150b6990fc4uL,
                            0x52beb9d922274uL,
                            0x70eed7164861auL,
                            0xa871e070c6a9uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x7d44744346beuL,
                            0x282b6a564a81duL,
                            0x4ed80f875236buL,
                            0x6fbbe1d450c50uL,
                            0x4eb728c12fcdbuL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x1b5994bbc8989uL,
                            0x74b7ba84c0660uL,
                            0x75678f1cdaeb8uL,
                            0x23206b0d6f10cuL,
                            0x3ee7300f2685duL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x27947841e7518uL,
                            0x32c7388dae87fuL,
                            0x414add3971be9uL,
                            0x1850832f0ef1uL,
                            0x7d47c6a2cfb89uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x255e49e7dd6b7uL,
                            0x38c2163d59ebauL,
                            0x3861f2a005845uL,
                            0x2e11e4ccbaec9uL,
                            0x1381576297912uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x2d0148ef0d6e0uL,
                            0x3522a8de787fbuL,
                            0x2ee055e74f9d2uL,
                            0x64038f6310813uL,
                            0x148cf58d34c9euL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x72f7d9ae4756duL,
                            0x7711e690ffc4auL,
                            0x582a2355b0d16uL,
                            0xdccfe885b6b4uL,
                            0x278febad4eaeauL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x492f67934f027uL,
                            0x7ded0815528d4uL,
                            0x58461511a6612uL,
                            0x5ea2e50de1544uL,
                            0x3ff2fa1ebd5dbuL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x2681f8c933966uL,
                            0x3840521931635uL,
                            0x674f14a308652uL,
                            0x3bd9c88a94890uL,
                            0x4104dd02fe9c6uL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x14e06db096ab8uL,
                            0x1219c89e6b024uL,
                            0x278abd486a2dbuL,
                            0x240b292609520uL,
                            0x165b5a48efcauL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x2bf5e1124422auL,
                            0x673146756ae56uL,
                            0x14ad99a87e830uL,
                            0x1eaca65b080fduL,
                            0x2c863b00afaf5uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0xa474a0846a76uL,
                            0x99a5ef981e32uL,
                            0x2a8ae3c4bbfe6uL,
                            0x45c34af14832cuL,
                            0x591b67d9bffecuL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x1b3719f18b55duL,
                            0x754318c83d337uL,
                            0x27c17b7919797uL,
                            0x145b084089b61uL,
                            0x489b4f8670301uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x70d1c80b49bfauL,
                            0x3d57e7d914625uL,
                            0x3c0722165e545uL,
                            0x5e5b93819e04fuL,
                            0x3de02ec7ca8f7uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x2102d3aeb92efuL,
                            0x68c22d50c3a46uL,
                            0x42ea89385894euL,
                            0x75f9ebf55f38cuL,
                            0x49f5fbba496cbuL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x5628c1e9c572euL,
                            0x598b108e822abuL,
                            0x55d8fae29361auL,
                            0xadc8d1a97b28uL,
                            0x6a1a6c288675uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x49a108a5bcfd4uL,
                            0x6178c8e7d6612uL,
                            0x1f03473710375uL,
                            0x73a49614a6098uL,
                            0x5604a86dcbfa6uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0xd1d47c1764b6uL,
                            0x1c08316a2e51uL,
                            0x2b3db45c95045uL,
                            0x1634f818d300cuL,
                            0x20989e89fe274uL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x4278b85eaec2euL,
                            0xef59657be2ceuL,
                            0x72fd169588770uL,
                            0x2e9b205260b30uL,
                            0x730b9950f7059uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x777fd3a2dcc7fuL,
                            0x594a9fb124932uL,
                            0x1f8e80ca15f0uL,
                            0x714d13cec3269uL,
                            0x403ed1d0ca67uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x32d35874ec552uL,
                            0x1f3048df1b929uL,
                            0x300d73b179b23uL,
                            0x6e67be5a37d0buL,
                            0x5bd7454308303uL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x4932115e7792auL,
                            0x457b9bbb930b8uL,
                            0x68f5d8b193226uL,
                            0x4164e8f1ed456uL,
                            0x5bb7db123067fuL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x2d19528b24cc2uL,
                            0x4ac66b8302ff3uL,
                            0x701c8d9fdad51uL,
                            0x6c1b35c5b3727uL,
                            0x133a78007380auL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x1f467c6ca62beuL,
                            0x2c4232a5dc12cuL,
                            0x7551dc013b087uL,
                            0x690c11b03bcduL,
                            0x740dca6d58f0euL
                        )
                    )
                ),
            ),
            arrayOf(
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x28c570478433cuL,
                            0x1d8502873a463uL,
                            0x7641e7eded49cuL,
                            0x1ecedd54cf571uL,
                            0x2c03f5256c2b0uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0xee0752cfce4euL,
                            0x660dd8116fbe9uL,
                            0x55167130fffebuL,
                            0x1c682b885955cuL,
                            0x161d25fa963eauL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x718757b53a47duL,
                            0x619e18b0f2f21uL,
                            0x5fbdfe4c1ec04uL,
                            0x5d798c81ebb92uL,
                            0x699468bdbd96buL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x53de66aa91948uL,
                            0x45f81a599b1buL,
                            0x3f7a8bd214193uL,
                            0x71d4da412331auL,
                            0x293e1c4e6c4a2uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x72f46f4dafecfuL,
                            0x2948ffadef7a3uL,
                            0x11ecdfdf3bc04uL,
                            0x3c2e98ffeed25uL,
                            0x525219a473905uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x6134b925112e1uL,
                            0x6bb942bb406eduL,
                            0x70c445c0dde2uL,
                            0x411d822c4d7a3uL,
                            0x5b605c447f032uL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x1fec6f0e7f04cuL,
                            0x3cebc692c477duL,
                            0x77986a19a95euL,
                            0x6eaaaa1778b0fuL,
                            0x2f12fef4cc5abuL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x5805920c47c89uL,
                            0x1924771f9972cuL,
                            0x38bbddf9fc040uL,
                            0x1f7000092b281uL,
                            0x24a76dcea8aebuL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x522b2dfc0c740uL,
                            0x7e8193480e148uL,
                            0x33fd9a04341b9uL,
                            0x3c863678a20bcuL,
                            0x5e607b2518a43uL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x4431ca596cf14uL,
                            0x15da7c801405uL,
                            0x3c9b6f8f10b5uL,
                            0x346922934017uL,
                            0x201f33139e457uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x31d8f6cdf1818uL,
                            0x1f86c4b144b16uL,
                            0x39875b8d73e9duL,
                            0x2fbf0d9ffa7b3uL,
                            0x5067acab6ccdduL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x27f6b08039d51uL,
                            0x4802f8000dfaauL,
                            0x9692a062c525uL,
                            0x1baea91075817uL,
                            0x397cba8862460uL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x5c3fbc81379e7uL,
                            0x41bbc255e2f02uL,
                            0x6a3f756998650uL,
                            0x1297fd4e07c42uL,
                            0x771b4022c1e1cuL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x13093f05959b2uL,
                            0x1bd352f2ec618uL,
                            0x75789b88ea86uL,
                            0x61d1117ea48b9uL,
                            0x2339d320766e6uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x5d986513a2fa7uL,
                            0x63f3a99e11b0fuL,
                            0x28a0ecfd6b26duL,
                            0x53b6835e18d8fuL,
                            0x331a189219971uL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x12f3a9d7572afuL,
                            0x10d00e953c4cauL,
                            0x603df116f2f8auL,
                            0x33dc276e0e088uL,
                            0x1ac9619ff649auL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x66f45fb4f80c6uL,
                            0x3cc38eeb9fea2uL,
                            0x107647270db1fuL,
                            0x710f1ea740dc8uL,
                            0x31167c6b83bdfuL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x33842524b1068uL,
                            0x77dd39d30fe45uL,
                            0x189432141a0d0uL,
                            0x88fe4eb8c225uL,
                            0x612436341f08buL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x349e31a2d2638uL,
                            0x137a7fa6b16cuL,
                            0x681ae92777edcuL,
                            0x222bfc5f8dc51uL,
                            0x1522aa3178d90uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x541db874e898duL,
                            0x62d80fb841b33uL,
                            0x3e6ef027fa97uL,
                            0x7a03c9e9633e8uL,
                            0x46ebe2309e5efuL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x2f5369614938uL,
                            0x356e5ada20587uL,
                            0x11bc89f6bf902uL,
                            0x36746419c8dbuL,
                            0x45fe70f505243uL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x24920c8951491uL,
                            0x107ec61944c5euL,
                            0x72752e017c01fuL,
                            0x122b7dda2e97auL,
                            0x16619f6db57a2uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x75a6960c0b8cuL,
                            0x6dde1c5e41b49uL,
                            0x42e3f516da341uL,
                            0x16a03fda8e79euL,
                            0x428d1623a0e39uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x74a4401a308fduL,
                            0x6ed4b9558109uL,
                            0x746f1f6a08867uL,
                            0x4636f5c6f2321uL,
                            0x1d81592d60bd3uL
                        )
                    )
                ),
            ),
            arrayOf(
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x5b69f7b85c5e8uL,
                            0x17a2d175650ecuL,
                            0x4cc3e6dbfc19euL,
                            0x73e1d3873be0euL,
                            0x3a5f6d51b0af8uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x68756a60dac5fuL,
                            0x55d757b8aec26uL,
                            0x3383df45f80bduL,
                            0x6783f8c9f96a6uL,
                            0x20234a7789ecduL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x20db67178b252uL,
                            0x73aa3da2c0edauL,
                            0x79045c01c70d3uL,
                            0x1b37b15251059uL,
                            0x7cd682353cffeuL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x5cd6068acf4f3uL,
                            0x3079afc7a74ccuL,
                            0x58097650b64b4uL,
                            0x47fabac9c4e99uL,
                            0x3ef0253b2b2cduL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x1a45bd887fab6uL,
                            0x65748076dc17cuL,
                            0x5b98000aa11a8uL,
                            0x4a1ecc9080974uL,
                            0x2838c8863bdc0uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x3b0cf4a465030uL,
                            0x22b8aef57a2duL,
                            0x2ad0677e925aduL,
                            0x4094167d7457auL,
                            0x21dcb8a606a82uL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x500fabe7731bauL,
                            0x7cc53c3113351uL,
                            0x7cf65fe080d81uL,
                            0x3c5d966011ba1uL,
                            0x5d840dbf6c6f6uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x4468c9d9fc8uL,
                            0x5da8554796b8cuL,
                            0x3b8be70950025uL,
                            0x6d5892da6a609uL,
                            0xbc3d08194a31uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x6380d309fe18buL,
                            0x4d73c2cb8ee0duL,
                            0x6b882adbac0b6uL,
                            0x36eabdddd4cbeuL,
                            0x3a4276232ac19uL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0xc172db447ecbuL,
                            0x3f8c505b7a77fuL,
                            0x6a857f97f3f10uL,
                            0x4fcc0567fe03auL,
                            0x770c9e824e1auL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x2432c8a7084fauL,
                            0x47bf73ca8a968uL,
                            0x1639176262867uL,
                            0x5e8df4f8010ceuL,
                            0x1ff177cea16deuL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x1d99a45b5b5fduL,
                            0x523674f2499ecuL,
                            0xf8fa26182613uL,
                            0x58f7398048c98uL,
                            0x39f264fd41500uL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x34aabfe097be1uL,
                            0x43bfc03253a33uL,
                            0x29bc7fe91b7f3uL,
                            0xa761e4844a16uL,
                            0x65c621272c35fuL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x53417dbe7e29cuL,
                            0x54573827394f5uL,
                            0x565eea6f650dduL,
                            0x42050748dc749uL,
                            0x1712d73468889uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x389f8ce3193dduL,
                            0x2d424b8177ce5uL,
                            0x73fa0d3440cduL,
                            0x139020cd49e97uL,
                            0x22f9800ab19ceuL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x29fdd9a6efdacuL,
                            0x7c694a9282840uL,
                            0x6f7cdeee44b3auL,
                            0x55a3207b25cc3uL,
                            0x4171a4d38598cuL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x2368a3e9ef8cbuL,
                            0x454aa08e2ac0buL,
                            0x490923f8fa700uL,
                            0x372aa9ea4582fuL,
                            0x13f416cd64762uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x758aa99c94c8cuL,
                            0x5f6001700ff44uL,
                            0x7694e488c01bduL,
                            0xd5fde948eed6uL,
                            0x508214fa574bduL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x215bb53d003d6uL,
                            0x1179e792ca8c3uL,
                            0x1a0e96ac840a2uL,
                            0x22393e2bb3ab6uL,
                            0x3a7758a4c86cbuL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x269153ed6fe4buL,
                            0x72a23aef89840uL,
                            0x52be5299699cuL,
                            0x3a5e5ef132316uL,
                            0x22f960ec6fabauL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x111f693ae5076uL,
                            0x3e3bfaa94ca90uL,
                            0x445799476b887uL,
                            0x24a0912464879uL,
                            0x5d9fd15f8de7fuL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x44d2aeed7521euL,
                            0x50865d2c2a7e4uL,
                            0x2705b5238ea40uL,
                            0x46c70b25d3b97uL,
                            0x3bc187fa47eb9uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x408d36d63727fuL,
                            0x5faf8f6a66062uL,
                            0x2bb892da8de6buL,
                            0x769d4f0c7e2e6uL,
                            0x332f35914f8fbuL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x70115ea86c20cuL,
                            0x16d88da24ada8uL,
                            0x1980622662adfuL,
                            0x501ebbc195a9duL,
                            0x450d81ce906fbuL
                        )
                    )
                ),
            ),
            arrayOf(
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x4d8961cae743fuL,
                            0x6bdc38c7dba0euL,
                            0x7d3b4a7e1b463uL,
                            0x844bdee2adf3uL,
                            0x4cbad279663abuL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x3b6a1a6205275uL,
                            0x2e82791d06dcfuL,
                            0x23d72caa93c87uL,
                            0x5f0b7ab68aaf4uL,
                            0x2de25d4ba6345uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x19024a0d71fcduL,
                            0x15f65115f101auL,
                            0x4e99067149708uL,
                            0x119d8d1cba5afuL,
                            0x7d7fbcefe2007uL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x45dc5f3c29094uL,
                            0x3455220b579afuL,
                            0x70c1631e068auL,
                            0x26bc0630e9b21uL,
                            0x4f9cd196dcd8duL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x71e6a266b2801uL,
                            0x9aae73e2df5duL,
                            0x40dd8b219b1a3uL,
                            0x546fb4517de0duL,
                            0x5975435e87b75uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x297d86a7b3768uL,
                            0x4835a2f4c6332uL,
                            0x70305f434160uL,
                            0x183dd014e56aeuL,
                            0x7ccdd084387a0uL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x484186760cc93uL,
                            0x7435665533361uL,
                            0x2f686336b801uL,
                            0x5225446f64331uL,
                            0x3593ca848190cuL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x6422c6d260417uL,
                            0x212904817bb94uL,
                            0x5a319deb854f5uL,
                            0x7a9d4e060da7duL,
                            0x428bd0ed61d0cuL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x3189a5e849aa7uL,
                            0x6acbb1f59b242uL,
                            0x7f6ef4753630cuL,
                            0x1f346292a2da9uL,
                            0x27398308da2d6uL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x10e4c0a702453uL,
                            0x4daafa37bd734uL,
                            0x49f6bdc3e8961uL,
                            0x1feffdcecdae6uL,
                            0x572c2945492c3uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x38d28435ed413uL,
                            0x4064f19992858uL,
                            0x7680fbef543cduL,
                            0x1aadd83d58d3cuL,
                            0x269597aebe8c3uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x7c745d6cd30beuL,
                            0x27c7755df78efuL,
                            0x1776833937fa3uL,
                            0x5405116441855uL,
                            0x7f985498c05bcuL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x615520fbf6363uL,
                            0xb9e9bf74da6auL,
                            0x4fe8308201169uL,
                            0x173f76127de43uL,
                            0x30f2653cd69b1uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x1ce889f0be117uL,
                            0x36f6a94510709uL,
                            0x7f248720016b4uL,
                            0x1821ed1e1cf91uL,
                            0x76c2ec470a31fuL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0xc938aac10c85uL,
                            0x41b64ed797141uL,
                            0x1beb1c1185e6duL,
                            0x1ed5490600f07uL,
                            0x2f1273f159647uL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x8bd755a70bc0uL,
                            0x49e3a885ce609uL,
                            0x16585881b5ad6uL,
                            0x3c27568d34f5euL,
                            0x38ac1997edc5fuL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x1fc7c8ae01e11uL,
                            0x2094d5573e8e7uL,
                            0x5ca3cbbf549d2uL,
                            0x4f920ecc54143uL,
                            0x5d9e572ad85b6uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x6b517a751b13buL,
                            0xcfd370b180ccuL,
                            0x5377925d1f41auL,
                            0x34e56566008a2uL,
                            0x22dfcd9cbfe9euL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x459b4103be0a1uL,
                            0x59a4b3f2d2adduL,
                            0x7d734c8bb8eebuL,
                            0x2393cbe594a09uL,
                            0xfe9877824cdeuL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x3d2e0c30d0cd9uL,
                            0x3f597686671bbuL,
                            0xaa587eb63999uL,
                            0xe3c7b592c619uL,
                            0x6b2916c05448cuL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x334d10aba913buL,
                            0x45cdb581cfdbuL,
                            0x5e3e0553a8f36uL,
                            0x50bb3041effb2uL,
                            0x4c303f307ff00uL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x403580dd94500uL,
                            0x48df77d92653fuL,
                            0x38a9fe3b349eauL,
                            0xea89850aafe1uL,
                            0x416b151ab706auL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x23bd617b28c85uL,
                            0x6e72ee77d5a61uL,
                            0x1a972ff174ddeuL,
                            0x3e2636373c60fuL,
                            0xd61b8f78b2abuL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0xd7efe9c136b0uL,
                            0x1ab1c89640ad5uL,
                            0x55f82aef41f97uL,
                            0x46957f317ed0duL,
                            0x191a2af74277euL
                        )
                    )
                ),
            ),
            arrayOf(
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x62b434f460efbuL,
                            0x294c6c0fad3fcuL,
                            0x68368937b4c0fuL,
                            0x5c9f82910875buL,
                            0x237e7dbe00545uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x6f74bc53c1431uL,
                            0x1c40e5dbbd9c2uL,
                            0x6c8fb9cae5c97uL,
                            0x4845c5ce1b7dauL,
                            0x7e2e0e450b5ccuL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x575ed6701b430uL,
                            0x4d3e17fa20026uL,
                            0x791fc888c4253uL,
                            0x2f1ba99078ac1uL,
                            0x71afa699b1115uL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x23c1c473b50d6uL,
                            0x3e7671de21d48uL,
                            0x326fa5547a1e8uL,
                            0x50e4dc25fafd9uL,
                            0x731fbc78f89uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x66f9b3953b61duL,
                            0x555f4283cccb9uL,
                            0x7dd67fb1960e7uL,
                            0x14707a1affed4uL,
                            0x21142e9c2b1cuL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0xc71848f81880uL,
                            0x44bd9d8233c86uL,
                            0x6e8578efe5830uL,
                            0x4045b6d7041b5uL,
                            0x4c4d6f3347e15uL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x4ddfc988f1970uL,
                            0x4f6173ea365e1uL,
                            0x645daf9ae4588uL,
                            0x7d43763db623buL,
                            0x38bf9500a88f9uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x7eccfc17d1fc9uL,
                            0x4ca280782831euL,
                            0x7b8337db1d7d6uL,
                            0x5116def3895fbuL,
                            0x193fddaaa7e47uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x2c93c37e8876fuL,
                            0x3431a28c583fauL,
                            0x49049da8bd879uL,
                            0x4b4a8407ac11cuL,
                            0x6a6fb99ebf0d4uL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x122b5b6e423c6uL,
                            0x21e50dff1ddd6uL,
                            0x73d76324e75c0uL,
                            0x588485495418euL,
                            0x136fda9f42c5euL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x6c1bb560855ebuL,
                            0x71f127e13ad48uL,
                            0x5c6b304905aecuL,
                            0x3756b8e889bc7uL,
                            0x75f76914a3189uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x4dfb1a305bdd1uL,
                            0x3b3ff05811f29uL,
                            0x6ed62283cd92euL,
                            0x65d1543ec52e1uL,
                            0x22183510be8duL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x2710143307a7fuL,
                            0x3d88fb48bf3abuL,
                            0x249eb4ec18f7auL,
                            0x136115dff295fuL,
                            0x1387c441fd404uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x766385ead2d14uL,
                            0x194f8b06095euL,
                            0x8478f6823b62uL,
                            0x6018689d37308uL,
                            0x6a071ce17b806uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x3c3d187978af8uL,
                            0x7afe1c88276bauL,
                            0x51df281c8ad68uL,
                            0x64906bda4245duL,
                            0x3171b26aaf1eduL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x5b7d8b28a47d1uL,
                            0x2c2ee149e34c1uL,
                            0x776f5629afc53uL,
                            0x1f4ea50fc49a9uL,
                            0x6c514a6334424uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x7319097564ca8uL,
                            0x1844ebc233525uL,
                            0x21d4543fdeee1uL,
                            0x1ad27aaff1bd2uL,
                            0x221fd4873cf08uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x2204f3a156341uL,
                            0x537414065a464uL,
                            0x43c0c3bedcf83uL,
                            0x5557e706ea620uL,
                            0x48daa596fb924uL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x61d5dc84c9793uL,
                            0x47de83040c29euL,
                            0x189deb26507e7uL,
                            0x4d4e6fadc479auL,
                            0x58c837fa0e8a7uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x28e665ca59cc7uL,
                            0x165c715940dd9uL,
                            0x785f3aa11c95uL,
                            0x57b98d7e38469uL,
                            0x676dd6fccad84uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x1688596fc9058uL,
                            0x66f6ad403619fuL,
                            0x4d759a87772efuL,
                            0x7856e6173bea4uL,
                            0x1c4f73f2c6a57uL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x6706efc7c3484uL,
                            0x6987839ec366duL,
                            0x731f95cf7f26uL,
                            0x3ae758ebce4bcuL,
                            0x70459adb7daf6uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x24fbd305fa0bbuL,
                            0x40a98cc75a1cfuL,
                            0x78ce1220a7533uL,
                            0x6217a10e1c197uL,
                            0x795ac80d1bf64uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x1db4991b42bb3uL,
                            0x469605b994372uL,
                            0x631e3715c9a58uL,
                            0x7e9cfefcf728fuL,
                            0x5fe162848ce21uL
                        )
                    )
                ),
            ),
            arrayOf(
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x1852d5d7cb208uL,
                            0x60d0fbe5ce50fuL,
                            0x5a1e246e37b75uL,
                            0x51aee05ffd590uL,
                            0x2b44c043677dauL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x1214fe194961auL,
                            0xe1ae39a9e9cbuL,
                            0x543c8b526f9f7uL,
                            0x119498067e91duL,
                            0x4789d446fc917uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x487ab074eb78euL,
                            0x1d33b5e8ce343uL,
                            0x13e419feb1b46uL,
                            0x2721f565de6a4uL,
                            0x60c52eef2bb9auL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x3c5c27cae6d11uL,
                            0x36a9491956e05uL,
                            0x124bac9131da6uL,
                            0x3b6f7de202b5duL,
                            0x70d77248d9b66uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x589bc3bfd8bf1uL,
                            0x6f93e6aa3416buL,
                            0x4c0a3d6c1ae48uL,
                            0x55587260b586auL,
                            0x10bc9c312ccfcuL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x2e84b3ec2a05buL,
                            0x69da2f03c1551uL,
                            0x23a174661a67buL,
                            0x209bca289f238uL,
                            0x63755bd3a976fuL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x7101897f1acb7uL,
                            0x3d82cb77b07b8uL,
                            0x684083d7769f5uL,
                            0x52b28472dce07uL,
                            0x2763751737c52uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x7a03e2ad10853uL,
                            0x213dcc6ad36abuL,
                            0x1a6e240d5bdd6uL,
                            0x7c24ffcf8fedfuL,
                            0xd8cc1c48bc16uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x402d36eb419a9uL,
                            0x7cef68c14a052uL,
                            0xf1255bc2d139uL,
                            0x373e7d431186auL,
                            0x70c2dd8a7ad16uL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x4967db8ed7e13uL,
                            0x15aeed02f523auL,
                            0x6149591d094bcuL,
                            0x672f204c17006uL,
                            0x32b8613816a53uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x194509f6fec0euL,
                            0x528d8ca31acacuL,
                            0x7826d73b8b9fauL,
                            0x24acb99e0f9b3uL,
                            0x2e0fac6363948uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x7f7bee448cd64uL,
                            0x4e10f10da0f3cuL,
                            0x3936cb9ab20e9uL,
                            0x7a0fc4fea6cd0uL,
                            0x4179215c735a4uL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x633b9286bcd34uL,
                            0x6cab3badb9c95uL,
                            0x74e387edfbdfauL,
                            0x14313c58a0fd9uL,
                            0x31fa85662241cuL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x94e7d7dced2auL,
                            0x68fa738e118euL,
                            0x41b640a5fee2buL,
                            0x6bb709df019d4uL,
                            0x700344a30cd99uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x26c422e3622f4uL,
                            0xf3066a05b5f0uL,
                            0x4e2448f0480a6uL,
                            0x244cde0dbf095uL,
                            0x24bb2312a9952uL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0xc2af5f85c6buL,
                            0x609f4cf2883fuL,
                            0x6e86eb5a1ca13uL,
                            0x68b44a2efccd1uL,
                            0xd1d2af9ffeb5uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0xed1732de67c3uL,
                            0x308c369291635uL,
                            0x33ef348f2d250uL,
                            0x4475ea1a1bbuL,
                            0xfee3e871e188uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x28aa132621edfuL,
                            0x42b244caf353buL,
                            0x66b064cc2e08auL,
                            0x6bb20020cbdd3uL,
                            0x16acd79718531uL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x1c6c57887b6aduL,
                            0x5abf21fd7592buL,
                            0x50bd41253867auL,
                            0x3800b71273151uL,
                            0x164ed34b18161uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x772af2d9b1d3duL,
                            0x6d486448b4e5buL,
                            0x2ce58dd8d18a8uL,
                            0x1849f67503c8buL,
                            0x123e0ef6b9302uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x6d94c192fe69auL,
                            0x5475222a2690fuL,
                            0x693789d86b8b3uL,
                            0x1f5c3bdfb69dcuL,
                            0x78da0fc61073fuL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x780f1680c3a94uL,
                            0x2a35d3cfcd453uL,
                            0x5e5cdc7ddf8uL,
                            0x6ee888078ac24uL,
                            0x54aa4b316b38uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x15d28e52bc66auL,
                            0x30e1e0351cb7euL,
                            0x30a2f74b11f8cuL,
                            0x39d120cd7de03uL,
                            0x2d25deeb256b1uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x468d19267cb8uL,
                            0x38cdca9b5fbf9uL,
                            0x1bbb05c2ca1e2uL,
                            0x3b015758e9533uL,
                            0x134610a6ab7dauL
                        )
                    )
                ),
            ),
            arrayOf(
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x265e777d1f515uL,
                            0xf1f54c1e39a5uL,
                            0x2f01b95522646uL,
                            0x4fdd8db9dde6duL,
                            0x654878cba97ccuL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x38ec78df6b0feuL,
                            0x13caebea36a22uL,
                            0x5ebc6e54e5f6auL,
                            0x32804903d0eb8uL,
                            0x2102fdba2b20duL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x6e405055ce6a1uL,
                            0x5024a35a532d3uL,
                            0x1f69054daf29duL,
                            0x15d1d0d7a8bd5uL,
                            0xad725db29ecbuL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x7bc0c9b056f85uL,
                            0x51cfebffaffd8uL,
                            0x44abbe94df549uL,
                            0x7ecbbd7e33121uL,
                            0x4f675f5302399uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x267b1834e2457uL,
                            0x6ae19c378bb88uL,
                            0x7457b5ed9d512uL,
                            0x3280d783d05fbuL,
                            0x4aefcffb71a03uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x536360415171euL,
                            0x2313309077865uL,
                            0x251444334afbcuL,
                            0x2b0c3853756e8uL,
                            0xbccbb72a2a86uL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x55e4c50fe1296uL,
                            0x5fdd13efc30duL,
                            0x1c0c6c380e5eeuL,
                            0x3e11de3fb62a8uL,
                            0x6678fd69108f3uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x6962feab1a9c8uL,
                            0x6aca28fb9a30buL,
                            0x56db7ca1b9f98uL,
                            0x39f58497018dduL,
                            0x4024f0ab59d6buL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x6fa31636863c2uL,
                            0x10ae5a67e42b0uL,
                            0x27abbf01fda31uL,
                            0x380a7b9e64fbcuL,
                            0x2d42e2108ead4uL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x17b0d0f537593uL,
                            0x16263c0c9842euL,
                            0x4ab827e4539a4uL,
                            0x6370ddb43d73auL,
                            0x420bf3a79b423uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x5131594dfd29buL,
                            0x3a627e98d52feuL,
                            0x1154041855661uL,
                            0x19175d09f8384uL,
                            0x676b2608b8d2duL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0xba651c5b2b47uL,
                            0x5862363701027uL,
                            0xc4d6c219c6dbuL,
                            0xf03dff8658deuL,
                            0x745d2ffa9c0cfuL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x6df5721d34e6auL,
                            0x4f32f767a0c06uL,
                            0x1d5abeac76e20uL,
                            0x41ce9e104e1e4uL,
                            0x6e15be54c1dcuL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x25a1e2bc9c8bduL,
                            0x104c8f3b037eauL,
                            0x405576fa96c98uL,
                            0x2e86a88e3876fuL,
                            0x1ae23ceb960cfuL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x25d871932994auL,
                            0x6b9d63b560b6euL,
                            0x2df2814c8d472uL,
                            0xfbbee20aa4eduL,
                            0x58ded861278ecuL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x35ba8b6c2c9a8uL,
                            0x1dea58b3185bfuL,
                            0x4b455cd23bbbeuL,
                            0x5ec19c04883f8uL,
                            0x8ba696b531d5uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x73793f266c55cuL,
                            0xb988a9c93b02uL,
                            0x9b0ea32325dbuL,
                            0x37cae71c17c5euL,
                            0x2ff39de85485fuL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x53eeec3efc57auL,
                            0x2fa9fe9022efduL,
                            0x699c72c138154uL,
                            0x72a751ebd1ff8uL,
                            0x120633b4947cfuL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x531474912100auL,
                            0x5afcdf7c0d057uL,
                            0x7a9e71b788deduL,
                            0x5ef708f3b0c88uL,
                            0x7433be3cb393uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x4987891610042uL,
                            0x79d9d7f5d0172uL,
                            0x3c293013b9ec4uL,
                            0xc2b85f39cacauL,
                            0x35d30a99b4d59uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x144c05ce997f4uL,
                            0x4960b8a347fefuL,
                            0x1da11f15d74f7uL,
                            0x54fac19c0feaduL,
                            0x2d873ede7af6duL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x202e14e5df981uL,
                            0x2ea02bc3eb54cuL,
                            0x38875b2883564uL,
                            0x1298c513ae9dduL,
                            0x543618a01600uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x2316443373409uL,
                            0x5de95503b22afuL,
                            0x699201beae2dfuL,
                            0x3db5849ff737auL,
                            0x2e773654707fauL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x2bdf4974c23c1uL,
                            0x4b3b9c8d261bduL,
                            0x26ae8b2a9bc28uL,
                            0x3068210165c51uL,
                            0x4b1443362d079uL
                        )
                    )
                ),
            ),
            arrayOf(
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x454e91c529ccbuL,
                            0x24c98c6bf72cfuL,
                            0x486594c3d89auL,
                            0x7ae13a3d7fa3cuL,
                            0x17038418eaf66uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x4b7c7b66e1f7auL,
                            0x4bea185efd998uL,
                            0x4fabc711055f8uL,
                            0x1fb9f7836fe38uL,
                            0x582f446752da6uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x17bd320324ce4uL,
                            0x51489117898c6uL,
                            0x1684d92a0410buL,
                            0x6e4d90f78c5a7uL,
                            0xc2a1c4bcda28uL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x4814869bd6945uL,
                            0x7b7c391a45db8uL,
                            0x57316ac35b641uL,
                            0x641e31de9096auL,
                            0x5a6a9b30a314duL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x5c7d06f1f0447uL,
                            0x7db70f80b3a49uL,
                            0x6cb4a3ec89a78uL,
                            0x43be8ad81397duL,
                            0x7c558bd1c6f64uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x41524d396463duL,
                            0x1586b449e1a1duL,
                            0x2f17e904aed8auL,
                            0x7e1d2861d3c8euL,
                            0x404a5ca0afbauL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x49e1b2a416fd1uL,
                            0x51c6a0b316c57uL,
                            0x575a59ed71bdcuL,
                            0x74c021a1fec1euL,
                            0x39527516e7f8euL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x740070aa743d6uL,
                            0x16b64cbdd1183uL,
                            0x23f4b7b32eb43uL,
                            0x319aba58235b3uL,
                            0x46395bfdcadd9uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x7db2d1a5d9a9cuL,
                            0x79a200b85422fuL,
                            0x355bfaa71dd16uL,
                            0xb77ea5f78aauL,
                            0x76579a29e822duL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x4b51352b434f2uL,
                            0x1327bd01c2667uL,
                            0x434d73b60c8a1uL,
                            0x3e0daa89443bauL,
                            0x2c514bb2a277uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x68e7e49c02a17uL,
                            0x45795346fe8b6uL,
                            0x89306c8f3546uL,
                            0x6d89f6b2f88f6uL,
                            0x43a384dc9e05buL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x3d5da8bf1b645uL,
                            0x7ded6a96a6d09uL,
                            0x6c3494fee2f4duL,
                            0x2c989c8b6bd4uL,
                            0x1160920961548uL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x5616369b4dcduL,
                            0x4ecab86ac6f47uL,
                            0x3c60085d700b2uL,
                            0x213ee10dfceauL,
                            0x2f637d7491e6euL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x5166929dacfaauL,
                            0x190826b31f689uL,
                            0x4f55567694a7duL,
                            0x705f4f7b1e522uL,
                            0x351e125bc5698uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x49b461af67bbeuL,
                            0x75915712c3a96uL,
                            0x69a67ef580c0duL,
                            0x54d38ef70cffcuL,
                            0x7f182d06e7ce2uL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x54b728e217522uL,
                            0x69a90971b0128uL,
                            0x51a40f2a963a3uL,
                            0x10be9ac12a6bfuL,
                            0x44acc043241c5uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x48e64ab0168ecuL,
                            0x2a2bdb8a86f4fuL,
                            0x7343b6b2d6929uL,
                            0x1d804aa8ce9a3uL,
                            0x67d4ac8c343e9uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x56bbb4f7a5777uL,
                            0x29230627c238fuL,
                            0x5ad1a122cd7fbuL,
                            0xdea56e50e364uL,
                            0x556d1c8312ad7uL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x6756b11be821uL,
                            0x462147e7bb03euL,
                            0x26519743ebfe0uL,
                            0x782fc59682ab5uL,
                            0x97abe38cc8c7uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x740e30c8d3982uL,
                            0x7c2b47f4682fduL,
                            0x5cd91b8c7dc1cuL,
                            0x77fa790f9e583uL,
                            0x746c6c6d1d824uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x1c9877ea52da4uL,
                            0x2b37b83a86189uL,
                            0x733af49310da5uL,
                            0x25e81161c04fbuL,
                            0x577e14a34bee8uL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x6cebebd4dd72buL,
                            0x340c1e442329fuL,
                            0x32347ffd1a93fuL,
                            0x14a89252cbbe0uL,
                            0x705304b8fb009uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x268ac61a73b0auL,
                            0x206f234bebe1cuL,
                            0x5b403a7cbebe8uL,
                            0x7a160f09f4135uL,
                            0x60fa7ee96fd78uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x51d354d296ec6uL,
                            0x7cbf5a63b16c7uL,
                            0x2f50bb3cf0c14uL,
                            0x1feb385cac65auL,
                            0x21398e0ca1635uL
                        )
                    )
                ),
            ),
            arrayOf(
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0xaaf9b4b75601uL,
                            0x26b91b5ae44f3uL,
                            0x6de808d7ab1c8uL,
                            0x6a769675530b0uL,
                            0x1bbfb284e98f7uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x5058a382b33f3uL,
                            0x175a91816913euL,
                            0x4f6cdb96b8ae8uL,
                            0x17347c9da81d2uL,
                            0x5aa3ed9d95a23uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x777e9c7d96561uL,
                            0x28e58f006ccacuL,
                            0x541bbbb2cac49uL,
                            0x3e63282994cecuL,
                            0x4a07e14e5e895uL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x358cdc477a49buL,
                            0x3cc88fe02e481uL,
                            0x721aab7f4e36buL,
                            0x408cc9469953uL,
                            0x50af7aed84afauL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x412cb980df999uL,
                            0x5e78dd8ee29dcuL,
                            0x171dff68c575duL,
                            0x2015dd2f6ef49uL,
                            0x3f0bac391d313uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x7de0115f65be5uL,
                            0x4242c21364dc9uL,
                            0x6b75b64a66098uL,
                            0x33c0102c085uL,
                            0x1921a316baebduL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x2ad9ad9f3c18buL,
                            0x5ec1638339aebuL,
                            0x5703b6559a83buL,
                            0x3fa9f4d05d612uL,
                            0x7b049deca062cuL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x22f7edfb870fcuL,
                            0x569eed677b128uL,
                            0x30937dcb0a5afuL,
                            0x758039c78ea1buL,
                            0x6458df41e273auL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x3e37a35444483uL,
                            0x661fdb7d27b99uL,
                            0x317761dd621e4uL,
                            0x7323c30026189uL,
                            0x6093dccbc2950uL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x6eebe6084034buL,
                            0x6cf01f70a8d7buL,
                            0xb41a54c6670auL,
                            0x6c84b99bb55dbuL,
                            0x6e3180c98b647uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x39a8585e0706duL,
                            0x3167ce72663feuL,
                            0x63d14ecdb4297uL,
                            0x4be21dcf970b8uL,
                            0x57d1ea084827auL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x2b6e7a128b071uL,
                            0x5b27511755dcfuL,
                            0x8584c2930565uL,
                            0x68c7bda6f4159uL,
                            0x363e999ddd97buL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x48dce24baec6uL,
                            0x2b75795ec05e3uL,
                            0x3bfa4c5da6dc9uL,
                            0x1aac8659e371euL,
                            0x231f979bc6f9buL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x43c135ee1fc4uL,
                            0x2a11c9919f2d5uL,
                            0x6334cc25dbacduL,
                            0x295da17b400dauL,
                            0x48ee9b78693a0uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x1de4bcc2af3c6uL,
                            0x61fc411a3eb86uL,
                            0x53ed19ac12ec0uL,
                            0x209dbc6b804e0uL,
                            0x79bfa9b08792uL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x1ed80a2d54245uL,
                            0x70efec72a5e79uL,
                            0x42151d42a822duL,
                            0x1b5ebb6d631e8uL,
                            0x1ef4fb1594706uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x3a51da300df4uL,
                            0x467b52b561c72uL,
                            0x4d5920210e590uL,
                            0xca769e789685uL,
                            0x38c77f684817uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x65ee65b167becuL,
                            0x52da19b850a9uL,
                            0x408665656429uL,
                            0x7ab39596f9a4cuL,
                            0x575ee92a4a0bfuL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x6bc450aa4d801uL,
                            0x4f4a6773b0ba8uL,
                            0x6241b0b0ebc48uL,
                            0x40d9c4f1d9315uL,
                            0x200a1e7e382f5uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x80908a182fcfuL,
                            0x532913b7ba98uL,
                            0x3dccf78c385c3uL,
                            0x68002dd5eaba9uL,
                            0x43d4e7112cd3fuL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x5b967eaf93ac5uL,
                            0x360acca580a31uL,
                            0x1c65fd5c6f262uL,
                            0x71c7f15c2ecabuL,
                            0x50eca52651e4uL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x4397660e668eauL,
                            0x7c2a75692f2f5uL,
                            0x3b29e7e6c66efuL,
                            0x72ba658bcda9auL,
                            0x6151c09fa131auL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x31ade453f0c9cuL,
                            0x3dfee07737868uL,
                            0x611ecf7a7d411uL,
                            0x2637e6cbd64f6uL,
                            0x4b0ee6c21c58fuL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x55c0dfdf05d96uL,
                            0x405569dcf475euL,
                            0x5c5c277498bbuL,
                            0x18588d95dc389uL,
                            0x1fef24fa800f0uL
                        )
                    )
                ),
            ),
            arrayOf(
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x2aff530976b86uL,
                            0xd85a48c0845auL,
                            0x796eb963642e0uL,
                            0x60bee50c4b626uL,
                            0x28005fe6c8340uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x653fb1aa73196uL,
                            0x607faec8306fauL,
                            0x4e85ec83e5254uL,
                            0x9f56900584fduL,
                            0x544d49292fc86uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x7ba9f34528688uL,
                            0x284a20fb42d5duL,
                            0x3652cd9706ffeuL,
                            0x6fd7baddde6b3uL,
                            0x72e472930f316uL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x3f635d32a7627uL,
                            0xcbecacde00feuL,
                            0x3411141eaa936uL,
                            0x21c1e42f3cb94uL,
                            0x1fee7f000fe06uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x5208c9781084fuL,
                            0x16468a1dc24d2uL,
                            0x7bf780ac540a8uL,
                            0x1a67eced75301uL,
                            0x5a9d2e8c2733auL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x305da03dbf7e5uL,
                            0x1228699b7aecauL,
                            0x12a23b2936bc9uL,
                            0x2a1bda56ae6e9uL,
                            0xf94051ee040uL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x793bb07af9753uL,
                            0x1e7b6ecd4fafduL,
                            0x2c7b1560fb43uL,
                            0x2296734cc5fb7uL,
                            0x47b7ffd25dd40uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x56b23c3d330b2uL,
                            0x37608e360d1a6uL,
                            0x10ae0f3c8722euL,
                            0x86d9b618b637uL,
                            0x7d79c7e8beabuL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x3fb9cbc08dd12uL,
                            0x75c3dd85370ffuL,
                            0x47f06fe2819acuL,
                            0x5db06ab9215eduL,
                            0x1c3520a35ea64uL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x6f40216bc059uL,
                            0x3a2579b0fd9b5uL,
                            0x71c26407eec8cuL,
                            0x72ada4ab54f0buL,
                            0x38750c3b66d12uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x253a6bccba34auL,
                            0x427070433701auL,
                            0x20b8e58f9870euL,
                            0x337c861db00ccuL,
                            0x1c3d05775d0eeuL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x6f1409422e51auL,
                            0x7856bbece2d25uL,
                            0x13380a72f031cuL,
                            0x43e1080a7f3bauL,
                            0x621e2c7d3304uL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x61796b0dbf0f3uL,
                            0x73c2f9c32d6f5uL,
                            0x6aa8ed1537ebeuL,
                            0x74e92c91838f4uL,
                            0x5d8e589ca1002uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x60cc8259838duL,
                            0x38d3f35b95f3uL,
                            0x56078c243a923uL,
                            0x2de3293241bb2uL,
                            0x7d6097bd3auL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x71d950842a94buL,
                            0x46b11e5c7d817uL,
                            0x5478bbecb4f0duL,
                            0x7c3054b0a1c5duL,
                            0x1583d7783c1cbuL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x34704cc9d28c7uL,
                            0x3dee598b1f200uL,
                            0x16e1c98746d9euL,
                            0x4050b7095afdfuL,
                            0x4958064e83c55uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x6a2ef5da27ae1uL,
                            0x28aace02e9d9duL,
                            0x2459e965f0e8uL,
                            0x7b864d3150933uL,
                            0x252a5f2e81ed8uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x94265066e80duL,
                            0xa60f918d61a5uL,
                            0x444bf7f30fdeuL,
                            0x1c40da9ed3c06uL,
                            0x79c170bd843buL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x6cd50c0d5d056uL,
                            0x5b7606ae779bauL,
                            0x70fbd226bdda1uL,
                            0x5661e53391ff9uL,
                            0x6768c0d7317b8uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x6ece464fa6fffuL,
                            0x3cc40bca460a0uL,
                            0x6e3a90afb8d0cuL,
                            0x5801abca11228uL,
                            0x6dec05e34ac9fuL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x625e5f155c1b3uL,
                            0x4f32f6f723296uL,
                            0x5ac980105efceuL,
                            0x17a61165eee36uL,
                            0x51445e14ddcd5uL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x147ab2bbea455uL,
                            0x1f240f2253126uL,
                            0xc3de9e314e89uL,
                            0x21ea5a4fca45fuL,
                            0x12e990086e4fduL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x2b4b3b144951uL,
                            0x5688977966aeauL,
                            0x18e176e399ffduL,
                            0x2e45c5eb4938buL,
                            0x13186f31e3929uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x496b37fdfbb2euL,
                            0x3c2439d5f3e21uL,
                            0x16e60fe7e6a4duL,
                            0x4d7ef889b621duL,
                            0x77b2e3f05d3e9uL
                        )
                    )
                ),
            ),
            arrayOf(
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x639c12ddb0a4uL,
                            0x6180490cd7ab3uL,
                            0x3f3918297467cuL,
                            0x74568be1781acuL,
                            0x7a195152e095uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x7a9c59c2ec4deuL,
                            0x7e9f09e79652duL,
                            0x6a3e422f22d86uL,
                            0x2ae8e3b836c8buL,
                            0x63b795fc7ad32uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x68f02389e5fc8uL,
                            0x59f1bc877506uL,
                            0x504990e410cecuL,
                            0x9bd7d0feaee2uL,
                            0x3e8fe83d032f0uL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x4c8de8efd13cuL,
                            0x1c67c06e6210euL,
                            0x183378f7f146auL,
                            0x64352ceaed289uL,
                            0x22d60899a6258uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x315b90570a294uL,
                            0x60ce108a925f1uL,
                            0x6eff61253c909uL,
                            0x3ef0e2d70b0uL,
                            0x75ba3b797fac4uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x1dbc070cdd196uL,
                            0x16d8fb1534c47uL,
                            0x500498183fa2auL,
                            0x72f59c423de75uL,
                            0x904d07b87779uL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x22d6648f940b9uL,
                            0x197a5a1873e86uL,
                            0x207e4c41a54bcuL,
                            0x5360b3b4bd6d0uL,
                            0x6240aacebaf72uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x61fd4ddba919cuL,
                            0x7d8e991b55699uL,
                            0x61b31473cc76cuL,
                            0x7039631e631d6uL,
                            0x43e2143fbc1dduL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x4749c5ba295a0uL,
                            0x37946fa4b5f06uL,
                            0x724c5ab5a51f1uL,
                            0x65633789dd3f3uL,
                            0x56bdaf238db40uL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0xd36cc19d3bb2uL,
                            0x6ec4470d72262uL,
                            0x6853d7018a9aeuL,
                            0x3aa3e4dc2c8ebuL,
                            0x3aa31507e1e5uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x2b9e3f53533ebuL,
                            0x2add727a806c5uL,
                            0x56955c8ce15a3uL,
                            0x18c4f070a290euL,
                            0x1d24a86d83741uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x47648ffd4ce1fuL,
                            0x60a9591839e9duL,
                            0x424d5f38117abuL,
                            0x42cc46912c10euL,
                            0x43b261dc9aeb4uL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x13d8b6c951364uL,
                            0x4c0017e8f632auL,
                            0x53e559e53f9c4uL,
                            0x4b20146886eeauL,
                            0x2b4d5e242940uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x31e1988bb79bbuL,
                            0x7b82f46b3bcabuL,
                            0xf7a8ce827b41uL,
                            0x5e15816177130uL,
                            0x326055cf5b276uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x155cb28d18df2uL,
                            0xc30d9ca11694uL,
                            0x2090e27ab3119uL,
                            0x208624e7a49b6uL,
                            0x27a6c809ae5d3uL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x4270ac43d6954uL,
                            0x2ed4cd95659a5uL,
                            0x75c0db37528f9uL,
                            0x2ccbcfd2c9234uL,
                            0x221503603d8c2uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x6ebcd1f0db188uL,
                            0x74ceb4b7d1174uL,
                            0x7d56168df4f5cuL,
                            0xbf79176fd18auL,
                            0x2cb67174ff60auL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x6cdf9390be1d0uL,
                            0x8e519c7e2b3duL,
                            0x253c3d2a50881uL,
                            0x21b41448e333duL,
                            0x7b1df4b73890fuL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x6221807f8f58cuL,
                            0x3fa92813a8be5uL,
                            0x6da98c38d5572uL,
                            0x1ed95554468fuL,
                            0x68698245d352euL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x2f2e0b3b2a224uL,
                            0xc56aa22c1c92uL,
                            0x5fdec39f1b278uL,
                            0x4c90af5c7f106uL,
                            0x61fcef2658fc5uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x15d852a18187auL,
                            0x270dbb59afb76uL,
                            0x7db120bcf92abuL,
                            0xe7a25d714087uL,
                            0x46cf4c473daf0uL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x46ea7f1498140uL,
                            0x70725690a8427uL,
                            0xa73ae9f079fbuL,
                            0x2dd924461c62buL,
                            0x1065aae50d8ccuL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x525ed9ec4e5f9uL,
                            0x22d20660684cuL,
                            0x7972b70397b68uL,
                            0x7a03958d3f965uL,
                            0x29387bcd14eb5uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x44525df200d57uL,
                            0x2d7f94ce94385uL,
                            0x60d00c170ecb7uL,
                            0x38b0503f3d8f0uL,
                            0x69a198e64f1ceuL
                        )
                    )
                ),
            ),
            arrayOf(
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x14434dcc5caeduL,
                            0x2c7909f667c20uL,
                            0x61a839d1fb576uL,
                            0x4f23800cabb76uL,
                            0x25b2697bd267fuL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x2b2e0d91a78bcuL,
                            0x3990a12ccf20cuL,
                            0x141c2e11f2622uL,
                            0xdfcefaa53320uL,
                            0x7369e6a92493auL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x73ffb13986864uL,
                            0x3282bb8f713acuL,
                            0x49ced78f297efuL,
                            0x6697027661defuL,
                            0x1420683db54e4uL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x6bb6fc1cc5ad0uL,
                            0x532c8d591669duL,
                            0x1af794da86c33uL,
                            0xe0e9d86d24d3uL,
                            0x31e83b4161d08uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0xbd1e249dd197uL,
                            0xbcb1820568fuL,
                            0x2eab1718830d4uL,
                            0x396fd816997e6uL,
                            0x60b63bebf508auL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0xc7129e062b4fuL,
                            0x1e526415b12fduL,
                            0x461a0fd27923duL,
                            0x18badf670a5b7uL,
                            0x55cf1eb62d550uL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x6b5e37df58c52uL,
                            0x3bcf33986c60euL,
                            0x44fb8835ceae7uL,
                            0x99dec18e71a4uL,
                            0x1a56fbaa62ba0uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x1101065c23d58uL,
                            0x5aa1290338b0fuL,
                            0x3157e9e2e7421uL,
                            0xea712017d489uL,
                            0x669a656457089uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x66b505c9dc9ecuL,
                            0x774ef86e35287uL,
                            0x4d1d944c0955euL,
                            0x52e4c39d72b20uL,
                            0x13c4836799c58uL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x4fb6a5d8bd080uL,
                            0x58ae34908589buL,
                            0x3954d977baf13uL,
                            0x413ea597441dcuL,
                            0x50bdc87dc8e5buL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x25d465ab3e1b9uL,
                            0xf8fe27ec2847uL,
                            0x2d6e6dbf04f06uL,
                            0x3038cfc1b3276uL,
                            0x66f80c93a637buL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x537836edfe111uL,
                            0x2be02357b2c0duL,
                            0x6dcee58c8d4f8uL,
                            0x2d732581d6192uL,
                            0x1dd56444725fduL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x7e60008bac89auL,
                            0x23d5c387c1852uL,
                            0x79e5df1f533a8uL,
                            0x2e6f9f1c5f0cfuL,
                            0x3a3a450f63a30uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x47ff83362127duL,
                            0x8e39af82b1f4uL,
                            0x488322ef27dabuL,
                            0x1973738a2a1a4uL,
                            0xe645912219f7uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x72f31d8394627uL,
                            0x7bd294a200f1uL,
                            0x665be00e274c6uL,
                            0x43de8f1b6368buL,
                            0x318c8d9393a9auL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x69e29ab1dd398uL,
                            0x30685b3c76bacuL,
                            0x565cf37f24859uL,
                            0x57b2ac28efef9uL,
                            0x509a41c325950uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x45d032afffe19uL,
                            0x12fe49b6cde4euL,
                            0x21663bc327cf1uL,
                            0x18a5e4c69f1dduL,
                            0x224c7c679a1d5uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x6edca6f925e9uL,
                            0x68c8363e677b8uL,
                            0x60cfa25e4fbcfuL,
                            0x1c4c17609404euL,
                            0x5bff02328a11uL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x1a0dd0dc512e4uL,
                            0x10894bf5fcd10uL,
                            0x52949013f9c37uL,
                            0x1f50fba4735c7uL,
                            0x576277cdee01auL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x2137023cae00buL,
                            0x15a3599eb26c6uL,
                            0x687221512b3cuL,
                            0x253cb3a0824e9uL,
                            0x780b8cc3fa2a4uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x38abc234f305fuL,
                            0x7a280bbc103deuL,
                            0x398a836695dfeuL,
                            0x3d0af41528a1auL,
                            0x5ff418726271buL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x347e813b69540uL,
                            0x76864c21c3cbbuL,
                            0x1e049dbcd74a8uL,
                            0x5b4d60f93749cuL,
                            0x29d4db8ca0a0cuL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x6080c1789db9duL,
                            0x4be7cef1ea731uL,
                            0x2f40d769d8080uL,
                            0x35f7d4c44a603uL,
                            0x106a03dc25a96uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x50aaf333353d0uL,
                            0x4b59a613cbb35uL,
                            0x223dfc0e19a76uL,
                            0x77d1e2bb2c564uL,
                            0x4ab38a51052cbuL
                        )
                    )
                ),
            ),
            arrayOf(
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x7d1ef5fddc09cuL,
                            0x7beeaebb9dad9uL,
                            0x58d30ba0acfbuL,
                            0x5cd92eab5ae90uL,
                            0x3041c6bb04ed2uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x42b256768d593uL,
                            0x2e88459427b4fuL,
                            0x2b3876630701uL,
                            0x34878d405eae5uL,
                            0x29cdd1adc088auL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x2f2f9d956e148uL,
                            0x6b3e6ad65c1feuL,
                            0x5b00972b79e5duL,
                            0x53d8d234c5dafuL,
                            0x104bbd6814049uL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x59a5fd67ff163uL,
                            0x3a998ead0352buL,
                            0x83c95fa4af9auL,
                            0x6fadbfc01266fuL,
                            0x204f2a20fb072uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0xfd3168f1ed67uL,
                            0x1bb0de7784a3euL,
                            0x34bcb78b20477uL,
                            0xa4a26e2e2182uL,
                            0x5be8cc57092a7uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x43b3d30ebb079uL,
                            0x357aca5c61902uL,
                            0x5b570c5d62455uL,
                            0x30fb29e1e18c7uL,
                            0x2570fb17c2791uL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x6a9550bb8245auL,
                            0x511f20a1a2325uL,
                            0x29324d7239beeuL,
                            0x3343cc37516c4uL,
                            0x241c5f91de018uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x2367f2cb61575uL,
                            0x6c39ac04d87dfuL,
                            0x6d4958bd7e5bduL,
                            0x566f4638a1532uL,
                            0x3dcb65ea53030uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x172940de6caauL,
                            0x6045b2e67451buL,
                            0x56c07463efcb3uL,
                            0x728b6bfe6e91uL,
                            0x8420edd5fcdfuL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0xc34e04f410ceuL,
                            0x344edc0d0a06buL,
                            0x6e45486d84d6duL,
                            0x44e2ecb3863f5uL,
                            0x4d654f321db8uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x720ab8362fa4auL,
                            0x29c4347cdd9bfuL,
                            0xe798ad5f8463uL,
                            0x4fef18bcb0bfeuL,
                            0xd9a53efbc176uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x5c116ddbdb5d5uL,
                            0x6d1b4bba5abcfuL,
                            0x4d28a48a5537auL,
                            0x56b8e5b040b99uL,
                            0x4a7a4f2618991uL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x3b291af372a4buL,
                            0x60e3028fe4498uL,
                            0x2267bca4f6a09uL,
                            0x719eec242b243uL,
                            0x4a96314223e0euL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x718025fb15f95uL,
                            0x68d6b8371fe94uL,
                            0x3804448f7d97cuL,
                            0x42466fe784280uL,
                            0x11b50c4cddd31uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x274408a4ffd6uL,
                            0x7d382aedb34dduL,
                            0x40acfc9ce385duL,
                            0x628bb99a45b1euL,
                            0x4f4bce4dce6bcuL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x2616ec49d0b6fuL,
                            0x1f95d8462e61cuL,
                            0x1ad3e9b9159c6uL,
                            0x79ba475a04df9uL,
                            0x3042cee561595uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x7ce5ae2242584uL,
                            0x2d25eb153d4e3uL,
                            0x3a8f3d09ba9c9uL,
                            0xf3690d04eb8euL,
                            0x73fcdd14b71c0uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x67079449bac41uL,
                            0x5b79c4621484fuL,
                            0x61069f2156b8duL,
                            0xeb26573b10afuL,
                            0x389e740c9a9ceuL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x578f6570eac28uL,
                            0x644f2339c3937uL,
                            0x66e47b7956c2cuL,
                            0x34832fe1f55d0uL,
                            0x25c425e5d6263uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x4b3ae34dcb9ceuL,
                            0x47c691a15ac9fuL,
                            0x318e06e5d400cuL,
                            0x3c422d9f83eb1uL,
                            0x61545379465a6uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x606a6f1d7de6euL,
                            0x4f1c0c46107e7uL,
                            0x229b1dcfbe5d8uL,
                            0x3acc60a7b1327uL,
                            0x6539a08915484uL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x4dbd414bb4a19uL,
                            0x7930849f1dbb8uL,
                            0x329c5a466caf0uL,
                            0x6c824544feb9buL,
                            0xf65320ef019buL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x21f74c3d2f773uL,
                            0x24b88d08bd3auL,
                            0x6e678cf054151uL,
                            0x43631272e747cuL,
                            0x11c5e4aac5cd1uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x6d1b1cafde0c6uL,
                            0x462c76a303a90uL,
                            0x3ca4e693cff9buL,
                            0x3952cd45786fduL,
                            0x4cabc7bdec330uL
                        )
                    )
                ),
            ),
            arrayOf(
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x7788f3f78d289uL,
                            0x5942809b3f811uL,
                            0x5973277f8c29cuL,
                            0x10f93bc5fe67uL,
                            0x7ee498165acb2uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x69624089c0a2euL,
                            0x75fc8e70473uL,
                            0x13e84ab1d2313uL,
                            0x2c10bedf6953buL,
                            0x639b93f0321c8uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x508e39111a1c3uL,
                            0x290120e912f7auL,
                            0x1cbf464acae43uL,
                            0x15373e9576157uL,
                            0xedf493c85b60uL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x7c4d284764113uL,
                            0x7fefebf06acecuL,
                            0x39afb7a824100uL,
                            0x1b48e47e7fd65uL,
                            0x4c00c54d1dfauL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x48158599b5a68uL,
                            0x1fd75bc41d5d9uL,
                            0x2d9fc1fa95d3cuL,
                            0x7da27f20eba11uL,
                            0x403b92e3019d4uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x22f818b465cf8uL,
                            0x342901dff09b8uL,
                            0x31f595dc683cduL,
                            0x37a57745fd682uL,
                            0x355bb12ab2617uL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x1dac75a8c7318uL,
                            0x3b679d5423460uL,
                            0x6b8fcb7b6400euL,
                            0x6c73783be5f9duL,
                            0x7518eaf8e052auL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x664cc7493bbf4uL,
                            0x33d94761874e3uL,
                            0x179e1796f613uL,
                            0x1890535e2867duL,
                            0xf9b8132182ecuL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x59c41b7f6c32uL,
                            0x79e8706531491uL,
                            0x6c747643cb582uL,
                            0x2e20c0ad494e4uL,
                            0x47c3871bbb175uL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x65d50c85066b0uL,
                            0x6167453361f7cuL,
                            0x6ba3818bb312uL,
                            0x6aff29baa7522uL,
                            0x8fea02ce8d48uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x4539771ec4f48uL,
                            0x7b9318badca28uL,
                            0x70f19afe016c5uL,
                            0x4ee7bb1608d23uL,
                            0xb89b8576469uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x5dd7668deead0uL,
                            0x4096d0ba47049uL,
                            0x6275997219114uL,
                            0x29bda8a67e6aeuL,
                            0x473829a74f75duL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x1533aad3902c9uL,
                            0x1dde06b11e47buL,
                            0x784bed1930b77uL,
                            0x1c80a92b9c867uL,
                            0x6c668b4d44e4duL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x2da754679c418uL,
                            0x3164c31be105auL,
                            0x11fac2b98ef5fuL,
                            0x35a1aaf779256uL,
                            0x2078684c4833cuL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0xcf217a78820cuL,
                            0x65024e7d2e769uL,
                            0x23bb5efdda82auL,
                            0x19fd4b632d3c6uL,
                            0x7411a6054f8a4uL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x2e53d18b175b4uL,
                            0x33e7254204af3uL,
                            0x3bcd7d5a1c4c5uL,
                            0x4c7c22af65d0fuL,
                            0x1ec9a872458c3uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x59d32b99dc86duL,
                            0x6ac075e22a9acuL,
                            0x30b9220113371uL,
                            0x27fd9a638966euL,
                            0x7c136574fb813uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x6a4d400a2509buL,
                            0x41791056971cuL,
                            0x655d5866e075cuL,
                            0x2302bf3e64df8uL,
                            0x3add88a5c7cd6uL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x298d459393046uL,
                            0x30bfecb3d90b8uL,
                            0x3d9b8ea3df8d6uL,
                            0x3900e96511579uL,
                            0x61ba1131a406auL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x15770b635dcf2uL,
                            0x59ecd83f79571uL,
                            0x2db461c0b7fbduL,
                            0x73a42a981345fuL,
                            0x249929fccc879uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0xa0f116959029uL,
                            0x5974fd7b1347auL,
                            0x1e0cc1c08edaduL,
                            0x673bdf8ad1f13uL,
                            0x5620310cbbd8euL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x6b5f477e285d6uL,
                            0x4ed91ec326cc8uL,
                            0x6d6537503a3fduL,
                            0x626d3763988d5uL,
                            0x7ec846f3658ceuL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x193434934d643uL,
                            0xd4a2445eaa51uL,
                            0x7d0708ae76fe0uL,
                            0x39847b6c3c7e1uL,
                            0x37676a2a4d9d9uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x68f3f1da22ec7uL,
                            0x6ed8039a2736buL,
                            0x2627ee04c3c75uL,
                            0x6ea90a647e7d1uL,
                            0x6daaf723399b9uL
                        )
                    )
                ),
            ),
            arrayOf(
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x304bfacad8ea2uL,
                            0x502917d108b07uL,
                            0x43176ca6dd0fuL,
                            0x5d5158f2c1d84uL,
                            0x2b5449e58eb3buL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x27562eb3dbe47uL,
                            0x291d7b4170be7uL,
                            0x5d1ca67dfa8e1uL,
                            0x2a88061f298a2uL,
                            0x1304e9e71627duL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x14d26adc9cfeuL,
                            0x7f1691ba16f13uL,
                            0x5e71828f06eacuL,
                            0x349ed07f0fffcuL,
                            0x4468de2d7c2dduL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x2d8c6f86307ceuL,
                            0x6286ba1850973uL,
                            0x5e9dcb08444d4uL,
                            0x1a96a543362b2uL,
                            0x5da6427e63247uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x3355e9419469euL,
                            0x1847bb8ea8a37uL,
                            0x1fe6588cf9b71uL,
                            0x6b1c9d2db6b22uL,
                            0x6cce7c6ffb44buL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x4c688deac22cauL,
                            0x6f775c3ff0352uL,
                            0x565603ee419bbuL,
                            0x6544456c61c46uL,
                            0x58f29abfe79f2uL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x264bf710ecdf6uL,
                            0x708c58527896buL,
                            0x42ceae6c53394uL,
                            0x4381b21e82b6auL,
                            0x6af93724185b4uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x6cfab8de73e68uL,
                            0x3e6efced4bd21uL,
                            0x56609500dbeuL,
                            0x71b7824ad85dfuL,
                            0x577629c4a7f41uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x24509c6a888uL,
                            0x2696ab12e6644uL,
                            0xcca27f4b80d8uL,
                            0xc7c1f11b119euL,
                            0x701f25bb0caecuL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0xf6d97cbec113uL,
                            0x4ce97fb7c93a3uL,
                            0x139835a11281buL,
                            0x728907ada9156uL,
                            0x720a5bc050955uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0xb0f8e4616ceduL,
                            0x1d3c4b50fb875uL,
                            0x2f29673dc0198uL,
                            0x5f4b0f1830ffauL,
                            0x2e0c92bfbdc40uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x709439b805a35uL,
                            0x6ec48557f8187uL,
                            0x8a4d1ba13a2cuL,
                            0x76348a0bf9aeuL,
                            0xe9b9cbb144efuL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x69bd55db1beeeuL,
                            0x6e14e47f731bduL,
                            0x1a35e47270eacuL,
                            0x66f225478df8euL,
                            0x366d44191cfd3uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x2d48ffb5720aduL,
                            0x57b7f21a1df77uL,
                            0x5550effba0645uL,
                            0x5ec6a4098a931uL,
                            0x221104eb3f337uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x41743f2bc8c14uL,
                            0x796b0ad8773c7uL,
                            0x29fee5cbb689buL,
                            0x122665c178734uL,
                            0x4167a4e6bc593uL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x62665f8ce8feeuL,
                            0x29d101ac59857uL,
                            0x4d93bbba59ffcuL,
                            0x17b7897373f17uL,
                            0x34b33370cb7eduL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x39d2876f62700uL,
                            0x1cecd1d6c87uL,
                            0x7f01a11747675uL,
                            0x2350da5a18190uL,
                            0x7938bb7e22552uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x591ee8681d6ccuL,
                            0x39db0b4ea79b8uL,
                            0x202220f380842uL,
                            0x2f276ba42e0acuL,
                            0x1176fc6e2dfe6uL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0xe28949770eb8uL,
                            0x5559e88147b72uL,
                            0x35e1e6e63ef30uL,
                            0x35b109aa7ff6fuL,
                            0x1f6a3e54f2690uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x76cd05b9c619buL,
                            0x69654b0901695uL,
                            0x7a53710b77f27uL,
                            0x79a1ea7d28175uL,
                            0x8fc3a4c677d5uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x4c199d30734eauL,
                            0x6c622cb9acc14uL,
                            0x5660a55030216uL,
                            0x68f1199f11fbuL,
                            0x4f2fad0116b90uL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x4d91db73bb638uL,
                            0x55f82538112c5uL,
                            0x6d85a279815deuL,
                            0x740b7b0cd9cf9uL,
                            0x3451995f2944euL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x6b24194ae4e54uL,
                            0x2230afded8897uL,
                            0x23412617d5071uL,
                            0x3d5d30f35969buL,
                            0x445484a4972efuL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x2fcd09fea7d7cuL,
                            0x296126b9ed22auL,
                            0x4a171012a05b2uL,
                            0x1db92c74d5523uL,
                            0x10b89ca604289uL
                        )
                    )
                ),
            ),
            arrayOf(
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x141be5a45f06euL,
                            0x5adb38becaea7uL,
                            0x3fd46db41f2bbuL,
                            0x6d488bbb5ce39uL,
                            0x17d2d1d9ef0d4uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x147499718289cuL,
                            0xa48a67e4c7abuL,
                            0x30fbc544bafe3uL,
                            0xc701315fe58auL,
                            0x20b878d577b75uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x2af18073f3e6auL,
                            0x33aea420d24feuL,
                            0x298008bf4ff94uL,
                            0x3539171db961euL,
                            0x72214f63cc65cuL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x5b7b9f43b29c9uL,
                            0x149ea31eea3b3uL,
                            0x4be7713581609uL,
                            0x2d87960395e98uL,
                            0x1f24ac855a154uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x37f405307a693uL,
                            0x2e5e66cf2b69cuL,
                            0x5d84266ae9c53uL,
                            0x5e4eb7de853b9uL,
                            0x5fdf48c58171cuL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x608328e9505aauL,
                            0x22182841dc49auL,
                            0x3ec96891d2307uL,
                            0x2f363fff22e03uL,
                            0xba739e2ae39uL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x426f5ea88bb26uL,
                            0x33092e77f75c8uL,
                            0x1a53940d819e7uL,
                            0x1132e4f818613uL,
                            0x72297de7d518duL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x698de5c8790d6uL,
                            0x268b8545beb25uL,
                            0x6d2648b96fedfuL,
                            0x47988ad1db07cuL,
                            0x3283a3e67ad7uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x41dc7be0cb939uL,
                            0x1b16c66100904uL,
                            0xa24c20cbc66duL,
                            0x4a2e9efe48681uL,
                            0x5e1296846271uL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x7bbc8242c4550uL,
                            0x59a06103b35b7uL,
                            0x7237e4af32033uL,
                            0x726421ab3537auL,
                            0x78cf25d38258cuL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x2eeb32d9c495auL,
                            0x79e25772f9750uL,
                            0x6d747833bbf23uL,
                            0x6cdd816d5d749uL,
                            0x39c00c9c13698uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x66b8e31489d68uL,
                            0x573857e10e2b5uL,
                            0x13be816aa1472uL,
                            0x41964d3ad4bf8uL,
                            0x6b52076b3ffuL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x37e16b9ce082duL,
                            0x1882f57853eb9uL,
                            0x7d29eacd01fc5uL,
                            0x2e76a59b5e715uL,
                            0x7de2e9561a9f7uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0xcfe19d95781cuL,
                            0x312cc621c453cuL,
                            0x145ace6da077cuL,
                            0x912bef9ce9b8uL,
                            0x4d57e3443bc76uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0xd4f4b6a55ecbuL,
                            0x7ebb0bb733bceuL,
                            0x7ba6a05200549uL,
                            0x4f6ede4e22069uL,
                            0x6b2a90af1a602uL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x3f3245bb2d80auL,
                            0xe5f720f36efduL,
                            0x3b9cccf60c06duL,
                            0x84e323f37926uL,
                            0x465812c8276c2uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x3f4fc9ae61e97uL,
                            0x3bc07ebfa2d24uL,
                            0x3b744b55cd4a0uL,
                            0x72553b25721f3uL,
                            0x5fd8f4e9d12d3uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x3beb22a1062d9uL,
                            0x6a7063b82c9a8uL,
                            0xa5a35dc197eduL,
                            0x3c80c06a53defuL,
                            0x5b32c2b1cb16uL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x4a42c7ad58195uL,
                            0x5c8667e799effuL,
                            0x2e5e74c850a1uL,
                            0x3f0db614e869auL,
                            0x31771a4856730uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x5eccd24da8fduL,
                            0x580bbfdf07918uL,
                            0x7e73586873c6auL,
                            0x74ceddf77f93euL,
                            0x3b5556a37b471uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0xc524e14dd482uL,
                            0x283457496c656uL,
                            0xad6bcfb6cd45uL,
                            0x375d1e8b02414uL,
                            0x4fc079d27a733uL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x48b440c86c50duL,
                            0x139929cca3b86uL,
                            0xf8f2e44cdf2fuL,
                            0x68432117ba6b2uL,
                            0x241170c2bae3cuL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x138b089bf2f7fuL,
                            0x4a05bfd34ea39uL,
                            0x203914c925ef5uL,
                            0x7497fffe04e3cuL,
                            0x124567cecaf98uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x1ab860ac473b4uL,
                            0x5c0227c86a7ffuL,
                            0x71b12bfc24477uL,
                            0x6a573a83075uL,
                            0x3f8612966c870uL
                        )
                    )
                ),
            ),
            arrayOf(
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0xfcfa36048d13uL,
                            0x66e7133bbb383uL,
                            0x64b42a8a45676uL,
                            0x4ea6e4f9a85cfuL,
                            0x26f57eee878a1uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x20cc9782a0ddeuL,
                            0x65d4e3070aab3uL,
                            0x7bc8e31547736uL,
                            0x9ebfb1432d98uL,
                            0x504aa77679736uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x32cd55687efb1uL,
                            0x4448f5e2f6195uL,
                            0x568919d460345uL,
                            0x34c2e0ad1a27uL,
                            0x4041943d9dba3uL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x17743a26caadduL,
                            0x48c9156f9c964uL,
                            0x7ef278d1e9ad0uL,
                            0xce58ea7bd01uL,
                            0x12d931429800duL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0xeeba43ebcc96uL,
                            0x384dd5395f878uL,
                            0x1df331a35d272uL,
                            0x207ecfd4af70euL,
                            0x1420a1d976843uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x67799d337594fuL,
                            0x1647548f6018uL,
                            0x57fce5578f145uL,
                            0x9220c142a71uL,
                            0x1b4f92314359auL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x73030a49866b1uL,
                            0x2442be90b2679uL,
                            0x77bd3d8947dcfuL,
                            0x1fb55c1552028uL,
                            0x5ff191d56f9a2uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x4109d89150951uL,
                            0x225bd2d2d47cbuL,
                            0x57cc080e73beauL,
                            0x6d71075721fcbuL,
                            0x239b572a7f132uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x6d433ac2d9068uL,
                            0x72bf930a47033uL,
                            0x64facf4a20eaduL,
                            0x365f7a2b9402auL,
                            0x20c526a758f3uL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x1ef59f042cc89uL,
                            0x3b1c24976dd26uL,
                            0x31d665cb16272uL,
                            0x28656e470c557uL,
                            0x452cfe0a5602cuL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x34f89ed8dbbcuL,
                            0x73b8f948d8ef3uL,
                            0x786c1d323caabuL,
                            0x43bd4a9266e51uL,
                            0x2aacc4615313uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0xf7a0647877dfuL,
                            0x4e1cc0f93f0d4uL,
                            0x7ec4726ef1190uL,
                            0x3bdd58bf512f8uL,
                            0x4cfb7d7b304b8uL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x699c29789ef12uL,
                            0x63beae321bc50uL,
                            0x325c340adbb35uL,
                            0x562e1a1e42bf6uL,
                            0x5b1d4cbc434d3uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x43d6cb89b75feuL,
                            0x3338d5b900e56uL,
                            0x38d327d531a53uL,
                            0x1b25c61d51b9fuL,
                            0x14b4622b39075uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x32615cc0a9f26uL,
                            0x57711b99cb6dfuL,
                            0x5a69c14e93c38uL,
                            0x6e88980a4c599uL,
                            0x2f98f71258592uL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x2ae444f54a701uL,
                            0x615397afbc5c2uL,
                            0x60d7783f3f8fbuL,
                            0x2aa675fc486bauL,
                            0x1d8062e9e7614uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x4a74cb50f9e56uL,
                            0x531d1c2640192uL,
                            0xc03d9d6c7fd2uL,
                            0x57ccd156610c1uL,
                            0x3a6ae249d806auL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x2da85a9907c5auL,
                            0x6b23721ec4cafuL,
                            0x4d2d3a4683aa2uL,
                            0x7f9c6870efdefuL,
                            0x298b8ce8aef25uL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x272ea0a2165deuL,
                            0x68179ef3ed06fuL,
                            0x4e2b9c0feac1euL,
                            0x3ee290b1b63bbuL,
                            0x6ba6271803a7duL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x27953eff70cb2uL,
                            0x54f22ae0ec552uL,
                            0x29f3da92e2724uL,
                            0x242ca0c22bd18uL,
                            0x34b8a8404d5ceuL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x6ecb583693335uL,
                            0x3ec76bfdfb84duL,
                            0x2c895cf56a04fuL,
                            0x6355149d54d52uL,
                            0x71d62bdd465e1uL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x5b5dab1f75ef5uL,
                            0x1e2d60cbeb9a5uL,
                            0x527c2175dfe57uL,
                            0x59e8a2b8ff51fuL,
                            0x1c333621262b2uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x3cc28d378df80uL,
                            0x72141f4968ca6uL,
                            0x407696bdb6d0duL,
                            0x5d271b22ffcfbuL,
                            0x74d5f317f3172uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x7e55467d9ca81uL,
                            0x6a5653186f50duL,
                            0x6b188ece62df1uL,
                            0x4c66d36844971uL,
                            0x4aebcc4547e9duL
                        )
                    )
                ),
            ),
            arrayOf(
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x8d9e7354b610uL,
                            0x26b750b6dc168uL,
                            0x162881e01acc9uL,
                            0x7966df31d01a5uL,
                            0x173bd9ddc9a1duL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x71b276d01c9uL,
                            0xb0d8918e025euL,
                            0x75beea79ee2ebuL,
                            0x3c92984094db8uL,
                            0x5d88fbf95a3dbuL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0xf1efe5872dfuL,
                            0x5da872318256auL,
                            0x59ceb81635960uL,
                            0x18cf37693c764uL,
                            0x6e1cd13b19eauL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x3af629e5b0353uL,
                            0x204f1a088e8e5uL,
                            0x10efc9ceea82euL,
                            0x589863c2fa34buL,
                            0x7f3a6a1a8d837uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0xad516f166f23uL,
                            0x263f56d57c81auL,
                            0x13422384638cauL,
                            0x1331ff1af0a50uL,
                            0x3080603526e16uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x644395d3d800buL,
                            0x2b9203dbedefcuL,
                            0x4b18ce656a355uL,
                            0x3f3466bc182cuL,
                            0x30d0fded2e513uL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x4971e68b84750uL,
                            0x52ccc9779f396uL,
                            0x3e904ae8255c8uL,
                            0x4ecae46f39339uL,
                            0x4615084351c58uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x14d1af21233b3uL,
                            0x1de1989b39c0buL,
                            0x52669dc6f6f9euL,
                            0x43434b28c3fc7uL,
                            0xa9214202c099uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x19c0aeb9a02euL,
                            0x1a2c06995d792uL,
                            0x664cbb1571c44uL,
                            0x6ff0736fa80b2uL,
                            0x3bca0d2895ca5uL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x8eb69ecc01bfuL,
                            0x5b4c8912df38duL,
                            0x5ea7f8bc2f20euL,
                            0x120e516caafafuL,
                            0x4ea8b4038df28uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x31bc3c5d62a4uL,
                            0x7d9fe0f4c081euL,
                            0x43ed51467f22cuL,
                            0x1e6cc0c1ed109uL,
                            0x5631deddae8f1uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x5460af1cad202uL,
                            0xb4919dd0655duL,
                            0x7c4697d18c14cuL,
                            0x231c890bba2a4uL,
                            0x24ce0930542cauL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x7a155fdf30b85uL,
                            0x1c6c6e5d487f9uL,
                            0x24be1134bdc5auL,
                            0x1405970326f32uL,
                            0x549928a7324f4uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x90f5fd06c106uL,
                            0x6abb1021e43fduL,
                            0x232bcfad711a0uL,
                            0x3a5c13c047f37uL,
                            0x41d4e3c28a06duL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x632a763ee1a2euL,
                            0x6fa4bffbd5e4duL,
                            0x5fd35a6ba4792uL,
                            0x7b55e1de99de8uL,
                            0x491b66dec0dcfuL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x4a8ed0da64a1uL,
                            0x5ecfc45096ebeuL,
                            0x5edee93b488b2uL,
                            0x5b3c11a51bc8fuL,
                            0x4cf6b8b0b7018uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x5b13dc7ea32a7uL,
                            0x18fc2db73131euL,
                            0x7e3651f8f57e3uL,
                            0x25656055fa965uL,
                            0x8f338d0c85eeuL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x3a821991a73bduL,
                            0x3be6418f5870uL,
                            0x1ddc18eac9ef0uL,
                            0x54ce09e998dc2uL,
                            0x530d4a82eb078uL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x173456c9abf9euL,
                            0x7892015100daduL,
                            0x33ee14095fecbuL,
                            0x6ad95d67a0964uL,
                            0xdb3e7e00cbfbuL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x43630e1f94825uL,
                            0x4d1956a6b4009uL,
                            0x213fe2df8b5e0uL,
                            0x5ce3a41191e6uL,
                            0x65ea753f10177uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x6fc3ee2096363uL,
                            0x7ec36b96d67acuL,
                            0x510ec6a0758b1uL,
                            0xed87df022109uL,
                            0x2a4ec1921e1auL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x6162f1cf795fuL,
                            0x324ddcafe5eb9uL,
                            0x18d5e0463218uL,
                            0x7e78b9092428euL,
                            0x36d12b5dec067uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x6259a3b24b8a2uL,
                            0x188b5f4170b9cuL,
                            0x681c0dee15debuL,
                            0x4dfe665f37445uL,
                            0x3d143c5112780uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x5279179154557uL,
                            0x39f8f0741424duL,
                            0x45e6eb357923duL,
                            0x42c9b5edb746fuL,
                            0x2ef517885ba82uL
                        )
                    )
                ),
            ),
            arrayOf(
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x6bffb305b2f51uL,
                            0x5b112b2d712dduL,
                            0x35774974fe4e2uL,
                            0x4af87a96e3a3uL,
                            0x57968290bb3a0uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x7974e8c58aedcuL,
                            0x7757e083488c6uL,
                            0x601c62ae7bc8buL,
                            0x45370c2ecab74uL,
                            0x2f1b78fab143auL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x2b8430a20e101uL,
                            0x1a49e1d88fee3uL,
                            0x38bbb47ce4d96uL,
                            0x1f0e7ba84d437uL,
                            0x7dc43e35dc2aauL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x2a5c273e9718uL,
                            0x32bc9dfb28b4fuL,
                            0x48df4f8d5db1auL,
                            0x54c87976c028fuL,
                            0x44fb81d82d50uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x66665887dd9c3uL,
                            0x629760a6ab0b2uL,
                            0x481e6c7243e6cuL,
                            0x97e37046fc77uL,
                            0x7ef72016758ccuL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x718c5a907e3d9uL,
                            0x3b9c98c6b383buL,
                            0x6ed255eccdcuL,
                            0x6976538229a59uL,
                            0x7f79823f9c30duL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x41ff068f587bauL,
                            0x1c00a191bcd53uL,
                            0x7b56f9c209e25uL,
                            0x3781e5fccaabeuL,
                            0x64a9b0431c06duL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x4d239a3b513e8uL,
                            0x29723f51b1066uL,
                            0x642f4cf04d9c3uL,
                            0x4da095aa09b7auL,
                            0xa4e0373d784duL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x3d6a15b7d2919uL,
                            0x41aa75046a5d6uL,
                            0x691751ec2d3dauL,
                            0x23638ab6721c4uL,
                            0x71a7d0ace183uL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x4355220e14431uL,
                            0xe1362a283981uL,
                            0x2757cd8359654uL,
                            0x2e9cd7ab10d90uL,
                            0x7c69bcf761775uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x72daac887ba0buL,
                            0xb7f4ac5dda60uL,
                            0x3bdda2c0498a4uL,
                            0x74e67aa180160uL,
                            0x2c3bcc7146ea7uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0xd7eb04e8295fuL,
                            0x4a5ea1e6fa0feuL,
                            0x45e635c436c60uL,
                            0x28ef4a8d4d18buL,
                            0x6f5a9a7322acauL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x1d4eba3d944beuL,
                            0x100f15f3dce5uL,
                            0x61a700e367825uL,
                            0x5922292ab3d23uL,
                            0x2ab9680ee8d3uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x1000c2f41c6c5uL,
                            0x219fdf737174uL,
                            0x314727f127de7uL,
                            0x7e5277d23b81euL,
                            0x494e21a2e147auL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x48a85dde50d9auL,
                            0x1c1f734493df4uL,
                            0x47bdb64866889uL,
                            0x59a7d048f8eecuL,
                            0x6b5d76cbea46buL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x141171e782522uL,
                            0x6806d26da7c1fuL,
                            0x3f31d1bc79ab9uL,
                            0x9f20459f5168uL,
                            0x16fb869c03dd3uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x7556cec0cd994uL,
                            0x5eb9a03b7510auL,
                            0x50ad1dd91cb71uL,
                            0x1aa5780b48a47uL,
                            0xae333f685277uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x6199733b60962uL,
                            0x69b157c266511uL,
                            0x64740f893f1cauL,
                            0x3aa408fbf684uL,
                            0x3f81e38b8f70duL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x37f355f17c824uL,
                            0x7ae85334815buL,
                            0x7e3abddd2e48fuL,
                            0x61eeabe1f45e5uL,
                            0xad3e2d34cdeduL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x10fcc7ed9affeuL,
                            0x4248cb0e96ff2uL,
                            0x4311c115172e2uL,
                            0x4c9d41cbf6925uL,
                            0x50510fc104f50uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x40fc5336e249duL,
                            0x3386639fb2de1uL,
                            0x7bbf871d17b78uL,
                            0x75f796b7e8004uL,
                            0x127c158bf0fa1uL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x28fc4ae51b974uL,
                            0x26e89bfd2dbd4uL,
                            0x4e122a07665cfuL,
                            0x7cab1203405c3uL,
                            0x4ed82479d167duL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x17c422e9879a2uL,
                            0x28a5946c8fec3uL,
                            0x53ab32e912b77uL,
                            0x7b44da09fe0a5uL,
                            0x354ef87d07ef4uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x3b52260c5d975uL,
                            0x79d6836171fdcuL,
                            0x7d994f140d4bbuL,
                            0x1b6c404561854uL,
                            0x302d92d205392uL
                        )
                    )
                ),
            ),
            arrayOf(
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x46fb6e4e0f177uL,
                            0x53497ad5265b7uL,
                            0x1ebdba01386fcuL,
                            0x302f0cb36a3cuL,
                            0xedc5f5eb426duL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x3c1a2bca4283duL,
                            0x23430c7bb2f02uL,
                            0x1a3ea1bb58bc2uL,
                            0x7265763de5c61uL,
                            0x10e5d3b76f1cauL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x3bfd653da8e67uL,
                            0x584953ec82a8auL,
                            0x55e288fa7707buL,
                            0x5395fc3931d81uL,
                            0x45b46c51361cbuL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x54ddd8a7fe3e4uL,
                            0x2cecc41c619d3uL,
                            0x43a6562ac4d91uL,
                            0x4efa5aca7bdd9uL,
                            0x5c1c0aef32122uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x2abf314f7fa1uL,
                            0x391d19e8a1528uL,
                            0x6a2fa13895fc7uL,
                            0x9d8eddeaa591uL,
                            0x2177bfa36dcb7uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x1bbcfa79db8fuL,
                            0x3d84beb3666e1uL,
                            0x20c921d812204uL,
                            0x2dd843d3b32ceuL,
                            0x4ae619387d8abuL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x17e44985bfb83uL,
                            0x54e32c626cc22uL,
                            0x96412ff38118uL,
                            0x6b241d61a246auL,
                            0x75685abe5ba43uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x3f6aa5344a32euL,
                            0x69683680f11bbuL,
                            0x4c3581f623aauL,
                            0x701af5875cba5uL,
                            0x1a00d91b17bf3uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x60933eb61f2b2uL,
                            0x5193fe92a4dd2uL,
                            0x3d995a550f43euL,
                            0x3556fb93a883duL,
                            0x135529b623b0euL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x716bce22e83feuL,
                            0x33d0130b83eb8uL,
                            0x952abad0afacuL,
                            0x309f64ed31b8auL,
                            0x5972ea051590auL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0xdbd7add1d518uL,
                            0x119f823e2231euL,
                            0x451d66e5e7de2uL,
                            0x500c39970f838uL,
                            0x79b5b81a65ca3uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x4ac20dc8f7811uL,
                            0x29589a9f501fauL,
                            0x4d810d26a6b4auL,
                            0x5ede00d96b259uL,
                            0x4f7e9c95905f3uL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x443d355299feuL,
                            0x39b7d7d5aee39uL,
                            0x692519a2f34ecuL,
                            0x6e4404924cf78uL,
                            0x1942eec4a144auL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x74bbc5781302euL,
                            0x73135bb81ec4cuL,
                            0x7ef671b61483cuL,
                            0x7264614ccd729uL,
                            0x31993ad92e638uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x45319ae234992uL,
                            0x2219d47d24fb5uL,
                            0x4f04488b06cf6uL,
                            0x53aaa9e724a12uL,
                            0x2a0a65314ef9cuL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x61acd3c1c793auL,
                            0x58b46b78779e6uL,
                            0x3369aacbe7af2uL,
                            0x509b0743074d4uL,
                            0x55dc39b6dea1uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x7937ff7f927c2uL,
                            0xc2fa14c6a5b6uL,
                            0x556bddb6dd07cuL,
                            0x6f6acc179d108uL,
                            0x4cf6e218647c2uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x1227cc28d5bb6uL,
                            0x78ee9bff57623uL,
                            0x28cb2241f893auL,
                            0x25b541e3c6772uL,
                            0x121a307710aa2uL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x1713ec77483c9uL,
                            0x6f70572d5facbuL,
                            0x25ef34e22ff81uL,
                            0x54d944f141188uL,
                            0x527bb94a6ced3uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x35d5e9f034a97uL,
                            0x126069785bc9buL,
                            0x5474ec7854ff0uL,
                            0x296a302a348cauL,
                            0x333fc76c7a40euL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x5992a995b482euL,
                            0x78dc707002ac7uL,
                            0x5936394d01741uL,
                            0x4fba4281aef17uL,
                            0x6b89069b20a7auL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x2fa8cb5c7db77uL,
                            0x718e6982aa810uL,
                            0x39e95f81a1a1buL,
                            0x5e794f3646cfbuL,
                            0x473d308a7639uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x2a0416270220duL,
                            0x75f248b69d025uL,
                            0x1cbbc16656a27uL,
                            0x5b9ffd6e26728uL,
                            0x23bc2103aa73euL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x6792603589e05uL,
                            0x248db9892595duL,
                            0x6a53cad2d08uL,
                            0x20d0150f7ba73uL,
                            0x102f73bfde043uL
                        )
                    )
                ),
            ),
            arrayOf(
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x4dae0b5511c9auL,
                            0x5257fffe0d456uL,
                            0x54108d1eb2180uL,
                            0x96cc0f9baefauL,
                            0x3f6bd725da4eauL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0xb9ab7f5745c6uL,
                            0x5caf0f8d21d63uL,
                            0x7debea408ea2buL,
                            0x9edb93896d16uL,
                            0x36597d25ea5c0uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x58d7b106058acuL,
                            0x3cdf8d20bee69uL,
                            0xa4cb765015euL,
                            0x36832337c7cc9uL,
                            0x7b7ecc19da60duL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x64a51a77cfa9buL,
                            0x29cf470ca0db5uL,
                            0x4b60b6e0898d9uL,
                            0x55d04ddffe6c7uL,
                            0x3bedc661bf5cuL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x2373c695c690duL,
                            0x4c0c8520dcf18uL,
                            0x384af4b7494b9uL,
                            0x4ab4a8ea22225uL,
                            0x4235ad7601743uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0xcb0d078975f5uL,
                            0x292313e530c4buL,
                            0x38dbb9124a509uL,
                            0x350d0655a11f1uL,
                            0xe7ce2b0cdf06uL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x6fedfd94b70f9uL,
                            0x2383f9745bfd4uL,
                            0x4beae27c4c301uL,
                            0x75aa4416a3f3fuL,
                            0x615256138aeceuL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x4643ac48c85a3uL,
                            0x6878c2735b892uL,
                            0x3a53523f4d877uL,
                            0x3a504ed8bee9duL,
                            0x666e0a5d8fb46uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x3f64e4870cb0duL,
                            0x61548b16d6557uL,
                            0x7a261773596f3uL,
                            0x7724d5f275d3auL,
                            0x7f0bc810d514duL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x49dad737213a0uL,
                            0x745dee5d31075uL,
                            0x7b1a55e7fdbe2uL,
                            0x5ba988f176ea1uL,
                            0x1d3a907ddec5auL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x6ba426f4136fuL,
                            0x3cafc0606b720uL,
                            0x518f0a2359cdauL,
                            0x5fae5e46feca7uL,
                            0xd1f8dbcf8eeduL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x693313ed081dcuL,
                            0x5b0a366901742uL,
                            0x40c872ca4ca7euL,
                            0x6f18094009e01uL,
                            0x11b44a31bfuL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x61f696a0aa75cuL,
                            0x38b0a57ad42cauL,
                            0x1e59ab706fdc9uL,
                            0x1308d46ebfcduL,
                            0x63d988a2d2851uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x7a06c3fc66c0cuL,
                            0x1c9bac1ba47fbuL,
                            0x23935c575038euL,
                            0x3f0bd71c59c13uL,
                            0x3ac48d916e835uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x20753afbd232euL,
                            0x71fbb1ed06002uL,
                            0x39cae47a4af3auL,
                            0x337c0b34d9c2uL,
                            0x33fad52b2368auL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x4c8d0c422cfe8uL,
                            0x760b4275971a5uL,
                            0x3da95bc1cad3duL,
                            0xf151ff5b7376uL,
                            0x3cc355ccb90a7uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x649c6c5e41e16uL,
                            0x60667eee6aa80uL,
                            0x4179d182be190uL,
                            0x653d9567e6979uL,
                            0x16c0f429a256duL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x69443903e9131uL,
                            0x16f4ac6f9dd36uL,
                            0x2ea4912e29253uL,
                            0x2b4643e68d25duL,
                            0x631eaf426bae7uL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x175b9a3700de8uL,
                            0x77c5f00aa48fbuL,
                            0x3917785ca0317uL,
                            0x5aa9b2c79399uL,
                            0x431f2c7f665f8uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x10410da66fe9fuL,
                            0x24d82dcb4d67duL,
                            0x3e6fe0e17752duL,
                            0x4dade1ecbb08fuL,
                            0x5599648b1ea91uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x26344858f7b19uL,
                            0x5f43d4a295ac0uL,
                            0x242a75c52acd4uL,
                            0x5934480220d10uL,
                            0x7b04715f91253uL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x6c280c4e6bac6uL,
                            0x3ada3b361766euL,
                            0x42fe5125c3b4fuL,
                            0x111d84d4aac22uL,
                            0x48d0acfa57cdeuL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x5bd28acf6ae43uL,
                            0x16fab8f56907duL,
                            0x7acb11218d5f2uL,
                            0x41fe02023b4dbuL,
                            0x59b37bf5c2f65uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x726e47dabe671uL,
                            0x2ec45e746f6c1uL,
                            0x6580e53c74686uL,
                            0x5eda104673f74uL,
                            0x16234191336d3uL
                        )
                    )
                ),
            ),
            arrayOf(
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x19cd61ff38640uL,
                            0x60c6c4b41ba9uL,
                            0x75cf70ca7366fuL,
                            0x118a8f16c011euL,
                            0x4a25707a203b9uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x499def6267ff6uL,
                            0x76e858108773cuL,
                            0x693cac5ddcb29uL,
                            0x311d00a9ff4uL,
                            0x2cdfdfecd5d05uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x7668a53f6ed6auL,
                            0x303ba2e142556uL,
                            0x3880584c10909uL,
                            0x4fe20000a261duL,
                            0x5721896d248e4uL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x55091a1d0da4euL,
                            0x4f6bfc7c1050buL,
                            0x64e4ecd2ea9beuL,
                            0x7eb1f28bbe70uL,
                            0x3c935afc4b03uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x65517fd181baeuL,
                            0x3e5772c76816duL,
                            0x19189640898auL,
                            0x1ed2a84de7499uL,
                            0x578edd74f63c1uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x276c6492b0c3duL,
                            0x9bfc40bf932euL,
                            0x588e8f11f330buL,
                            0x3d16e694dc26euL,
                            0x3ec2ab590288cuL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x13a09ae32d1cbuL,
                            0x3e81eb85ab4e4uL,
                            0x7aaca43cae1fuL,
                            0x62f05d7526374uL,
                            0xe1bf66c6adbauL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0xd27be4d87bb9uL,
                            0x56c27235db434uL,
                            0x72e6e0ea62d37uL,
                            0x5674cd06ee839uL,
                            0x2dd5c25a200fcuL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x3d5e9792c887euL,
                            0x319724dabbc55uL,
                            0x2b97c78680800uL,
                            0x7afdfdd34e6dduL,
                            0x730548b35ae88uL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x3094ba1d6e334uL,
                            0x6e126a7e3300buL,
                            0x89c0aefcfbc5uL,
                            0x2eea11f836583uL,
                            0x585a2277d8784uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x551a3cba8b8eeuL,
                            0x3b6422be2d886uL,
                            0x630e1419689bcuL,
                            0x4653b07a7a955uL,
                            0x3043443b411dbuL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x25f8233d48962uL,
                            0x6bd8f04aff431uL,
                            0x4f907fd9a6312uL,
                            0x40fd3c737d29buL,
                            0x7656278950ef9uL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x73a3ea86cf9duL,
                            0x6e0e2abfb9c2euL,
                            0x60e2a38ea33eeuL,
                            0x30b2429f3fe18uL,
                            0x28bbf484b613fuL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x3cf59d51fc8c0uL,
                            0x7a0a0d6de4718uL,
                            0x55c3a3e6fb74buL,
                            0x353135f884fd5uL,
                            0x3f4160a8c1b84uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x12f5c6f136c7cuL,
                            0xfedba237de4cuL,
                            0x779bccebfab44uL,
                            0x3aea93f4d6909uL,
                            0x1e79cb358188fuL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x153d8f5e08181uL,
                            0x8533bbdb2efduL,
                            0x1149796129431uL,
                            0x17a6e36168643uL,
                            0x478ab52d39d1fuL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x436c3eef7e3f1uL,
                            0x7ffd3c21f0026uL,
                            0x3e77bf20a2da9uL,
                            0x418bffc8472deuL,
                            0x65d7951b3a3b3uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x6a4d39252d159uL,
                            0x790e35900ecd4uL,
                            0x30725bf977786uL,
                            0x10a5c1635a053uL,
                            0x16d87a411a212uL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x4d5e2d54e0583uL,
                            0x2e5d7b33f5f74uL,
                            0x3a5de3f887ebfuL,
                            0x6ef24bd6139b7uL,
                            0x1f990b577a5a6uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x57e5a42066215uL,
                            0x1a18b44983677uL,
                            0x3e652de1e6f8fuL,
                            0x6532be02ed8ebuL,
                            0x28f87c8165f38uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x44ead1be8f7d6uL,
                            0x5759d4f31f466uL,
                            0x378149f47943uL,
                            0x69f3be32b4f29uL,
                            0x45882fe1534d6uL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x49929943c6fe4uL,
                            0x4347072545b15uL,
                            0x3226bced7e7c5uL,
                            0x3a134ced89dfuL,
                            0x7dcf843ce405fuL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x1345d757983d6uL,
                            0x222f54234cccduL,
                            0x1784a3d8adbb4uL,
                            0x36ebeee8c2bccuL,
                            0x688fe5b8f626fuL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0xd6484a4732c0uL,
                            0x7b94ac6532d92uL,
                            0x5771b8754850fuL,
                            0x48dd9df1461c8uL,
                            0x6739687e73271uL
                        )
                    )
                ),
            ),
            arrayOf(
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x5cc9dc80c1ac0uL,
                            0x683671486d4cduL,
                            0x76f5f1a5e8173uL,
                            0x6d5d3f5f9df4auL,
                            0x7da0b8f68d7e7uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x2014385675a6uL,
                            0x6155fb53d1defuL,
                            0x37ea32e89927cuL,
                            0x59a668f5a82euL,
                            0x46115aba1d4dcuL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x71953c3b5da76uL,
                            0x6642233d37a81uL,
                            0x2c9658076b1bduL,
                            0x5a581e63010ffuL,
                            0x5a5f887e83674uL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x628d3a0a643b9uL,
                            0x1cd8640c93d2uL,
                            0xb7b0cad70f2cuL,
                            0x3864da98144beuL,
                            0x43e37ae2d5d1cuL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x301cf70a13d11uL,
                            0x2a6a1ba1891ecuL,
                            0x2f291fb3f3ae0uL,
                            0x21a7b814bea52uL,
                            0x3669b656e44d1uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x63f06eda6e133uL,
                            0x233342758070fuL,
                            0x98e0459cc075uL,
                            0x4df5ead6c7c1buL,
                            0x6a21e6cd4fd5euL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x129126699b2e3uL,
                            0xee11a2603de8uL,
                            0x60ac2f5c74c21uL,
                            0x59b192a196808uL,
                            0x45371b07001e8uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x6170a3046e65fuL,
                            0x5401a46a49e38uL,
                            0x20add5561c4a8uL,
                            0x7abb4edde9e46uL,
                            0x586bf9f1a195fuL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x3088d5ef8790buL,
                            0x38c2126fcb4dbuL,
                            0x685bae149e3c3uL,
                            0xbcd601a4e930uL,
                            0xeafb03790e52uL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x805e0f75ae1duL,
                            0x464cc59860a28uL,
                            0x248e5b7b00befuL,
                            0x5d99675ef8f75uL,
                            0x44ae3344c5435uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x555c13748042fuL,
                            0x4d041754232c0uL,
                            0x521b430866907uL,
                            0x3308e40fb9c39uL,
                            0x309acc675a02cuL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x289b9bba543eeuL,
                            0x3ab592e28539euL,
                            0x64d82abcdd83auL,
                            0x3c78ec172e327uL,
                            0x62d5221b7f946uL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x5d4263af77a3cuL,
                            0x23fdd2289aeb0uL,
                            0x7dc64f77eb9ecuL,
                            0x1bd28338402cuL,
                            0x14f29a5383922uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x4299c18d0936duL,
                            0x5914183418a49uL,
                            0x52a18c721aed5uL,
                            0x2b151ba82976duL,
                            0x5c0efde4bc754uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x17edc25b2d7f5uL,
                            0x37336a6081beeuL,
                            0x7b5318887e5c3uL,
                            0x49f6d491a5be1uL,
                            0x5e72365c7bee0uL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x339062f08b33euL,
                            0x4bbf3e657cfb2uL,
                            0x67af7f56e5967uL,
                            0x4dbd67f9ed68fuL,
                            0x70b20555cb734uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x3fc074571217fuL,
                            0x3a0d29b2b6aebuL,
                            0x6478ccdde59duL,
                            0x55e4d051bddfauL,
                            0x77f1104c47b4euL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x113c555112c4cuL,
                            0x7535103f9b7cauL,
                            0x140ed1d9a2108uL,
                            0x2522333bc2afuL,
                            0xe34398f4a064uL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x30b093e4b1928uL,
                            0x1ce7e7ec80312uL,
                            0x4e575bdf78f84uL,
                            0x61f7a190bed39uL,
                            0x6f8aded6ca379uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x522d93ecebde8uL,
                            0x24f045e0f6cfuL,
                            0x16db63426cfa1uL,
                            0x1b93a1fd30fd8uL,
                            0x5e5405368a362uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x123dfdb7b29auL,
                            0x4344356523c68uL,
                            0x79a527921ee5fuL,
                            0x74bfccb3e817euL,
                            0x780de72ec8d3duL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x7eaf300f42772uL,
                            0x5455188354ce3uL,
                            0x4dcca4a3dcbacuL,
                            0x3d314d0bfebcbuL,
                            0x1defc6ad32b58uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x28545089ae7bcuL,
                            0x1e38fe9a0c15cuL,
                            0x12046e0e2377buL,
                            0x6721c560aa885uL,
                            0xeb28bf671928uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x3be1aef5195a7uL,
                            0x6f22f62bdb5ebuL,
                            0x39768b8523049uL,
                            0x43394c8fbfdbduL,
                            0x467d201bf8dd2uL
                        )
                    )
                ),
            ),
            arrayOf(
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x6f4bd567ae7a9uL,
                            0x65ac89317b783uL,
                            0x7d3b20fd8932uL,
                            0xf208326916uL,
                            0x2ef9c5a5ba384uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x6919a74ef4faduL,
                            0x59ed4611452bfuL,
                            0x691ec04ea09efuL,
                            0x3cbcb2700e984uL,
                            0x71c43c4f5ba3cuL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x56df6fa9e74cduL,
                            0x79c95e4cf56dfuL,
                            0x7be643bc609e2uL,
                            0x149c12ad9e878uL,
                            0x5a758ca390c5fuL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x918b1d61dc94uL,
                            0xd350260cd19cuL,
                            0x7a2ab4e37b4d9uL,
                            0x21fea735414d7uL,
                            0xa738027f639duL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x72710d9462495uL,
                            0x25aafaa007456uL,
                            0x2d21f28eaa31buL,
                            0x17671ea005fd0uL,
                            0x2dbae244b3eb7uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x74a2f57ffe1ccuL,
                            0x1bc3073087301uL,
                            0x7ec57f4019c34uL,
                            0x34e082e1fa524uL,
                            0x2698ca635126auL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x5702f5e3dd90euL,
                            0x31c9a4a70c5c7uL,
                            0x136a5aa78fc24uL,
                            0x1992f3b9f7b01uL,
                            0x3c004b0c4afa3uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x5318832b0ba78uL,
                            0x6f24b9ff17cecuL,
                            0xa47f30e060c7uL,
                            0x58384540dc8d0uL,
                            0x1fb43dcc49caeuL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x146ac06f4b82buL,
                            0x4b500d89e7355uL,
                            0x3351e1c728a12uL,
                            0x10b9f69932fe3uL,
                            0x6b43fd01cd1fduL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x742583e760ef3uL,
                            0x73dc1573216b8uL,
                            0x4ae48fdd7714auL,
                            0x4f85f8a13e103uL,
                            0x73420b2d6ff0duL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x75d4b4697c544uL,
                            0x11be1fff7f8f4uL,
                            0x119e16857f7e1uL,
                            0x38a14345cf5d5uL,
                            0x5a68d7105b52fuL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x4f6cb9e851e06uL,
                            0x278c4471895e5uL,
                            0x7efcdce3d64e4uL,
                            0x64f6d455c4b4cuL,
                            0x3db5632fea34buL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x190b1829825d5uL,
                            0xe7d3513225c9uL,
                            0x1c12be3b7abaeuL,
                            0x58777781e9ca6uL,
                            0x59197ea495df2uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x6ee2bf75dd9d8uL,
                            0x6c72ceb34be8duL,
                            0x679c9cc345ec7uL,
                            0x7898df96898a4uL,
                            0x4321adf49d75uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x16019e4e55aaeuL,
                            0x74fc5f25d209cuL,
                            0x4566a939ded0duL,
                            0x66063e716e0b7uL,
                            0x45eafdc1f4d70uL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x64624cfccb1eduL,
                            0x257ab8072b6c1uL,
                            0x120725676f0auL,
                            0x4a018d04e8eeeuL,
                            0x3f73ceea5d56duL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x401858045d72buL,
                            0x459e5e0ca2d30uL,
                            0x488b719308beauL,
                            0x56f4a0d1b32b5uL,
                            0x5a5eebc80362duL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x7bfd10a4e8dc6uL,
                            0x7c899366736f4uL,
                            0x55ebbeaf95c01uL,
                            0x46db060903f8auL,
                            0x2605889126621uL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x18e3cc676e542uL,
                            0x26079d995a990uL,
                            0x4a7c217908b2uL,
                            0x1dc7603e6655auL,
                            0xdedfa10b2444uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x704a68360ff04uL,
                            0x3cecc3cde8b3euL,
                            0x21cd5470f64ffuL,
                            0x6abc18d953989uL,
                            0x54ad0c2e4e615uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x367d5b82b522auL,
                            0xd3f4b83d7dc7uL,
                            0x3067f4cdbc58duL,
                            0x20452da697937uL,
                            0x62ecb2baa77a9uL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x72836afb62874uL,
                            0xaf3c2094b240uL,
                            0xc285297f357auL,
                            0x7cc2d5680d6e3uL,
                            0x61913d5075663uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x5795261152b3duL,
                            0x7a1dbbafa3cbduL,
                            0x5ad31c52588d5uL,
                            0x45f3a4164685cuL,
                            0x2e59f919a966duL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x62d361a3231dauL,
                            0x65284004e01b8uL,
                            0x656533be91d60uL,
                            0x6ae016c00a89fuL,
                            0x3ddbc2a131c05uL
                        )
                    )
                ),
            ),
            arrayOf(
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x257a22796bb14uL,
                            0x6f360fb443e75uL,
                            0x680e47220eaeauL,
                            0x2fcf2a5f10c18uL,
                            0x5ee7fb38d8320uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x40ff9ce5ec54buL,
                            0x57185e261b35buL,
                            0x3e254540e70a9uL,
                            0x1b5814003e3f8uL,
                            0x78968314ac04buL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x5fdcb41446a8euL,
                            0x5286926ff2a71uL,
                            0xf231e296b3f6uL,
                            0x684a357c84693uL,
                            0x61d0633c9bca0uL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x328bcf8fc73dfuL,
                            0x3b4de06ff95b4uL,
                            0x30aa427ba11a5uL,
                            0x5ee31bfda6d9cuL,
                            0x5b23ac2df8067uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x44935ffdb2566uL,
                            0x12f016d176c6euL,
                            0x4fbb00f16f5aeuL,
                            0x3fab78d99402auL,
                            0x6e965fd847aeduL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x2b953ee80527buL,
                            0x55f5bcdb1b35auL,
                            0x43a0b3fa23c66uL,
                            0x76e07388b820auL,
                            0x79b9bbb9dd95duL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x17dae8e9f7374uL,
                            0x719f76102da33uL,
                            0x5117c2a80ca8buL,
                            0x41a66b65d0936uL,
                            0x1ba811460accbuL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x355406a3126c2uL,
                            0x50d1918727d76uL,
                            0x6e5ea0b498e0euL,
                            0xa3b6063214f2uL,
                            0x5065f158c9fd2uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x169fb0c429954uL,
                            0x59aedd9ecee10uL,
                            0x39916eb851802uL,
                            0x57917555cc538uL,
                            0x3981f39e58a4fuL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x5dfa56de66fdeuL,
                            0x58809075908uL,
                            0x6d3d8cb854a94uL,
                            0x5b2f4e970b1e3uL,
                            0x30f4452edcbc1uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x38a7559230a93uL,
                            0x52c1cde8ba31fuL,
                            0x2a4f2d4745a3duL,
                            0x7e9d42d4a28auL,
                            0x38dc083705acduL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x52782c5759740uL,
                            0x53f3397d990aduL,
                            0x3a939c7e84d15uL,
                            0x234c4227e39e0uL,
                            0x632d9a1a593f2uL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x1fd11ed0c84a7uL,
                            0x21b3ed2757e1uL,
                            0x73e1de58fc1c6uL,
                            0x5d110c84616abuL,
                            0x3a5a7df28af64uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x36b15b807cba6uL,
                            0x3f78a9e1afed7uL,
                            0xa59c2c608f1fuL,
                            0x52bdd8ecb81b7uL,
                            0xb24f48847ed4uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x2d4be511beac7uL,
                            0x6bda4d99e5b9buL,
                            0x17e6996914e01uL,
                            0x7b1f0ce7fcf80uL,
                            0x34fcf74475481uL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x31dab78cfaa98uL,
                            0x4e3216e5e54b7uL,
                            0x249823973b689uL,
                            0x2584984e48885uL,
                            0x119a3042fb37uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x7e04c789767cauL,
                            0x1671b28cfb832uL,
                            0x7e57ea2e1c537uL,
                            0x1fbaaef444141uL,
                            0x3d3bdc164dfa6uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x2d89ce8c2177duL,
                            0x6cd12ba182cf4uL,
                            0x20a8ac19a7697uL,
                            0x539fab2cc72d9uL,
                            0x56c088f1ede20uL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x35fac24f38f02uL,
                            0x7d75c6197ab03uL,
                            0x33e4bc2a42fa7uL,
                            0x1c7cd10b48145uL,
                            0x38b7ea483590uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x53d1110a86e17uL,
                            0x6416eb65f466duL,
                            0x41ca6235fce20uL,
                            0x5c3fc8a99bb12uL,
                            0x9674c6b99108uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x6f82199316ff8uL,
                            0x5d54f1a9f3e9uL,
                            0x3bcc5d0bd274auL,
                            0x5b284b8d2d5aduL,
                            0x6e5e31025969euL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x4fb0e63066222uL,
                            0x130f59747e660uL,
                            0x41868fecd41auL,
                            0x3105e8c923bc6uL,
                            0x3058ad43d1838uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x462f587e593fbuL,
                            0x3d94ba7ce362duL,
                            0x330f9b52667b7uL,
                            0x5d45a48e0f00auL,
                            0x8f5114789a8duL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x40ffde57663d0uL,
                            0x71445d4c20647uL,
                            0x2653e68170f7cuL,
                            0x64cdee3c55ed6uL,
                            0x26549fa4efe3duL
                        )
                    )
                ),
            ),
            arrayOf(
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x68549af3f666euL,
                            0x9e2941d4bb68uL,
                            0x2e8311f5dff3cuL,
                            0x6429ef91ffbd2uL,
                            0x3a10dfe132ce3uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x55a461e6bf9d6uL,
                            0x78eeef4b02e83uL,
                            0x1d34f648c16cfuL,
                            0x7fea2aba5132uL,
                            0x1926e1dc6401euL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x74e8aea17cea0uL,
                            0xc743f83fbc0fuL,
                            0x7cb03c4bf5455uL,
                            0x68a8ba9917e98uL,
                            0x1fa1d01d861e5uL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x4ac00d1df94abuL,
                            0x3ba2101bd271buL,
                            0x7578988b9c4afuL,
                            0xf2bf89f49f7euL,
                            0x73fced18ee9a0uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x55947d599832uL,
                            0x346fe2aa41990uL,
                            0x164c8079195buL,
                            0x799ccfb7bba27uL,
                            0x773563bc6a75cuL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x1e90863139cb3uL,
                            0x4f8b407d9a0d6uL,
                            0x58e24ca924f69uL,
                            0x7a246bbe76456uL,
                            0x1f426b701b864uL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x635c891a12552uL,
                            0x26aebd38ede2fuL,
                            0x66dc8faddae05uL,
                            0x21c7d41a03786uL,
                            0xb76bb1b3fa7euL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x1264c41911c01uL,
                            0x702f44584bdf9uL,
                            0x43c511fc68edeuL,
                            0x482c3aed35f9uL,
                            0x4e1af5271d31buL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0xc1f97f92939buL,
                            0x17a88956dc117uL,
                            0x6ee005ef99dc7uL,
                            0x4aa9172b231ccuL,
                            0x7b6dd61eb772auL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0xabf9ab01d2c7uL,
                            0x3880287630ae6uL,
                            0x32eca045beddbuL,
                            0x57f43365f32d0uL,
                            0x53fa9b659bff6uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x5c1e850f33d92uL,
                            0x1ec119ab9f6f5uL,
                            0x7f16f6de663e9uL,
                            0x7a7d6cb16dec6uL,
                            0x703e9bceaf1d2uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x4c8e994885455uL,
                            0x4ccb5da9cad82uL,
                            0x3596bc610e975uL,
                            0x7a80c0ddb9f5euL,
                            0x398d93e5c4c61uL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x77c60d2e7e3f2uL,
                            0x4061051763870uL,
                            0x67bc4e0ecd2aauL,
                            0x2bb941f1373b9uL,
                            0x699c9c9002c30uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x3d16733e248f3uL,
                            0xe2b7e14be389uL,
                            0x42c0ddaf6784auL,
                            0x589ea1fc67850uL,
                            0x53b09b5ddf191uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x6a7235946f1ccuL,
                            0x6b99cbb2fbe60uL,
                            0x6d3a5d6485c62uL,
                            0x4839466e923c0uL,
                            0x51caf30c6fcdduL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x2f99a18ac54c7uL,
                            0x398a39661ee6fuL,
                            0x384331e40cde3uL,
                            0x4cd15c4de19a6uL,
                            0x12ae29c189f8euL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x3a7427674e00auL,
                            0x6142f4f7e74c1uL,
                            0x4cc93318c3a15uL,
                            0x6d51bac2b1ee7uL,
                            0x5504aa292383fuL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x6c0cb1f0d01cfuL,
                            0x187469ef5d533uL,
                            0x27138883747bfuL,
                            0x2f52ae53a90e8uL,
                            0x5fd14fe958ebauL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x2fe5ebf93cb8euL,
                            0x226da8acbe788uL,
                            0x10883a2fb7ea1uL,
                            0x94707842cf44uL,
                            0x7dd73f960725duL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x42ddf2845ab2cuL,
                            0x6214ffd3276bbuL,
                            0xb8d181a5246uL,
                            0x268a6d579eb20uL,
                            0x93ff26e58647uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x524fe68059829uL,
                            0x65b75e47cb621uL,
                            0x15eb0a5d5cc19uL,
                            0x5209b3929d5auL,
                            0x2f59bcbc86b47uL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x1d560b691c301uL,
                            0x7f5bafce3ce08uL,
                            0x4cd561614806cuL,
                            0x4588b6170b188uL,
                            0x2aa55e3d01082uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x47d429917135fuL,
                            0x3eacfa07af070uL,
                            0x1deab46b46e44uL,
                            0x7a53f3ba46cdfuL,
                            0x5458b42e2e51auL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x192e60c07444fuL,
                            0x5ae8843a21daauL,
                            0x6d721910b1538uL,
                            0x3321a95a6417euL,
                            0x13e9004a8a768uL
                        )
                    )
                ),
            ),
            arrayOf(
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x600c9193b877fuL,
                            0x21c1b8a0d7765uL,
                            0x379927fb38ea2uL,
                            0x70d7679dbe01buL,
                            0x5f46040898de9uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x58845832fcedbuL,
                            0x135cd7f0c6e73uL,
                            0x53ffbdfe8e35buL,
                            0x22f195e06e55buL,
                            0x73937e8814bceuL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x37116297bf48duL,
                            0x45a9e0d069720uL,
                            0x25af71aa744ecuL,
                            0x41af0cb8aaba3uL,
                            0x2cf8a4e891d5euL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x5487e17d06ba2uL,
                            0x3872a032d6596uL,
                            0x65e28c09348e0uL,
                            0x27b6bb2ce40c2uL,
                            0x7a6f7f2891d6auL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x3fd8707110f67uL,
                            0x26f8716a92db2uL,
                            0x1cdaa1b753027uL,
                            0x504be58b52661uL,
                            0x2049bd6e58252uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x1fd8d6a9aef49uL,
                            0x7cb67b7216fa1uL,
                            0x67aff53c3b982uL,
                            0x20ea610da9628uL,
                            0x6011aadfc5459uL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x6d0c802cbf890uL,
                            0x141bfed554c7buL,
                            0x6dbb667ef4263uL,
                            0x58f3126857edcuL,
                            0x69ce18b779340uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x7926dcf95f83cuL,
                            0x42e25120e2becuL,
                            0x63de96df1fa15uL,
                            0x4f06b50f3f9ccuL,
                            0x6fc5cc1b0b62fuL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x75528b29879cbuL,
                            0x79a8fd2125a3duL,
                            0x27c8d4b746ab8uL,
                            0xf8893f02210cuL,
                            0x15596b3ae5710uL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x731167e5124cauL,
                            0x17b38e8bbe13fuL,
                            0x3d55b942f9056uL,
                            0x9c1495be913fuL,
                            0x3aa4e241afb6duL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x739d23f9179a2uL,
                            0x632fadbb9e8c4uL,
                            0x7c8522bfe0c48uL,
                            0x6ed0983ef5aa9uL,
                            0xd2237687b5f4uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x138bf2a3305f5uL,
                            0x1f45d24d86598uL,
                            0x5274bad2160feuL,
                            0x1b6041d58d12auL,
                            0x32fcaa6e4687auL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x7a4732787ccdfuL,
                            0x11e427c7f0640uL,
                            0x3659385f8c64uL,
                            0x5f4ead9766bfbuL,
                            0x746f6336c2600uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x56e8dc57d9af5uL,
                            0x5b3be17be4f78uL,
                            0x3bf928cf82f4buL,
                            0x52e55600a6f11uL,
                            0x4627e9cefebd6uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x2f345ab6c971cuL,
                            0x653286e63e7e9uL,
                            0x51061b78a23aduL,
                            0x14999acb54501uL,
                            0x7b4917007ed66uL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x41b28dd53a2dduL,
                            0x37be85f87ea86uL,
                            0x74be3d2a85e41uL,
                            0x1be87fac96ca6uL,
                            0x1d03620fe08cduL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x5fb5cab84b064uL,
                            0x2513e778285b0uL,
                            0x457383125e043uL,
                            0x6bda3b56e223duL,
                            0x122ba376f844fuL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x232cda2b4e554uL,
                            0x422ba30ff840uL,
                            0x751e7667b43f5uL,
                            0x6261755da5f3euL,
                            0x2c70bf52b68euL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x532bf458d72e1uL,
                            0x40f96e796b59cuL,
                            0x22ef79d6f9da3uL,
                            0x501ab67beca77uL,
                            0x6b0697e3feb43uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x7ec4b5d0b2fbbuL,
                            0x200e910595450uL,
                            0x742057105715euL,
                            0x2f07022530f60uL,
                            0x26334f0a409efuL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0xf04adf62a3c0uL,
                            0x5e0edb48bb6d9uL,
                            0x7c34aa4fbc003uL,
                            0x7d74e4e5cac24uL,
                            0x1cc37f43441b2uL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x656f1c9ceaeb9uL,
                            0x7031cacad5aecuL,
                            0x1308cd0716c57uL,
                            0x41c1373941942uL,
                            0x3a346f772f196uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x7565a5cc7324fuL,
                            0x1ca0d5244a11uL,
                            0x116b067418713uL,
                            0xa57d8c55edaeuL,
                            0x6c6809c103803uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x55112e2da6ac8uL,
                            0x6363d0a3dba5auL,
                            0x319c98ba6f40cuL,
                            0x2e84b03a36ec7uL,
                            0x5911b9f6ef7cuL
                        )
                    )
                ),
            ),
            arrayOf(
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x1acf3512eeaefuL,
                            0x2639839692a69uL,
                            0x669a234830507uL,
                            0x68b920c0603d4uL,
                            0x555ef9d1c64b2uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x39983f5df0ebbuL,
                            0x1ea2589959826uL,
                            0x6ce638703cdd6uL,
                            0x6311678898505uL,
                            0x6b3cecf9aa270uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x770ba3b73bd08uL,
                            0x11475f7e186d4uL,
                            0x251bc9892bbcuL,
                            0x24eab9bffcc5auL,
                            0x675f4de133817uL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x7f6d93bdab31duL,
                            0x1f3aca5bfd425uL,
                            0x2fa521c1c9760uL,
                            0x62180ce27f9cduL,
                            0x60f450b882cd3uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x452036b1782fcuL,
                            0x2d95b07681c5uL,
                            0x5901cf99205b2uL,
                            0x290686e5eecb4uL,
                            0x13d99df70164cuL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x35ec321e5c0cauL,
                            0x13ae337f44029uL,
                            0x4008e813f2da7uL,
                            0x640272f8e0c3auL,
                            0x1c06de9e55edauL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x52b40ff6d69aauL,
                            0x31b8809377ffauL,
                            0x536625cd14c2cuL,
                            0x516af252e17d1uL,
                            0x78096f8e7d32buL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x77ad6a33ec4e2uL,
                            0x717c5dc11d321uL,
                            0x4a114559823e4uL,
                            0x306ce50a1e2b1uL,
                            0x4cf38a1fec2dbuL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x2aa650dfa5ce7uL,
                            0x54916a8f19415uL,
                            0xdc96fe71278uL,
                            0x55f2784e63eb8uL,
                            0x373cad3a26091uL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x6a8fb89ddbbaduL,
                            0x78c35d5d97e37uL,
                            0x66e3674ef2cb2uL,
                            0x34347ac53dd8fuL,
                            0x21547eda5112auL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x4634d82c9f57cuL,
                            0x4249268a6d652uL,
                            0x6336d687f2ff7uL,
                            0x4fe4f4e26d9a0uL,
                            0x40f3d945441uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x5e939fd5986d3uL,
                            0x12a2147019bdfuL,
                            0x4c466e7d09cb2uL,
                            0x6fa5b95d203dduL,
                            0x63550a334a254uL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x2584572547b49uL,
                            0x75c58811c1377uL,
                            0x4d3c637cc171buL,
                            0x33d30747d34e3uL,
                            0x39a92bafaa7d7uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x7d6edb569cf37uL,
                            0x60194a5dc2ca0uL,
                            0x5af59745e10a6uL,
                            0x7a8f53e004875uL,
                            0x3eea62c7daf78uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x4c713e693274euL,
                            0x6ed1b7a6eb3a4uL,
                            0x62ace697d8e15uL,
                            0x266b8292ab075uL,
                            0x68436a0665c9cuL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x6d317e820107cuL,
                            0x90815d2ca3cauL,
                            0x3ff1eb1499a1uL,
                            0x23960f050e319uL,
                            0x5373669c91611uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x235e8202f3f27uL,
                            0x44c9f2eb61780uL,
                            0x630905b1d7003uL,
                            0x4fcc8d274ead1uL,
                            0x17b6e7f68ab78uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x14ab9a0e5257uL,
                            0x9939567f8ba5uL,
                            0x4b47b2a423c82uL,
                            0x688d7e57ac42duL,
                            0x1cb4b5a678f87uL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x4aa62a2a007e7uL,
                            0x61e0e38f62d6euL,
                            0x2f888fcc4782uL,
                            0x7562b83f21c00uL,
                            0x2dc0fd2d82ef6uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x4c06b394afc6cuL,
                            0x4931b4bf636ccuL,
                            0x72b60d0322378uL,
                            0x25127c6818b25uL,
                            0x330bca78de743uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x6ff841119744euL,
                            0x2c560e8e49305uL,
                            0x7254fefe5a57auL,
                            0x67ae2c560a7dfuL,
                            0x3c31be1b369f1uL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0xbc93f9cb4272uL,
                            0x3f8f9db73182duL,
                            0x2b235eabae1c4uL,
                            0x2ddbf8729551auL,
                            0x41cec1097e7d5uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x4864d08948aeeuL,
                            0x5d237438df61euL,
                            0x2b285601f7067uL,
                            0x25dbcbae6d753uL,
                            0x330b61134262duL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x619d7a26d808auL,
                            0x3c3b3c2adbef2uL,
                            0x6877c9eec7f52uL,
                            0x3beb9ebe1b66duL,
                            0x26b44cd91f287uL
                        )
                    )
                ),
            ),
            arrayOf(
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x7f29362730383uL,
                            0x7fd7951459c36uL,
                            0x7504c512d49e7uL,
                            0x87ed7e3bc55fuL,
                            0x7deb10149c726uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x48478f387475uL,
                            0x69397d9678a3euL,
                            0x67c8156c976f3uL,
                            0x2eb4d5589226cuL,
                            0x2c709e6c1c10auL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x2af6a8766ee7auL,
                            0x8aaa79a1d96cuL,
                            0x42f92d59b2fb0uL,
                            0x1752c40009c07uL,
                            0x8e68e9ff62ceuL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x509d50ab8f2f9uL,
                            0x1b8ab247be5e5uL,
                            0x5d9b2e6b2e486uL,
                            0x4faa5479a1339uL,
                            0x4cb13bd738f71uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x5500a4bc130aduL,
                            0x127a17a938695uL,
                            0x2a26fa34e36duL,
                            0x584d12e1ecc28uL,
                            0x2f1f3f87eeba3uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x48c75e515b64auL,
                            0x75b6952071ef0uL,
                            0x5d46d42965406uL,
                            0x7746106989f9fuL,
                            0x19a1e353c0ae2uL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x172cdd596bdbduL,
                            0x731ddf881684uL,
                            0x10426d64f8115uL,
                            0x71a4fd8a9a3dauL,
                            0x736bd3990266auL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x47560bafa05c3uL,
                            0x418dcabcc2fa3uL,
                            0x35991cecf8682uL,
                            0x24371a94b8c60uL,
                            0x41546b11c20c3uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x32d509334b3b4uL,
                            0x16c102cae70aauL,
                            0x1720dd51bf445uL,
                            0x5ae662faf9821uL,
                            0x412295a2b87fauL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x55261e293eac6uL,
                            0x6426759b65ccuL,
                            0x40265ae116a48uL,
                            0x6c02304bae5bcuL,
                            0x760bb8d195aduL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x19b88f57ed6e9uL,
                            0x4cdbf1904a339uL,
                            0x42b49cd4e4f2cuL,
                            0x71a2e771909d9uL,
                            0x14e153ebb52d2uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x61a17cde6818auL,
                            0x53dad34108827uL,
                            0x32b32c55c55b6uL,
                            0x2f9165f9347a3uL,
                            0x6b34be9bc33acuL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x469656571f2d3uL,
                            0xaa61ce6f423fuL,
                            0x3f940d71b27a1uL,
                            0x185f19d73d16auL,
                            0x1b9c7b62e6dduL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x72f643a78c0b2uL,
                            0x3de45c04f9e7buL,
                            0x706d68d30fa5cuL,
                            0x696f63e8e2f24uL,
                            0x2012c18f0922duL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x355e55ac89d29uL,
                            0x3e8b414ec7101uL,
                            0x39db07c520c90uL,
                            0x6f41e9b77efe1uL,
                            0x8af5b784e4bauL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x314d289cc2c4buL,
                            0x23450e2f1bc4euL,
                            0xcd93392f92f4uL,
                            0x1370c6a946b7duL,
                            0x6423c1d5afd98uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x499dc881f2533uL,
                            0x34ef26476c506uL,
                            0x4d107d2741497uL,
                            0x346c4bd6efdb3uL,
                            0x32b79d71163a1uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x5f8d9edfcb36auL,
                            0x1e6e8dcbf3990uL,
                            0x7974f348af30auL,
                            0x6e6724ef19c7cuL,
                            0x480a5efbc13e2uL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x14ce442ce221fuL,
                            0x18980a72516ccuL,
                            0x72f80db86677uL,
                            0x703331fda526euL,
                            0x24b31d47691c8uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x1e70b01622071uL,
                            0x1f163b5f8a16auL,
                            0x56aaf341ad417uL,
                            0x7989635d830f7uL,
                            0x47aa27600cb7buL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x41eedc015f8c3uL,
                            0x7cf8d27ef854auL,
                            0x289e3584693f9uL,
                            0x4a7857b309a7uL,
                            0x545b585d14ddauL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x4e4d0e3b321e1uL,
                            0x7451fe3d2ac40uL,
                            0x666f678eea98duL,
                            0x38858667feaduL,
                            0x4d22dc3e64c8duL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x7275ea0d43a0fuL,
                            0x681137dd7ccf7uL,
                            0x1e79cbab79a38uL,
                            0x22a214489a66auL,
                            0xf62f9c332ba5uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x46589d63b5f39uL,
                            0x7eaf979ec3f96uL,
                            0x4ebe81572b9a8uL,
                            0x21b7f5d61694auL,
                            0x1c0fa01a36371uL
                        )
                    )
                ),
            ),
            arrayOf(
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x2b0e8c936a50uL,
                            0x6b83b58b6cd21uL,
                            0x37ed8d3e72680uL,
                            0xa037db9f2a62uL,
                            0x4005419b1d2bcuL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x604b622943dffuL,
                            0x1c899f6741a58uL,
                            0x60219e2f232fbuL,
                            0x35fae92a7f9cbuL,
                            0xfa3614f3b1cauL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x3febdb9be82f0uL,
                            0x5e74895921400uL,
                            0x553ea38822706uL,
                            0x5a17c24cfc88cuL,
                            0x1fba218aef40auL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x657043e7b0194uL,
                            0x5c11b55efe9e7uL,
                            0x7737bc6a074fbuL,
                            0xeae41ce355ccuL,
                            0x6c535d13ff776uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x49448fac8f53euL,
                            0x34f74c6e8356auL,
                            0xad780607dba2uL,
                            0x7213a7eb63eb6uL,
                            0x392e3acaa8c86uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x534e93e8a35afuL,
                            0x8b10fd02c997uL,
                            0x26ac2acb81e05uL,
                            0x9d8c98ce3b79uL,
                            0x25e17fe4d50acuL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x77ff576f121a7uL,
                            0x4e5f9b0fc722buL,
                            0x46f949b0d28c8uL,
                            0x4cde65d17ef26uL,
                            0x6bba828f89698uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x9bd71e04f676uL,
                            0x25ac841f2a145uL,
                            0x1a47eac823871uL,
                            0x1a8a8c36c581auL,
                            0x255751442a9fbuL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x1bc6690fe3901uL,
                            0x314132f5abc5auL,
                            0x611835132d528uL,
                            0x5f24b8eb48a57uL,
                            0x559d504f7f6b7uL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x91e7f6d266fduL,
                            0x36060ef037389uL,
                            0x18788ec1d1286uL,
                            0x287441c478eb0uL,
                            0x123ea6a3354bduL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x38378b3eb54d5uL,
                            0x4d4aaa78f94eeuL,
                            0x4a002e875a74duL,
                            0x10b851367b17cuL,
                            0x1ab12d5807e3uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x5189041e32d96uL,
                            0x5b062b090231uL,
                            0xc91766e7b78fuL,
                            0xaa0f55a138ecuL,
                            0x4a3961e2c918auL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x7d644f3233f1euL,
                            0x1c69f9e02c064uL,
                            0x36ae5e5266898uL,
                            0x8fc1dad38b79uL,
                            0x68aceead9bd41uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x43be0f8e6bba0uL,
                            0x68fdffc614e3buL,
                            0x4e91dab5b3be0uL,
                            0x3b1d4c9212ff0uL,
                            0x2cd6bce3fb1dbuL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x4c90ef3d7c210uL,
                            0x496f5a0818716uL,
                            0x79cf88cc239b8uL,
                            0x2cb9c306cf8dbuL,
                            0x595760d5b508fuL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x2cbebfd022790uL,
                            0xb8822aec1105uL,
                            0x4d1cfd226bcccuL,
                            0x515b2fa4971beuL,
                            0x2cb2c5df54515uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x1bfe104aa6397uL,
                            0x11494ff996c25uL,
                            0x64251623e5800uL,
                            0xd49fc5e044beuL,
                            0x709fa43edcb29uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x25d8c63fd2acauL,
                            0x4c5cd29dffd61uL,
                            0x32ec0eb48af05uL,
                            0x18f9391f9b77cuL,
                            0x70f029ecf0c81uL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x2afaa5e10b0b9uL,
                            0x61de08355254duL,
                            0xeb587de3c28duL,
                            0x4f0bb9f7dbbd5uL,
                            0x44eca5a2a74bduL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x307b32eed3e33uL,
                            0x6748ab03ce8c2uL,
                            0x57c0d9ab810bcuL,
                            0x42c64a224e98cuL,
                            0xb7d5d8a6c314uL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x448327b95d543uL,
                            0x146681e3a4bauL,
                            0x38714adc34e0cuL,
                            0x4f26f0e298e30uL,
                            0x272224512c7deuL
                        )
                    )
                ),
                GePrecomp(
                    yPlusX = Fe(
                        ulongArrayOf(
                            0x3bb8a42a975fcuL,
                            0x6f2d5b46b17efuL,
                            0x7b6a9223170e5uL,
                            0x53713fe3b7e6uL,
                            0x19735fd7f6bc2uL
                        )
                    ),
                    yMinusX = Fe(
                        ulongArrayOf(
                            0x492af49c5342euL,
                            0x2365cdf5a0357uL,
                            0x32138a7ffbb60uL,
                            0x2a1f7d14646feuL,
                            0x11b5df18a44ccuL
                        )
                    ),
                    xy2d = Fe(
                        ulongArrayOf(
                            0x390d042c84266uL,
                            0x1efe32a8fdc75uL,
                            0x6925ee7ae1238uL,
                            0x4af9281d0e832uL,
                            0xfef911191df8uL
                        )
                    )
                ),
            ),
        )

        val ZERO = GePrecomp(
            yPlusX = Fe.ONE,
            yMinusX = Fe.ONE,
            xy2d = Fe.ZERO
        )

        fun select(pos: Int, b: Byte): GePrecomp {
            require(b >= -8 && b <= 8) { "b must be in range -8..8" }
            val bnegative: UByte = (b.toUByte().toUInt() shr 7).toUByte()
            val babs: UByte = (b - (((-(bnegative.toByte())) and b.toInt()) shl 1)).toUByte()
            val t = when (babs) {
                1u.toUByte() -> GE_BASE[pos][0]
                2u.toUByte() -> GE_BASE[pos][1]
                3u.toUByte() -> GE_BASE[pos][2]
                4u.toUByte() -> GE_BASE[pos][3]
                5u.toUByte() -> GE_BASE[pos][4]
                6u.toUByte() -> GE_BASE[pos][5]
                7u.toUByte() -> GE_BASE[pos][6]
                8u.toUByte() -> GE_BASE[pos][7]
                else -> ZERO
            }

            return if (bnegative != 0.toUByte()) {
                // minusT
                GePrecomp(t.yMinusX, t.yPlusX, -t.xy2d)
            } else {
                t
            }
        }
    }
}
