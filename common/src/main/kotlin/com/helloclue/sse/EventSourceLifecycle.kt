package com.helloclue.sse

import com.helloclue.sse.EventSourceLifecycle.Closed
import com.helloclue.sse.EventSourceLifecycle.Connecting
import com.helloclue.sse.EventSourceLifecycle.Open

/**
 * Equivalent of `EventSource.readyState` attribute values in the [Server-Sent Events][Sse] specification.
 *
 *
 * ### From [The `EventSource` interface](https://www.w3.org/TR/2015/REC-eventsource-20150203/#the-eventsource-interface):
 *
 * > The `readyState` attribute represents the state of the connection. It can have the following values:
 *
 * > **[_`CONNECTING`_][Connecting] (numeric value 0)**
 *
 * >   >   The connection has not yet been established, or it was closed and the user agent is [reconnecting][Connecting.retry].
 *
 * > **[_**`OPEN`**_][Open] (numeric value 1)**
 *
 * >   >   The user agent has an open connection and is dispatching events as it receives them.
 *
 * > **[_**`CLOSED`**_][Closed] (numeric value 2)**
 *
 * >   >   The connection is not open, and the user agent is not trying to reconnect. Either there was a [fatal error][Closed.ByError] or the [`close()`][close] method [was invoked][Closed.Explicitly.ByClient].
 *
 *
 * @see [EventSource]
 */
sealed class EventSourceLifecycle<out Response, out ConnectionError> {
    abstract val response: Response?

    sealed class Connecting<out Response, out ConnectionError>
        : EventSourceLifecycle<Response, ConnectionError>() {
        override val response: Nothing? get() = null

        abstract val retry: EventSourceConnectionRetry<Response, ConnectionError>?

        data class AwaitingRetry<out Response, out ConnectionError>(
            override val retry: EventSourceConnectionRetry<Response, ConnectionError>?
        ) : Connecting<Response, ConnectionError>()

        /**
         * ### From [Processing model](https://www.w3.org/TR/2015/REC-eventsource-20150203/#processing-model):
         *
         * > … if the operating system has reported that there is no network connectivity, user agents might [wait for the operating system to announce that the network connection has returned][AwaitingConnectivity] before retrying.
         */
        data class AwaitingConnectivity<out Response, out ConnectionError>(
            override val retry: EventSourceConnectionRetry<Response, ConnectionError>?
        ) : Connecting<Response, ConnectionError>()

        data class PerformingRequest<out Response, out ConnectionError>(
            override val retry: EventSourceConnectionRetry<Response, ConnectionError>?
        ) : Connecting<Response, ConnectionError>()
    }

    data class Open<out Response>(
        override val response: Response
    ) : EventSourceLifecycle<Response, Nothing>()

    sealed class Closed<out Response>
        : EventSourceLifecycle<Response, Nothing>() {

        sealed class ByError<out Response>
            : Closed<Response>() {

            data class UnexpectedError<out Response>(
                val error: Throwable,
                override val response: Response?
            ) : Closed.ByError<Response>()

            data class ResponseUnsuccessfulFatal<out Response>(
                override val response: Response
            ) : Closed.ByError<Response>()

            data class InvalidContentType<out Response>(
                val mediaType: String?,
                val mediaSubType: String?,
                override val response: Response
            ) : Closed.ByError<Response>()
        }

        sealed class Explicitly<out Response>
            : Closed<Response>() {

            /**
             * ### From [Processing model](https://www.w3.org/TR/2015/REC-eventsource-20150203/#processing-model):
             *
             * > … a client can be told to stop reconnecting using the `HTTP 204 No Content` response code.
             */
            data class ByServer<out Response>(
                override val response: Response
            ) : Closed.Explicitly<Response>()

            data class ByClient<out Response>(
                override val response: Response
            ) : Closed.Explicitly<Response>()
        }
    }
}
