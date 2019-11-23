@file:Suppress("UnnecessaryVariable")

package com.helloclue.sse

import kotlin.math.pow
import kotlin.math.roundToLong

/** Given the number of attempted retries and a random number returns the extra time to wait to connect again. */
fun computeExponentialBackoffRetryTime(
    random: Long,
    attemptedRetries: Int,
    timeSlotMilliseconds: Milliseconds = 3000L
): Milliseconds {
    val maxExp = 2f.pow(attemptedRetries).roundToLong()
    val randomLowerBound = 1
    val randomUpperBound = maxExp
    val randomLength = randomUpperBound - randomLowerBound + 1
    val k = (random floorMod randomLength) + randomLowerBound
    return k * timeSlotMilliseconds
}

/**
 * Returns the floor modulus of the long arguments.
 *
 * The floor modulus is `[this] - (floorDiv([this], [other]) * [other])`,
 * has the same sign as the divisor [other],
 * and is in the range of `-abs([other]) < r < +abs([other])`.
 *
 * _See [`Math.floorMod`](https://docs.oracle.com/javase/8/docs/api/java/lang/Math.html#floorMod-long-long-) from Java 8._
 *
 * @receiver the dividend
 * @param [other] the divisor
 */
private infix fun Long.floorMod(other: Long): Long = ((this % other) + this) % other
