package com.helloclue.sse

import kotlinx.coroutines.CoroutineScope

/**
 * Allows running coroutines in a blocking fashion to wait
 * for completion; useful for tests.
 */
expect fun runBlocking(block: suspend CoroutineScope.() -> Unit)