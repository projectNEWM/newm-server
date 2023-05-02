package io.newm.shared.ktx

import java.util.HexFormat

fun ByteArray.toHexString(): String = HexFormat.of().formatHex(this)
