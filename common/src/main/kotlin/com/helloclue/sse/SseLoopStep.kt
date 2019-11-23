@file:Suppress("ReplaceSingleLineLet")

package com.helloclue.sse

import com.helloclue.sse.EventSourceConnectionRetry.Reason.ConnectionClosed
import com.helloclue.sse.EventSourceConnectionRetry.Reason.ConnectionUnsuccessful
import com.helloclue.sse.EventSourceConnectionRetry.Reason.ResponseUnsuccessfulRetriable
import com.helloclue.sse.EventSourceLifecycle.Closed
import com.helloclue.sse.EventSourceLifecycle.Connecting
import com.helloclue.sse.EventSourceLifecycle.Open
import kotlinx.coroutines.CancellationException

typealias ComputeRetryTime<Response, ConnectionError> = (
    reason: EventSourceConnectionRetry.Reason<Response, ConnectionError>,
    attemptNumber: Int
) -> Milliseconds

typealias AwaitDelay = suspend (milliseconds: Milliseconds) -> Unit

typealias AwaitConnectivity = suspend () -> Unit
typealias MaybeAwaitConnectivity = () -> AwaitConnectivity?

internal typealias GetHttpCode<Response> = Response.() -> Int?
internal typealias GetMediaType<Response> = Response.() -> String?
internal typealias GetMediaSubType<Response> = Response.() -> String?
internal typealias Dispose<Response> = Response.() -> Unit

@PublishedApi
internal suspend inline fun <Response, reified ConnectionError> sseLoopStep(
    state: EventSourceState<Response, ConnectionError>,
    noinline awaitDelay: AwaitDelay,
    noinline maybeAwaitConnectivity: MaybeAwaitConnectivity,
    performRequest: () -> Response,
    getResponseHttpCode: GetHttpCode<Response>,
    getResponseMediaType: GetMediaType<Response>,
    getResponseMediaSubType: GetMediaSubType<Response>,
    receiveMessages: (Response) -> Unit,
    disposeResponse: Dispose<Response>,
    computeRetryTime: ComputeRetryTime<Response, ConnectionError>
): EventSourceLifecycle<Response, ConnectionError>? =
    state.lifecycle.let { lifecycle ->
        try {
            when (lifecycle) {
                null -> {
                    val retryReason = null // initial
                    Connecting.AwaitingRetry(computeRetry(retryReason, lifecycle, computeRetryTime))
                }

                is Connecting.AwaitingRetry -> {
                    val retryTime = lifecycle.retry?.retryTime ?: 0
                    if (retryTime > 0) awaitDelay(retryTime)
                    Connecting.AwaitingConnectivity(lifecycle.retry)
                }

                is Connecting.AwaitingConnectivity -> {
                    maybeAwaitConnectivity()?.invoke()
                    Connecting.PerformingRequest(lifecycle.retry)
                }

                is Connecting.PerformingRequest -> {
                    val response = performRequest()
                    val httpCode = getResponseHttpCode(response)
                    when {
                        Sse.isValidResponseCode(httpCode) -> {
                            val mediaType = getResponseMediaType(response)
                            val mediaSubType = getResponseMediaSubType(response)
                            when {
                                Sse.isValidContentType(mediaType, mediaSubType) ->
                                    Open(response)
                                else ->
                                    Closed.ByError.InvalidContentType(mediaType, mediaSubType, response)
                            }
                        }
                        Sse.isRecoverableResponseCode(httpCode) -> {
                            val retryReason = ResponseUnsuccessfulRetriable(httpCode, response)
                            Connecting.AwaitingRetry(computeRetry(retryReason, lifecycle, computeRetryTime))
                        }
                        Sse.isNoContentResponseCode(httpCode) ->
                            Closed.Explicitly.ByServer(response)
                        else ->
                            Closed.ByError.ResponseUnsuccessfulFatal(response)
                    }
                }

                is Open -> {
                    val response = lifecycle.response
                    response.use(disposeResponse, receiveMessages)
                    val retryReason = ConnectionClosed(response)
                    Connecting.AwaitingRetry(computeRetry(retryReason, lifecycle, computeRetryTime))
                }

                is Closed -> null // stop loop
            }
        } catch (cancel: CancellationException) {
            throw cancel // propagate coroutine cancellation
        } catch (error: Throwable) {
            val connectionError = error as? ConnectionError
            if (connectionError != null) {
                val retryReason = ConnectionUnsuccessful(connectionError)
                Connecting.AwaitingRetry(computeRetry(retryReason, lifecycle, computeRetryTime))
            } else {
                val response = (lifecycle as? Open)?.response
                Closed.ByError.UnexpectedError(error, response)
            }
        }
    }

@PublishedApi
internal inline fun <ConnectionError, Response> computeRetry(
    retryReason: EventSourceConnectionRetry.Reason<Response, ConnectionError>?,
    lifecycle: EventSourceLifecycle<Response, ConnectionError>?,
    computeRetryTime: ComputeRetryTime<Response, ConnectionError>
): EventSourceConnectionRetry<Response, ConnectionError>? =
    when (retryReason) {
        null -> null
        else -> {
            val previousDelay = (lifecycle as? Connecting)?.retry
            val attemptNumber = previousDelay?.attemptNumber?.plus(1) ?: 0
            val retryDelayTime = computeRetryTime(retryReason, attemptNumber)
            EventSourceConnectionRetry(
                reason = retryReason,
                retryTime = retryDelayTime,
                attemptNumber = attemptNumber
            )
        }
    }

/** Same as the `kotlin.io.use` extension function over `java.io.Closeable`, but generic for any type [T]. */
@PublishedApi
internal inline fun <T, R> T.use(
    close: T.() -> Unit,
    block: (T) -> R
): R {
    var exception: Throwable? = null
    try {
        return block(this)
    } catch (e: Throwable) {
        exception = e
        throw e
    } finally {
        when (exception) {
            null -> close()
            else -> try {
                close()
            } catch (_: Throwable) {
                // ignored here
            }
        }
    }
}
