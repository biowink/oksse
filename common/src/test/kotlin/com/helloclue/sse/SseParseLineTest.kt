package com.helloclue.sse

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertTrue

class SseParseLineTest {

    @Test
    fun `onEventType is called when the field is "event" with correct event type`() {
        val eventType = "fun"
        parseSseLine(
            line = "event:$eventType",
            onEventEnd = { assertTrue(true) },
            onEventType = { assertEquals(eventType, it) },
            onEventData = { assertFails { } },
            onEventId = { assertFails { } },
            onRetryTime = { assertFails { } },
            onComment = { assertFails { } },
            onInvalidReconnectionTime = { assertFails { } },
            onInvalidField = { assertFails { } }
        )
    }

    @Test
    fun `onEventData is called when the field is "data" with correct value of data`() {
        val value = "YOHO"
        parseSseLine(
            line = "data:$value",
            onEventEnd = { assertTrue(true) },
            onEventType = { assertFails { } },
            onEventData = { assertEquals(value, it) },
            onEventId = { assertFails { } },
            onRetryTime = { assertFails { } },
            onComment = { assertFails { } },
            onInvalidReconnectionTime = { assertFails { } },
            onInvalidField = { assertFails { } }
        )
    }

    @Test
    fun `onEventData is called when the field is "data" and leading space is removed from value if any`() {
        val value = " +02"
        val expectedValue = "+02"
        parseSseLine(
            line = "data:$value",
            onEventEnd = { assertTrue(true) },
            onEventType = { assertFails { } },
            onEventData = { assertEquals(expectedValue, it) },
            onEventId = { assertFails { } },
            onRetryTime = { assertFails { } },
            onComment = { assertFails { } },
            onInvalidReconnectionTime = { assertFails { } },
            onInvalidField = { assertFails { } }
        )
    }

    @Test
    fun `onEventId is called when the field is "id"`() {
        val eventId = ""
        parseSseLine(
            line = "id:$eventId",
            onEventEnd = { assertFails { } },
            onEventType = { assertFails { } },
            onEventData = { assertFails { } },
            onEventId = { assertEquals(eventId, it) },
            onRetryTime = { assertFails { } },
            onComment = { assertTrue(true) },
            onInvalidReconnectionTime = { assertFails { } },
            onInvalidField = { assertFails { } }
        )
    }

    @Test
    fun `onRetryTime is called when the field is "retry" and the value consists of ASCII digits`() {
        val retryTime = 3000L
        parseSseLine(
            line = "retry:$retryTime",
            onEventEnd = { assertFails { } },
            onEventType = { assertFails { } },
            onEventData = { assertFails { } },
            onEventId = { assertFails { } },
            onRetryTime = { assertEquals(retryTime, it) },
            onComment = { assertTrue(true) },
            onInvalidReconnectionTime = { assertFails { } },
            onInvalidField = { assertFails { } }
        )
    }

    @Test
    fun `onInvalidReconnectionTime is called when the field is "retry" and the value is not ASCII digits`() {
        val retryTime = "¯\\_(ツ)_/¯"
        parseSseLine(
            line = "retry:$retryTime",
            onEventEnd = { assertFails { } },
            onEventType = { assertFails { } },
            onEventData = { assertFails { } },
            onEventId = { assertFails { } },
            onRetryTime = { assertFails { } },
            onComment = { assertTrue(true) },
            onInvalidReconnectionTime = { assertEquals(retryTime, it) },
            onInvalidField = { assertFails { } }
        )
    }

    @Test
    fun `onInvalidField is called when there is unknown field`() {
        val field = "¯\\_(ツ)_/¯"
        parseSseLine(
            line = "$field:",
            onEventEnd = { assertFails { } },
            onEventType = { assertFails { } },
            onEventData = { assertFails { } },
            onEventId = { assertFails { } },
            onRetryTime = { assertFails { } },
            onComment = { assertTrue(true) },
            onInvalidReconnectionTime = { assertFails { } },
            onInvalidField = { assertEquals(field, it) }
        )
    }

    @Test
    fun `onComment is called when the line starts with colon`() {
        parseSseLine(
            line = ":",
            onEventEnd = { assertFails { } },
            onEventType = { assertFails { } },
            onEventData = { assertFails { } },
            onEventId = { assertFails { } },
            onRetryTime = { assertFails { } },
            onComment = { assertTrue(true) },
            onInvalidReconnectionTime = { assertFails { } },
            onInvalidField = { assertFails { } }
        )
    }

    @Test
    fun `onEventEnd is called when there is empty line`() {
        parseSseLine(
            line = "",
            // Verify on event end is called
            onEventEnd = { assertTrue(true) },
            onEventType = { assertFails { } },
            onEventData = { assertFails { } },
            onEventId = { assertFails { } },
            onRetryTime = { assertFails { } },
            onComment = { assertFails { } },
            onInvalidReconnectionTime = { assertFails { } },
            onInvalidField = { assertFails { } }
        )
    }
}