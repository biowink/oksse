package com.helloclue.sse

import kotlinx.coroutines.channels.produce
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertTrue

class SseParseLinesTest {

    @Test
    fun `onEventData is called when sse stream contains data`(): Unit = runBlocking {
        // GIVEN
        val data = "yooohoo"

        // WHEN - produce the stream
        val lines = produce {
            send("data: $data")
            send("") // mark end of stream
        }

        // THEN
        parseSseLines(
            lines = lines,
            onEvent = { assertEquals(data, it.data) },
            onEventEnd = { assertTrue(true) }, // end of stream
            onEventType = { assertFails { } },
            onEventData = { assertEquals(data, it) },
            onEventId = { assertFails { } },
            onRetryTime = { assertFails { } },
            onComment = { assertFails { } },
            onInvalidReconnectionTime = { assertFails { } },
            onInvalidField = { assertFails { } }
        )
    }

    @Test
    fun `onEvent provides all the data from the sse stream`(): Unit = runBlocking {
        // GIVEN
        val dataList = listOf("yooohoo", "yet another yooohoo")

        // WHEN - produce the stream
        val lines = produce {
            for (data in dataList) {
                send("data: $data")
            }
            send("") // mark end of stream
        }

        // THEN
        parseSseLines(
            lines = lines,
            onEvent = { assertEquals(dataList.joinToString("\n"), it.data) },
            onEventEnd = { assertTrue(true) }, // end of stream
            onEventType = { assertFails { } },
            onEventData = { assertTrue(true) },
            onEventId = { assertFails { } },
            onRetryTime = { assertFails { } },
            onComment = { assertFails { } },
            onInvalidReconnectionTime = { assertFails { } },
            onInvalidField = { assertFails { } }
        )
    }

    @Test
    fun `onEventType is called when sse stream contains event`(): Unit = runBlocking {
        // GIVEN
        val event = "ticker-event"

        // WHEN - produce the stream
        val lines = produce {
            send("event: $event")
            send("") // mark end of stream
        }

        // THEN
        parseSseLines(
            lines = lines,
            onEvent = { assertFails { } },
            onEventEnd = { assertTrue(true) }, // end of stream
            onEventType = { assertEquals(event, it) },
            onEventData = { assertFails { } },
            onEventId = { assertFails { } },
            onRetryTime = { assertFails { } },
            onComment = { assertFails { } },
            onInvalidReconnectionTime = { assertFails { } },
            onInvalidField = { assertFails { } }
        )
    }

    @Test
    fun `onEventId is called when sse stream contains id`(): Unit = runBlocking {
        // GIVEN
        val id = "1"

        // WHEN - produce the stream
        val lines = produce {
            send("id: $id")
            send("") // mark end of stream
        }

        // THEN
        parseSseLines(
            lines = lines,
            onEvent = { assertFails { } },
            onEventEnd = { assertTrue(true) }, // end of stream
            onEventType = { assertFails { } },
            onEventData = { assertFails { } },
            onEventId = { assertEquals(id, it) },
            onRetryTime = { assertFails { } },
            onComment = { assertFails { } },
            onInvalidReconnectionTime = { assertFails { } },
            onInvalidField = { assertFails { } }
        )
    }

    @Test
    fun `verify id, event and data are set when sse stream contains them`(): Unit = runBlocking {
        // GIVEN
        val id = "1"
        val event = "fun"
        val data = "yoohoo"

        // WHEN - produce the stream
        val lines = produce {
            send("id: $id")
            send("event: $event")
            send("data: $data")
            send("") // mark end of stream
        }

        // THEN
        parseSseLines(
            lines = lines,
            onEvent = {
                assertEquals(id, it.lastEventId)
                assertEquals(event, it.type)
                assertEquals(data, it.data)
            },
            onEventEnd = { assertTrue(true) }, // end of stream
            onEventType = { assertEquals(event, it) },
            onEventData = { assertEquals(data, it) },
            onEventId = { assertEquals(id, it) },
            onRetryTime = { assertFails { } },
            onComment = { assertFails { } },
            onInvalidReconnectionTime = { assertFails { } },
            onInvalidField = { assertFails { } }
        )
    }
}