package com.helloclue.sse.okhttp

import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okio.BufferedSource
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

suspend fun Call.Factory.enqueueSuspending(request: Request): Response =
    newCall(request).let { call ->
        suspendCancellableCoroutine { continuation ->
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) = continuation.resume(response)
                override fun onFailure(call: Call, e: IOException) = continuation.resumeWithException(e)
            })
        }
    }

fun OkHttpClient.Builder.applySseDefaults(): OkHttpClient.Builder = this
    .readTimeout(0, TimeUnit.SECONDS)
    .retryOnConnectionFailure(true)

fun OkHttpClient.shutdownDispatcher() =
    dispatcher().executorService().shutdown()

/** Like [BufferedSource.exhausted], but doesn't throw if the source is closed or interrupted. */
@PublishedApi
internal fun BufferedSource.exhaustedSafe() =
    when {
        !isOpen -> true
        else ->
            try {
                exhausted()
            } catch (_: IOException) {
                true
            }
    }
