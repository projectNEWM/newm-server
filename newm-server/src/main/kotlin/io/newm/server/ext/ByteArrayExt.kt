package io.newm.server.ext

import java.util.*

fun ByteArray.toHexString(): String = HexFormat.of().formatHex(this)
