package com.helloclue.sse.okhttp

import com.helloclue.sse.AwaitConnectivity
import com.helloclue.sse.EventId
import com.helloclue.sse.Logger
import com.helloclue.sse.MaybeAwaitConnectivity
import com.helloclue.sse.Milliseconds
import com.helloclue.sse.connectToSse
import kotlinx.coroutines.CoroutineScope
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okio.BufferedSource
import java.io.IOException
import kotlin.random.Random

@PublishedApi
internal val NoWait: AwaitConnectivity? = null

/** @see [connectToSse] */
fun CoroutineScope.connectToSse(
    client: OkHttpClient,
    requestBuilder: Request.Builder,
    initialRetryTime: Milliseconds? = null,
    initialEventId: EventId? = null,
    maybeAwaitConnectivity: MaybeAwaitConnectivity = { NoWait },
    logger: Logger? = null,
    random: Random = Random.Default
) =
    connectToSse<Response, IOException>(
        addHeaderToRequest = { name, value -> requestBuilder.header(name, value) },
        performRequest = { client.enqueueSuspending(requestBuilder.build()) },
        getResponseHttpCode = { code() },
        getResponseMediaType = { contentType?.type() },
        getResponseMediaSubType = { contentType?.subtype() },
        indexOfInBuffer = { source.buffer.indexOf(it) },
        readAllInBuffer = { source.buffer.readUtf8() },
        bufferSize = { source.buffer.size },
        readInStream = { source.readUtf8(it) },
        skipInStream = { source.skip(it) },
        isStreamExhausted = { source.exhaustedSafe() },
        disposeResponse = { body()?.close() },
        initialRetryTime = initialRetryTime,
        initialEventId = initialEventId,
        maybeAwaitConnectivity = maybeAwaitConnectivity,
        logger = logger,
        random = random::nextLong
    )

inline val Response.contentType: MediaType?
    get() = body()?.contentType()

inline val Response.source: BufferedSource
    get() = body()!!.source()
