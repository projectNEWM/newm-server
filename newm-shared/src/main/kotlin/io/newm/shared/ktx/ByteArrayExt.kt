package io.newm.shared.ktx

import java.util.*

fun ByteArray.toHexString(): String = HexFormat.of().formatHex(this)
