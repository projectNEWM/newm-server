package io.newm.shared.ktx

import kotlinx.coroutines.runBlocking

inline fun <T> coLazy(crossinline initializer: suspend () -> T): Lazy<T> = lazy { runBlocking { initializer() } }
