package com.helloclue.sse

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.delay
import kotlin.jvm.JvmName

internal typealias PerformRequest<Response> = suspend () -> Response
internal typealias IndexOfInBuffer<Response> = Response.(Byte) -> Long
internal typealias ReadAllInBuffer<Response> = Response.() -> String
internal typealias BufferSize<Response> = Response.() -> Long
internal typealias ReadInStream<Response> = Response.(Long) -> String
internal typealias SkipInStream<Response> = Response.(Long) -> Unit
internal typealias IsStreamExhausted<Response> = suspend Response.() -> Boolean

/**
 * Connects to a [stream of events][EventSource] according to the [Server-Sent Events][Sse] protocol, retrying when appropriate.
 *
 * @see [Sse]
 */
inline fun <Response, reified ConnectionError> CoroutineScope.connectToSse(
    initialRetryTime: Milliseconds?,
    initialEventId: EventId?,
    noinline awaitDelay: AwaitDelay = ::delay,
    noinline maybeAwaitConnectivity: MaybeAwaitConnectivity,
    crossinline addHeaderToRequest: AddHeader,
    crossinline performRequest: PerformRequest<Response>,
    crossinline getResponseHttpCode: GetHttpCode<Response>,
    crossinline getResponseMediaType: GetMediaType<Response>,
    crossinline getResponseMediaSubType: GetMediaSubType<Response>,
    crossinline indexOfInBuffer: IndexOfInBuffer<Response>,
    crossinline readAllInBuffer: ReadAllInBuffer<Response>,
    crossinline bufferSize: BufferSize<Response>,
    crossinline readInStream: ReadInStream<Response>,
    crossinline skipInStream: SkipInStream<Response>,
    crossinline isStreamExhausted: IsStreamExhausted<Response>,
    crossinline disposeResponse: Dispose<Response>,
    crossinline random: () -> Long,
    logger: Logger? = null
): EventSource<Response, ConnectionError> =
    produce {
        logger?.onStart()

        val awaitDelayLogging = awaitDelay + logger
        val maybeAwaitConnectivityLogging = maybeAwaitConnectivity + logger

        lateinit var state: EventSourceState<Response, ConnectionError>
        val setState: suspend (EventSourceState<Response, ConnectionError>) -> Unit =
            { newState ->
                logger?.onState(newState)
                state = newState
                send(newState)
            }

        setState(EventSourceState.initial(initialRetryTime, initialEventId))

        sseLoop@ while (true) {
            val lifecycle = sseLoopStep(
                state = state,
                awaitDelay = awaitDelayLogging,
                maybeAwaitConnectivity = maybeAwaitConnectivityLogging,
                getResponseHttpCode = getResponseHttpCode,
                getResponseMediaType = getResponseMediaType,
                getResponseMediaSubType = getResponseMediaSubType,
                disposeResponse = disposeResponse,
                performRequest = {
                    Sse.prepareRequest(
                        lastEventId = state.lastEventId,
                        addHeader = addHeaderToRequest
                    )
                    performRequest()
                },
                receiveMessages = { response ->
                    val lines = readSseLinesFromBufferedStream(
                        indexOfInBuffer = { response.indexOfInBuffer(it) },
                        readAllInBuffer = { response.readAllInBuffer() },
                        bufferSize = { response.bufferSize() },
                        readInStream = { response.readInStream(it) },
                        skipInStream = { response.skipInStream(it) },
                        isStreamExhausted = { response.isStreamExhausted() },
                        logger = logger
                    )
                    parseSseLines(
                        lines = lines,
                        onRetryTime = { retryTime -> (state + retryTime).also { setState(it) } },
                        onEvent = { event -> (state + event).also { setState(it) } },
                        onEventEnd = { logger?.onEventEnd() },
                        onEventType = { logger?.onEventType(it) },
                        onEventData = { logger?.onEventData(it) },
                        onEventId = { logger?.onEventId(it) },
                        onComment = { logger?.onComment(it) },
                        onInvalidReconnectionTime = { logger?.onInvalidReconnectionTime(it) },
                        onInvalidField = { logger?.onInvalidField(it) }
                    )
                },
                computeRetryTime = { _, currentAttemptNumber ->
                    val extraRetryTime = when {
                        currentAttemptNumber <= 0 -> 0
                        else -> computeExponentialBackoffRetryTime(
                            random = random(),
                            attemptedRetries = currentAttemptNumber - 1
                        )
                    }
                    state.retryTimeOrDefault + extraRetryTime
                }
            )
            lifecycle?.let { setState(state + it) }
        }
    }

@JvmName("plusNullable")
@PublishedApi internal operator fun AwaitDelay.plus(logger: Logger?): AwaitDelay = logger?.let(::plus) ?: this
@PublishedApi internal operator fun AwaitDelay.plus(logger: Logger): AwaitDelay = { logger.onRetryDelay(it); invoke(it) }

@JvmName("plusNullable")
@PublishedApi internal operator fun MaybeAwaitConnectivity.plus(logger: Logger?): MaybeAwaitConnectivity = logger?.let(::plus) ?: this
@PublishedApi internal operator fun MaybeAwaitConnectivity.plus(logger: Logger): MaybeAwaitConnectivity = { invoke()?.plus(logger) }
@PublishedApi internal operator fun AwaitConnectivity.plus(logger: Logger): AwaitConnectivity = { logger.onAwaitingConnection(); invoke() }
