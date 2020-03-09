package com.helloclue.sse

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking

/**
 * Binds the [kotlinx.coroutines.runBlocking] for JVM.
 */
actual fun runBlocking(block: suspend CoroutineScope.() -> Unit) = runBlocking { block() }