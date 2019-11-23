package com.helloclue.sse

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce

internal typealias ByteCount = Long

typealias LineEnding = String
typealias LinePartial = String

@PublishedApi
internal const val EOF = true

/**
 * Returns a stream of lines (separated through [parseSseLineEnd]) parsed from a buffered character stream,
 * suspending when the buffer is exhausted but the underlying source is not.
 *
 * @see [parseSseLineEnd]
 */
@PublishedApi
internal inline fun CoroutineScope.readSseLinesFromBufferedStream(
    crossinline indexOfInBuffer: (Byte) -> Index,
    crossinline readAllInBuffer: () -> String,
    crossinline bufferSize: () -> ByteCount,
    crossinline readInStream: (ByteCount) -> String,
    crossinline skipInStream: (ByteCount) -> Unit,
    noinline isStreamExhausted: suspend () -> Boolean,
    logger: Logger? = null
): ReceiveChannel<Line> =
    produce {
        var previousExhausted = false
        var previousLineEndCr = false

        val partialLineBuffer = object {
            private val lineParts = mutableListOf<String>()

            /* Save part of the buffer [allows us to remember previous buffer parts when trying to read further] */
            operator fun plus(linePart: String) = Unit.also { lineParts.add(linePart) }

            /* Get all the previous parts of the buffer */
            fun getAllAndClear() = lineParts.joinToString(separator = "").also { lineParts.clear() }

            /* Get all the previous parts of the buffer and read from next part */
            fun getAllAndClear(mainBufferBytes: ByteCount) = getAllAndClear() + readInStream(mainBufferBytes)
        }

        while (true) {
            val bufferEof = parseSseLineEnd(
                indexOfCr = { indexOfInBuffer('\r'.toByte()) },
                indexOfLf = { indexOfInBuffer('\n'.toByte()) },
                onEof = {
                    EOF
                },
                onCrLf = { index ->
                    previousLineEndCr = false
                    partialLineBuffer.getAllAndClear(index).also {
                        logger?.onLine(it)
                        logger?.onLineEnding("\\r\\n")
                        skipInStream(2)
                        send(it)
                    }
                    !EOF
                },
                onLf = { index ->
                    val ignore = index == 0L && previousLineEndCr
                    previousLineEndCr = false
                    if (ignore) {
                        logger?.onLineEnding("[\\n]")
                        skipInStream(1)
                    } else {
                        partialLineBuffer.getAllAndClear(index).also {
                            logger?.onLine(it)
                            logger?.onLineEnding("\\n")
                            skipInStream(1)
                            send(it)
                        }
                    }
                    !EOF
                },
                onCr = { index ->
                    previousLineEndCr = index == bufferSize() - 1
                    partialLineBuffer.getAllAndClear(index).also {
                        logger?.onLine(it)
                        logger?.onLineEnding(if (previousLineEndCr) "\\r…" else "\\r")
                        skipInStream(1)
                        send(it)
                    }
                    !EOF
                }
            )

            /* Store the part of the buffer when it reaches EOF */
            if (bufferEof) {
                readAllInBuffer()
                    .takeIf(String::isNotEmpty)
                    ?.let(partialLineBuffer::plus)
            }

            if (bufferEof && isStreamExhausted()) {
                if (!previousExhausted) {
                    previousExhausted = true
                } else {
                    partialLineBuffer.getAllAndClear()
                        .takeIf(String::isNotEmpty)
                        ?.also { logger?.onLinePartial(it) }

                    logger?.onEndOfFile()
                    return@produce
                }
            }
        }
    }
