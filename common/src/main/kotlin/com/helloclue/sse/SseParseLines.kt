package com.helloclue.sse

import com.helloclue.sse.Sse.Event
import kotlinx.coroutines.channels.ReceiveChannel

private typealias OnEvent = (Event) -> Unit

/**
 * Parses a [stream of lines][lines] according to the [Server-Sent Events][Sse] protocol.
 *
 *
 * ### From [Interpreting an event stream](https://www.w3.org/TR/2015/REC-eventsource-20150203/#event-stream-interpretation):
 *
 * > When the user agent is required to **dispatch the event**, then the user agent must act as follows:
 *
 * > (1) Set the last event ID string of the event source to value of the _last event ID_ buffer. The buffer does not get reset, so the last event ID string of the event source remains set to this value until the next time it is set by the server.
 *
 * > (2) If the _data_ buffer is an empty string, set the _data_ buffer and the _event type_ buffer to the empty string and abort these steps.
 *
 * > (3) If the _data_ buffer's last character is a U+000A LINE FEED (LF) character, then remove the last character from the _data_ buffer.
 *
 * > …
 *
 * > (5) If the _event type_ buffer has a value other than the empty string, change the type of the newly created event to equal the value of the _event type_ buffer.
 *
 * > (6) Set the data buffer and the event type buffer to the empty string.
 *
 * @see [parseSseLine]
 */
@PublishedApi
internal suspend inline fun parseSseLines(
    lines: ReceiveChannel<Line>,
    onEvent: OnEvent,
    onEventEnd: OnEventEnd,
    onEventType: OnEventType,
    onEventData: OnEventData,
    onEventId: OnEventId,
    onRetryTime: OnRetryTime,
    onComment: OnComment,
    onInvalidReconnectionTime: OnInvalidReconnectionTime,
    onInvalidField: OnInvalidField
) {
    val data = StringBuilder()
    var eventType = ""
    var lastEventId = ""

    for (line in lines) {
        parseSseLine(
            line = line,
            onEventEnd = {
                onEventEnd()
                if (data.isNotEmpty()) {
                    onEvent(Event(
                        data = data.removeSuffix("\n").toString(), //TODO use regex for [removeSuffix]
                        type = eventType.ifEmpty { Sse.defaultEventType },
                        lastEventId = lastEventId
                    ))
                }
                data.clear()
                eventType = ""
            },
            onEventId = {
                onEventId(it)
                lastEventId = it
            },
            onEventType = {
                onEventType(it)
                eventType = it
            },
            onEventData = {
                onEventData(it)
                data.append(it).append('\n')
            },
            onRetryTime = onRetryTime,
            onComment = onComment,
            onInvalidReconnectionTime = onInvalidReconnectionTime,
            onInvalidField = onInvalidField
        )
    }
}
