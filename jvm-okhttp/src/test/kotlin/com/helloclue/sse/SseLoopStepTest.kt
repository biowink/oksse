package com.helloclue.sse

import com.helloclue.sse.EventSourceConnectionRetry.Reason.*
import com.helloclue.sse.EventSourceLifecycle.*
import com.helloclue.sse.okhttp.NoWait
import kotlinx.coroutines.runBlocking
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import org.junit.Test
import java.io.IOException
import kotlin.test.assertEquals

class SseLoopStepTest {

    @Test
    fun `sse event state steps to awaiting retry when lifecycle is null`(): Unit = runBlocking {
        // GIVEN
        val expected = Connecting.AwaitingRetry<Response, Throwable>(null)
        val state = EventSourceState.initial<Response, Throwable>(
            retryTime = 3000,
            lastEventId = null
        )

        // WHEN
        val lifecycle = sseLoopStep(
            state = state,
            awaitDelay = { },
            maybeAwaitConnectivity = { NoWait },
            performRequest = { Response.Builder().build() },
            getResponseHttpCode = { 200 },
            getResponseMediaType = { "" },
            getResponseMediaSubType = { "" },
            receiveMessages = {},
            disposeResponse = {},
            computeRetryTime = TestUtils.computeRetryTime()
        )

        // THEN
        assertEquals(expected, lifecycle)
    }

    @Test
    fun `sse event state steps to awaiting connectivity when lifecycle is awaiting retry`(): Unit = runBlocking {
        // GIVEN
        val expected = Connecting.AwaitingConnectivity<Response, Throwable>(null)
        val state = EventSourceState.WithoutEvent<Response, Throwable>(
            lifecycle = Connecting.AwaitingRetry(null),
            retryTime = null,
            lastEventId = null
        )

        // WHEN
        val lifecycle = sseLoopStep(
            state = state,
            awaitDelay = { },
            maybeAwaitConnectivity = { NoWait },
            performRequest = { Response.Builder().build() },
            getResponseHttpCode = { 200 },
            getResponseMediaType = { "" },
            getResponseMediaSubType = { "" },
            receiveMessages = {},
            disposeResponse = {},
            computeRetryTime = TestUtils.computeRetryTime()
        )

        // THEN
        assertEquals(expected, lifecycle)
    }

    @Test
    fun `sse event state steps to performing request when lifecycle is awaiting connectivity`(): Unit = runBlocking {
        // GIVEN
        val expected = Connecting.PerformingRequest<Response, Throwable>(null)
        val state = EventSourceState.WithoutEvent<Response, Throwable>(
            lifecycle = Connecting.AwaitingConnectivity(null),
            retryTime = null,
            lastEventId = null
        )

        // WHEN
        val lifecycle = sseLoopStep(
            state = state,
            awaitDelay = { },
            maybeAwaitConnectivity = { NoWait },
            performRequest = { Response.Builder().build() },
            getResponseHttpCode = { 200 },
            getResponseMediaType = { "" },
            getResponseMediaSubType = { "" },
            receiveMessages = {},
            disposeResponse = {},
            computeRetryTime = TestUtils.computeRetryTime()
        )

        // THEN
        assertEquals(expected, lifecycle)
    }

    @Test
    fun `sse event state steps to open when lifecycle is performing request and response is valid`(): Unit = runBlocking {
        // GIVEN
        val request = Request.Builder()
            .url("http://localhost")
            .build()
        val response = Response.Builder()
            .request(request)
            .code(200)
            .message("")
            .protocol(Protocol.HTTP_1_1)
            .build()
        val expected = Open<Response>(response)
        val state = EventSourceState.WithoutEvent<Response, Throwable>(
            lifecycle = Connecting.PerformingRequest(null),
            retryTime = null,
            lastEventId = null
        )

        // WHEN
        val lifecycle = sseLoopStep(
            state = state,
            awaitDelay = { },
            maybeAwaitConnectivity = { NoWait },
            performRequest = { response },
            getResponseHttpCode = { code() },
            getResponseMediaType = { "text" },
            getResponseMediaSubType = { "event-stream" },
            receiveMessages = {},
            disposeResponse = {},
            computeRetryTime = TestUtils.computeRetryTime()
        )

        // THEN
        assertEquals(expected, lifecycle)
    }

    @Test
    fun `sse event state steps to invalid content type when lifecycle is performing request and response contains invalid content type`(): Unit = runBlocking {
        // GIVEN
        val request = Request.Builder()
            .url("http://localhost")
            .build()
        val response = Response.Builder()
            .request(request)
            .code(200)
            .message("")
            .protocol(Protocol.HTTP_1_1)
            .build()
        val expected = Closed.ByError.InvalidContentType<Response>("", "", response)
        val state = EventSourceState.WithoutEvent<Response, Throwable>(
            lifecycle = Connecting.PerformingRequest(null),
            retryTime = null,
            lastEventId = null
        )

        // WHEN
        val lifecycle = sseLoopStep(
            state = state,
            awaitDelay = { },
            maybeAwaitConnectivity = { NoWait },
            performRequest = { response },
            getResponseHttpCode = { code() },
            getResponseMediaType = { "" },
            getResponseMediaSubType = { "" },
            receiveMessages = {},
            disposeResponse = {},
            computeRetryTime = TestUtils.computeRetryTime()
        )

        // THEN
        assertEquals(expected, lifecycle)
    }

    @Test
    fun `sse event state steps to awaiting retry when lifecycle is performing request and response is invalid`(): Unit = runBlocking {
        // GIVEN
        val request = Request.Builder()
            .url("http://localhost")
            .build()
        val response = Response.Builder()
            .request(request)
            .code(500)
            .message("")
            .protocol(Protocol.HTTP_1_1)
            .build()
        val retryTime = 300L
        val retry = EventSourceConnectionRetry(
            reason = ResponseUnsuccessfulRetriable(500, response),
            retryTime = retryTime,
            attemptNumber = 0
        )
        val expected = Connecting.AwaitingRetry<Response, Throwable>(retry)
        val state = EventSourceState.WithoutEvent<Response, Throwable>(
            lifecycle = Connecting.PerformingRequest(null),
            retryTime = null,
            lastEventId = null
        )

        // WHEN
        val lifecycle = sseLoopStep(
            state = state,
            awaitDelay = { },
            maybeAwaitConnectivity = { NoWait },
            performRequest = { response },
            getResponseHttpCode = { code() },
            getResponseMediaType = { "text" },
            getResponseMediaSubType = { "event-stream" },
            receiveMessages = {},
            disposeResponse = {},
            computeRetryTime = { _, _ -> retryTime }
        )

        // THEN
        assertEquals(expected, lifecycle)
    }

    @Test
    fun `sse event state steps to closed by server when lifecycle is performing request and response contains no content`(): Unit = runBlocking {
        // GIVEN
        val request = Request.Builder()
            .url("http://localhost")
            .build()
        val response = Response.Builder()
            .request(request)
            .code(204)
            .message("")
            .protocol(Protocol.HTTP_1_1)
            .build()
        val expected = Closed.Explicitly.ByServer<Response>(response)
        val state = EventSourceState.WithoutEvent<Response, Throwable>(
            lifecycle = Connecting.PerformingRequest(null),
            retryTime = null,
            lastEventId = null
        )

        // WHEN
        val lifecycle = sseLoopStep(
            state = state,
            awaitDelay = { },
            maybeAwaitConnectivity = { NoWait },
            performRequest = { response },
            getResponseHttpCode = { code() },
            getResponseMediaType = { "" },
            getResponseMediaSubType = { "" },
            receiveMessages = {},
            disposeResponse = {},
            computeRetryTime = TestUtils.computeRetryTime()
        )

        // THEN
        assertEquals(expected, lifecycle)
    }

    @Test
    fun `sse event state steps to closed by error when lifecycle is performing request and response is not supported`(): Unit = runBlocking {
        // GIVEN
        val request = Request.Builder()
            .url("http://localhost")
            .build()
        val response = Response.Builder()
            .request(request)
            .code(208) // Some weird status code
            .message("")
            .protocol(Protocol.HTTP_1_1)
            .build()
        val expected = Closed.ByError.ResponseUnsuccessfulFatal<Response>(response)
        val state = EventSourceState.WithoutEvent<Response, Throwable>(
            lifecycle = Connecting.PerformingRequest(null),
            retryTime = null,
            lastEventId = null
        )

        // WHEN
        val lifecycle = sseLoopStep(
            state = state,
            awaitDelay = { },
            maybeAwaitConnectivity = { NoWait },
            performRequest = { response },
            getResponseHttpCode = { code() },
            getResponseMediaType = { "" },
            getResponseMediaSubType = { "" },
            receiveMessages = {},
            disposeResponse = {},
            computeRetryTime = TestUtils.computeRetryTime()
        )

        // THEN
        assertEquals(expected, lifecycle)
    }

    @Test
    fun `sse event state steps to awaiting retry on connection closed when lifecycle is open and response is consumed`(): Unit = runBlocking {
        // GIVEN
        val request = Request.Builder()
            .url("http://localhost")
            .build()
        val response = Response.Builder()
            .request(request)
            .code(200)
            .message("")
            .protocol(Protocol.HTTP_1_1)
            .build()
        val startLifecycle = Open(response)
        val expected = Connecting.AwaitingRetry<Response, Throwable>(computeRetry(
            retryReason = ConnectionClosed(response),
            lifecycle = startLifecycle,
            computeRetryTime = TestUtils.computeRetryTime()
        ))
        val state = EventSourceState.WithoutEvent<Response, Throwable>(
            lifecycle = startLifecycle,
            retryTime = null,
            lastEventId = null
        )

        // WHEN
        val lifecycle = sseLoopStep(
            state = state,
            awaitDelay = { },
            maybeAwaitConnectivity = { NoWait },
            performRequest = { response },
            getResponseHttpCode = { code() },
            getResponseMediaType = { "" },
            getResponseMediaSubType = { "" },
            receiveMessages = {},
            disposeResponse = {},
            computeRetryTime = TestUtils.computeRetryTime()
        )

        // THEN
        assertEquals(expected, lifecycle)
    }

    @Test
    fun `sse event state steps to null when lifecycle is closed`(): Unit = runBlocking {
        // GIVEN
        val request = Request.Builder()
            .url("http://localhost")
            .build()
        val response = Response.Builder()
            .request(request)
            .code(200)
            .message("")
            .protocol(Protocol.HTTP_1_1)
            .build()
        val expected = null
        val state = EventSourceState.WithoutEvent<Response, Throwable>(
            lifecycle = Closed.Explicitly.ByServer(response),
            retryTime = null,
            lastEventId = null
        )

        // WHEN
        val lifecycle = sseLoopStep(
            state = state,
            awaitDelay = { },
            maybeAwaitConnectivity = { NoWait },
            performRequest = { response },
            getResponseHttpCode = { code() },
            getResponseMediaType = { "" },
            getResponseMediaSubType = { "" },
            receiveMessages = {},
            disposeResponse = {},
            computeRetryTime = TestUtils.computeRetryTime()
        )

        // THEN
        assertEquals(expected, lifecycle)
    }

    @Test
    fun `sse event state steps to awaiting retry on connection unsuccessful when error is thrown`(): Unit = runBlocking {
        // GIVEN
        val request = Request.Builder()
            .url("http://localhost")
            .build()
        val response = Response.Builder()
            .request(request)
            .code(200)
            .message("")
            .protocol(Protocol.HTTP_1_1)
            .build()
        val startLifecycle = Open(response)
        val ioException = IOException()
        val expected = Connecting.AwaitingRetry<Response, Throwable>(computeRetry(
            retryReason = ConnectionUnsuccessful(ioException),
            lifecycle = startLifecycle,
            computeRetryTime = TestUtils.computeRetryTime()
        ))
        val state = EventSourceState.WithoutEvent<Response, Throwable>(
            lifecycle = startLifecycle,
            retryTime = null,
            lastEventId = null
        )

        // WHEN
        val lifecycle = sseLoopStep(
            state = state,
            awaitDelay = { },
            maybeAwaitConnectivity = { NoWait },
            performRequest = { response },
            getResponseHttpCode = { code() },
            getResponseMediaType = { "" },
            getResponseMediaSubType = { "" },
            receiveMessages = { throw ioException },
            disposeResponse = {},
            computeRetryTime = TestUtils.computeRetryTime()
        )

        // THEN
        assertEquals(expected, lifecycle)
    }

    // region Retry

    @Test
    fun `compute retry returns null when there is no retry reason`() {
        // WHEN
        val retry = computeRetry<Throwable, Response>(
            retryReason = null,
            lifecycle = null,
            computeRetryTime = { _, _ -> 0 }
        )

        // THEN
        assertEquals(null, retry)
    }

    @Test
    fun `compute retry when returns something when there is a retry reason`() {
        // GIVEN
        val retryReason = ConnectionUnsuccessful(IOException())
        val retryTime = 3000L
        val attemptNumber = 0

        // WHEN
        val retry = computeRetry<Throwable, Response>(
            retryReason = retryReason,
            lifecycle = null,
            computeRetryTime = { _, _ -> retryTime }
        ) ?: return

        // THEN
        assertEquals(retryReason, retry.reason)
        assertEquals(retryTime, retry.retryTime)
        assertEquals(attemptNumber, retry.attemptNumber)
    }

    // endregion
}