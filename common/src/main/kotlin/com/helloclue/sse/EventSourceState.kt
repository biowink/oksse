package com.helloclue.sse

import com.helloclue.sse.EventSourceState.WithEvent
import com.helloclue.sse.EventSourceState.WithoutEvent
import com.helloclue.sse.Sse.Event

typealias Milliseconds = Long

typealias EventType = String
typealias EventData = String
typealias EventId = String

/**
 * ### From [The `EventSource` interface](https://www.w3.org/TR/2015/REC-eventsource-20150203/#the-eventsource-interface):
 *
 * > … each [EventSource] object has the following associated with it:
 *
 * > A [**reconnection time**][retryTime], in [milliseconds][Milliseconds]. This must initially be a user-agent-defined value, probably in [the region of a few seconds][Sse.defaultRetryTime].
 *
 * > A [**last event ID string**][lastEventId]. This must initially be [the empty string][Sse.NoEventId].
 *
 * @see [EventSource]
 */
sealed class EventSourceState<out Response, out ConnectionError> {
    abstract val lifecycle: EventSourceLifecycle<Response, ConnectionError>?
    abstract val retryTime: Milliseconds?
    abstract val lastEventId: EventId?

    data class WithoutEvent<out Response, out ConnectionError>(
        override val lifecycle: EventSourceLifecycle<Response, ConnectionError>?,
        override val retryTime: Milliseconds?,
        override val lastEventId: EventId?
    ) : EventSourceState<Response, ConnectionError>()

    data class WithEvent<out Response, out ConnectionError>(
        override val lifecycle: EventSourceLifecycle<Response, ConnectionError>?,
        override val retryTime: Milliseconds?,
        val lastEvent: Event
    ) : EventSourceState<Response, ConnectionError>() {
        override val lastEventId get() = lastEvent.lastEventId
    }

    companion object {
        fun <Response, ConnectionError> initial(
            retryTime: Milliseconds?,
            lastEventId: EventId?
        ): EventSourceState<Response, ConnectionError> =
            WithoutEvent(
                lifecycle = null,
                retryTime = retryTime,
                lastEventId = lastEventId
            )
    }
}

inline val EventSourceState<*, *>.retryTimeOrDefault
    get() = retryTime ?: Sse.defaultRetryTime

operator fun <Response, ConnectionError> EventSourceState<Response, ConnectionError>.plus(
    lifecycle: EventSourceLifecycle<Response, ConnectionError>
) =
    when (this) {
        is WithoutEvent -> copy(lifecycle = lifecycle)
        is WithEvent -> copy(lifecycle = lifecycle)
    }

operator fun <Response, ConnectionError> EventSourceState<Response, ConnectionError>.plus(
    retryTime: Milliseconds
) =
    when (this) {
        is WithoutEvent -> copy(retryTime = retryTime)
        is WithEvent -> copy(retryTime = retryTime)
    }

operator fun <Response, ConnectionError> EventSourceState<Response, ConnectionError>.plus(
    event: Event
) =
    when (this) {
        is WithoutEvent -> WithEvent(lastEvent = event, lifecycle = lifecycle, retryTime = retryTime)
        is WithEvent -> copy(lastEvent = event)
    }
