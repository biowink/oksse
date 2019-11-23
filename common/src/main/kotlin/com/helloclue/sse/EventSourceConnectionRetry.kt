package com.helloclue.sse

data class EventSourceConnectionRetry<out Response, out ConnectionError>(
    val reason: Reason<Response, ConnectionError>,
    val retryTime: Milliseconds,
    val attemptNumber: Int
) {
    sealed class Reason<out Response, out ConnectionError> {
        abstract val response: Response?
        abstract val connectionError: ConnectionError?

        data class ConnectionUnsuccessful<out ConnectionError>(
            override val connectionError: ConnectionError
        ) : Reason<Nothing, ConnectionError>() {
            override val response: Nothing? get() = null
        }

        data class ResponseUnsuccessfulRetriable<out Response>(
            val httpCode: Int?,
            override val response: Response
        ) : Reason<Response, Nothing>() {
            override val connectionError: Nothing? get() = null
        }

        data class ConnectionClosed<out Response>(
            override val response: Response
        ) : Reason<Response, Nothing>() {
            override val connectionError: Nothing? get() = null
        }
    }
}
