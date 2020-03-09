package com.helloclue.sse

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertTrue

class SseParseLineEndTest {

    @Test
    fun `onLf is called when the line feed character is at the beginning`() {
        // Index of line feed character in the line
        val index = 0L
        parseSseLineEnd(
            indexOfCr = { -1 },
            indexOfLf = { index },
            onCrLf = { assertFails {  } },
            onLf = { assertEquals(index, it) },
            onCr = { assertFails {  } },
            onEof = { assertFails {  } }
        )
    }

    @Test
    fun `onLf is called when there is a line feed character in the line`() {
        // Index of line feed character in the line
        val index = 14L
        parseSseLineEnd(
            indexOfCr = { -1 },
            indexOfLf = { index },
            onCrLf = { assertFails {  } },
            onLf = { assertEquals(index, it) },
            onCr = { assertFails {  } },
            onEof = { assertFails {  } }
        )
    }

    @Test
    fun `onCr is called when there is a carriage return character in the line`() {
        // Index of carriage return character in the line
        val index = 15L
        parseSseLineEnd(
            indexOfCr = { index },
            indexOfLf = { -1 },
            onCrLf = { assertFails {  } },
            onLf = { assertFails {  } },
            onCr = { assertEquals(index, it) },
            onEof = { assertFails {  } }
        )
    }

    @Test
    fun `onCrLf is called when there is a carriage return and immediately a line feed character next in the line`() {
        // Index of carriage return character in the line
        val index = 15L
        parseSseLineEnd(
            indexOfCr = { index },
            indexOfLf = { index + 1 },
            onCrLf = { assertEquals(index, it) },
            onLf = { assertFails {  } },
            onCr = { assertFails {  } },
            onEof = { assertFails {  } }
        )
    }

    @Test
    fun `onLf is called when there is a line feed character before carriage return character in the line`() {
        // Index of line feed character in the line
        val index = 14L
        parseSseLineEnd(
            indexOfCr = { 74 },
            indexOfLf = { index },
            onCrLf = { assertFails {  } },
            onLf = { assertEquals(index, it) },
            onCr = { assertFails {  } },
            onEof = { assertFails {  } }
        )
    }

    @Test
    fun `onCr is called when there is a carriage return character before line feed character in the line`() {
        // Index of carriage return character in the line
        val index = 15L
        parseSseLineEnd(
            indexOfCr = { index },
            indexOfLf = { 74 },
            onCrLf = { assertFails {  } },
            onLf = { assertFails {  } },
            onCr = { assertEquals(index, it) },
            onEof = { assertFails {  } }
        )
    }

    @Test
    fun `onEof is called when there is no next line terminator character in the line`() {
        parseSseLineEnd(
            indexOfCr = { -1 },
            indexOfLf = { -1 },
            onCrLf = { assertFails {  } },
            onLf = { assertFails {  } },
            onCr = { assertFails {  } },
            onEof = { assertTrue(true) }
        )
    }
}