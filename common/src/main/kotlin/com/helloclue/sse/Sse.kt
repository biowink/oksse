package com.helloclue.sse

internal typealias AddHeader = (name: String, value: String) -> Unit

/**
 * See [**Server-Sent Events** (W3C Recommendation 03 February 2015)](https://www.w3.org/TR/2015/REC-eventsource-20150203)
 *
 * @see [Event]
 * @see [EventSource]
 * @see [connectToSse]
 */
object Sse {

    /**
     * Equivalent of the `MessageEvent` interface in the [Server-Sent Events][Sse] specification.
     *
     *
     * ### From [Interpreting an event stream](https://www.w3.org/TR/2015/REC-eventsource-20150203/#event-stream-interpretation):
     *
     * > When the user agent is required to dispatch the event, then the user agent must act as follows:
     *
     * > Create an event that uses the `MessageEvent` interface, with the event type [`message`][Sse.defaultEventType], … The `data` attribute must be initialized to the value of the _data_ buffer, … and the `lastEventId` attribute must be initialized to the last event ID string of the event source.
     *
     * > …
     *
     * > If the _event type_ buffer has a value other than the empty string, change the type of the newly created event to equal the value of the _event type_ buffer.
     *
     * @see [Sse]
     */
    data class Event(
        val type: EventType,
        val data: EventData,
        val lastEventId: EventId
    )

    /**
     * ### From [The `EventSource` interface](https://www.w3.org/TR/2015/REC-eventsource-20150203/#the-eventsource-interface):
     *
     * > ∙ A **last event ID string**. This must initially be the empty string.
     */
    const val NoEventId: EventId = ""

    /**
     * ### From [Introduction](https://www.w3.org/TR/2015/REC-eventsource-20150203/#server-sent-events-intro):
     *
     * > The default event type is "message".
     */
    const val defaultEventType: EventType = "message"

    const val defaultRetryTime: Milliseconds = 3000

    /**
     * Adds the necessary headers for an [Sse] connection.
     *
     *
     * ### From [Processing model](https://www.w3.org/TR/2015/REC-eventsource-20150203/#processing-model):
     *
     * > For HTTP connections, the `Accept` header may be included; if included, it must contain only formats of event framing that are supported by the user agent (one of which must be `text/event-stream`, as [described below][isValidContentType]).
     *
     * > If the [event source's last event ID string][lastEventId] is not the [empty string][NoEventId], then a `Last-Event-ID` HTTP header must be included with the request, whose value is the value of the [event source's last event ID string][EventSourceState.lastEventId], encoded as UTF-8.
     *
     * > User agents should use the `Cache-Control: no-cache` header in requests to bypass any caches for requests of event sources. User agents should ignore HTTP cache headers in the response, never caching event sources.
     */
    @PublishedApi
    internal inline fun prepareRequest(
        lastEventId: String?,
        addHeader: AddHeader
    ) {
        addHeader("Accept-Encoding", "")
        addHeader("Accept", "text/event-stream")
        addHeader("Cache-Control", "no-cache")

        lastEventId
            ?.takeUnless { it == NoEventId }
            ?.let { addHeader("Last-Event-Id", it) }
    }

    /**
     * Returns whether the HTTP response code of an [Sse] connection is valid.
     *
     *
     * ### From [Processing model](https://www.w3.org/TR/2015/REC-eventsource-20150203/#processing-model):
     *
     * > HTTP 200 OK responses … must be processed line by line as [described below][parseSseLines].
     *
     * @see [isRecoverableResponseCode]
     * @see [isValidContentType]
     */
    @PublishedApi
    internal fun isValidResponseCode(
        httpResponseCode: Int?
    ) =
        when (httpResponseCode) {
            200
            -> true
            else
            -> false
        }

    /**
     * Returns whether the HTTP response code of an [Sse] connection is recoverable and can be retried.
     *
     *
     * ### From [Processing model](https://www.w3.org/TR/2015/REC-eventsource-20150203/#processing-model):
     *
     * > HTTP 305 Use Proxy, 401 Unauthorized, and 407 Proxy Authentication Required should be treated transparently as for any other subresource.
     *
     * > HTTP 500 Internal Server Error, 502 Bad Gateway, 503 Service Unavailable, and 504 Gateway Timeout responses, and any network error that prevents the connection from being established in the first place (e.g. DNS errors), must cause the user agent to asynchronously reestablish the connection.
     *
     * > …
     *
     * > Any other HTTP response code not listed here must cause the user agent to [fail the connection][EventSourceLifecycle.Closed.ResponseUnsuccessfulFatal].
     *
     * @see [isValidResponseCode]
     */
    @PublishedApi
    internal fun isRecoverableResponseCode(
        httpResponseCode: Int?
    ) =
        when (httpResponseCode) {
            500,
            502,
            503,
            504
            -> true
            else
            -> false
        }

    @PublishedApi
    internal fun isNoContentResponseCode(
        httpResponseCode: Int?
    ) =
        when (httpResponseCode) {
            204
            -> true
            else
            -> false
        }

    /**
     * Returns whether the content type in the response of an [Sse] connection is valid.
     *
     *
     * ### From [Processing model](https://www.w3.org/TR/2015/REC-eventsource-20150203/#processing-model):
     *
     * > … Content-Type header specifying the type `text/event-stream`, ignoring any MIME type parameters, must be processed line by line as [described below][parseSseLines].
     *
     * > …
     *
     * > … responses that have a Content-Type specifying an unsupported type, or that have no Content-Type at all, must cause the user agent to [fail the connection][EventSourceLifecycle.Closed.InvalidContentType].
     */
    @PublishedApi
    internal fun isValidContentType(
        mediaType: String?,
        mediaSubType: String?
    ) =
        mediaType == "text" && mediaSubType == "event-stream"
}
