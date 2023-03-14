package io.newm.shared.ext

import java.util.*

fun ByteArray.toHexString(): String = HexFormat.of().formatHex(this)
