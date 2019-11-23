package com.helloclue.sse.sample

import com.google.gson.GsonBuilder
import com.google.gson.JsonParseException
import com.google.gson.JsonParser
import com.helloclue.sse.Comment
import com.helloclue.sse.EventData
import com.helloclue.sse.EventId
import com.helloclue.sse.EventSourceState
import com.helloclue.sse.EventType
import com.helloclue.sse.Line
import com.helloclue.sse.LineEnding
import com.helloclue.sse.LinePartial
import com.helloclue.sse.Logger
import com.helloclue.sse.Milliseconds
import com.helloclue.sse.okhttp.applySseDefaults
import com.helloclue.sse.okhttp.connectToSse
import com.helloclue.sse.okhttp.shutdownDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.logging.HttpLoggingInterceptor.Level.HEADERS
import kotlin.random.Random

internal fun sseLoop(url: String, authToken: String?) {
    val prettyPrint = prettyPrint()

    val client = OkHttpClient.Builder()
        .addNetworkInterceptor(HttpLoggingInterceptor { log("NetworkLog='$it'") }.setLevel(HEADERS))
        .applySseDefaults()
        .build()

    val requestBuilder = Request.Builder()
        .url(url)
        .header("Accept-Language", "en")
        .apply { authToken?.let { header("Authorization", "Token $authToken") } }

    val randomSeed = Random.nextLong()
    log("RandomSeed=$randomSeed")

    runBlocking {
        withContext(Dispatchers.Default) {
            val eventSource = connectToSse(
                client,
                requestBuilder,
                random = Random(randomSeed),
                logger = object : Logger {
                    override fun onStart() = log("Start")
                    override fun onLine(line: Line) = log("Line='$line'")
                    override fun onLineEnding(lineEnding: LineEnding) = log("LineEnding='$lineEnding'")
                    override fun onLinePartial(linePartial: LinePartial) = log("LinePartial='$linePartial'")
                    override fun onEndOfFile() = log("EOF 🚫")
                    override fun onEventEnd() = log("EventEnd")
                    override fun onEventType(type: EventType) = log("EventType='$type'")
                    override fun onEventId(id: EventId) = log("EventId='$id'")
                    override fun onComment(comment: Comment) = log("Comment='$comment'")
                    override fun onInvalidReconnectionTime(value: String) = log("InvalidReconnectionTime='$value'")
                    override fun onInvalidField(field: String) = log("InvalidField='$field'")
                    override fun onAwaitingConnection() = log("OnAwaitingConnection")
                    override fun onRetryDelay(milliseconds: Milliseconds) = log("OnRetryDelay='$milliseconds'")
                    override fun onState(state: EventSourceState<*, *>) = log("State='$state'")
                    override fun onEventData(data: EventData) {
                        log("EventData='$data'")
                        prettyPrint(data)?.prependIndent("| ")?.let { json -> log("EventDataPrettyPrint=\n$json") }
                    }
                }
            )

            for (state in eventSource) {
                println(">>>> state -> $state")
            }
        }
    }

    client.shutdownDispatcher()
}

private fun log(message: String) =
    message
        .lines()
        .mapIndexed { index, line -> (if (index == 0) "-- " else "   ") + line }
        .joinToString("\n")
        .let(::println)

private fun prettyPrint(): (json: String) -> String? {
    val gson = GsonBuilder().setPrettyPrinting().create()
    val parser = JsonParser()
    return { maybeJson: String ->
        try {
            parser.parse(maybeJson)
                .takeIf { it.isJsonObject }
                ?.asJsonObject
                ?.let(gson::toJson)
        }
        catch (_: JsonParseException) {
            null
        }
    }
}
