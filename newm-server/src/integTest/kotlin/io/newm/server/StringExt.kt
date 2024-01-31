package io.newm.server

val charPool: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')

fun String.Companion.randomString(length: Int = 10) = List(length) { charPool.random() }.joinToString("")
