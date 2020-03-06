package com.helloclue.sse

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

class BackoffTest {

    @Test
    fun `the exponential backoff time starts with default of 3000 milliseconds`() {
        val backoffTime = computeExponentialBackoffRetryTime(
            random = Random.nextLong(),
            attemptedRetries = 1
        )
        assertEquals(3000, backoffTime)
    }

    @Test
    fun `the exponential backoff time should increase exponentially`() {
        val random = Random.nextLong(Long.MAX_VALUE)
        val exponentialValues = mutableListOf<Milliseconds>()
        for (i in 1..10) {
            exponentialValues.add(
                computeExponentialBackoffRetryTime(
                    random = random,
                    attemptedRetries = i
                )
            )
        }
        // Checks if the list is in increasing order
        val isIncreasing = exponentialValues
            .filterIndexed { index, value -> value > exponentialValues.getOrElse(index + 1) { Long.MAX_VALUE } }
            .isEmpty()

        assertEquals(true, isIncreasing)
    }
}