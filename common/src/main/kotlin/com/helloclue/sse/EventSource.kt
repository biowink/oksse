package com.helloclue.sse

import kotlinx.coroutines.channels.ReceiveChannel

/**
 * Equivalent of [the `EventSource` interface](https://www.w3.org/TR/2015/REC-eventsource-20150203/#the-eventsource-interface)
 * in the [Server-Sent Events][Sse] specification.
 *
 * @see [EventSourceLifecycle]
 * @see [EventSourceState]
 * @see [connectToSse]
 * @see [close]
 */
typealias EventSource<Response, ConnectionError> =
    ReceiveChannel<EventSourceState<Response, ConnectionError>>

/**
 * Closes this [EventSource].
 *
 *
 * ### From [The `EventSource` interface](https://www.w3.org/TR/2015/REC-eventsource-20150203/#the-eventsource-interface):
 *
 * > The [`close()`][close] method must abort any instances of the fetch algorithm started for this [EventSource] object, and must set the `readyState` attribute to [`CLOSED`][EventSourceLifecycle.Closed.ClosedExplicitly].
 */
fun EventSource<*, *>.close() = cancel()
