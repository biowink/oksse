package com.helloclue.sse

typealias Line = String
typealias Comment = String

typealias OnEventEnd = () -> Unit
typealias OnEventType = (type: EventType) -> Unit
typealias OnEventData = (data: EventData) -> Unit
typealias OnEventId = (id: EventId) -> Unit
typealias OnRetryTime = (retryTime: Milliseconds) -> Unit
typealias OnComment = (comment: Comment) -> Unit
typealias OnInvalidReconnectionTime = (value: String) -> Unit
typealias OnInvalidField = (field: String) -> Unit

@PublishedApi
internal val digitsOnly = Regex("^[0-9]+$")

/**
 * Parses a single [line] according to the [Server-Sent Events][Sse] protocol.
 *
 *
 * ### From [Interpreting an event stream](https://www.w3.org/TR/2015/REC-eventsource-20150203/#event-stream-interpretation):
 *
 * > Lines must be processed, in the order they are received, as follows:
 *
 * > **↪ If the line is empty (a blank line)**
 *
 * >   >   [Dispatch the event][OnEventEnd], as defined below.
 *
 * > **↪ If the line starts with a U+003A COLON character (:)**
 *
 * >   >   [Ignore the line][OnComment].
 *
 * > **↪ If the line contains a U+003A COLON character (:)**
 *
 * >   >   Collect the characters on the line before the first U+003A COLON character (:), and let _field_ be that string.
 *
 * >   >   Collect the characters on the line after the first U+003A COLON character (:), and let _value_ be that string. If _value_ starts with a U+0020 SPACE character, remove it from _value_.
 *
 * >   >   Process the field using the steps described below, using field as the _field_ name and value as the field _value_.
 *
 * > **↪ Otherwise, the string is not empty but does not contain a U+003A COLON character (:)**
 *
 * >   >   Process the field using the steps described below, using the whole line as the field name, and the empty string as the field value.
 *
 * > Once the end of the file is reached, any pending data must be discarded. (If the file ends in the middle of an event, before the final empty line, the incomplete event is not dispatched.)
 *
 * > The steps to **process the field** given a field name and a field value depend on the field name, as given in the following list. Field names must be compared literally, with no case folding performed.
 *
 * > **↪ [If the field name is "event"][onEventType]**
 *
 * >   >   Set the event type buffer to field value.
 *
 * > **↪ [If the field name is "data"][onEventData]**
 *
 * >   >   Append the field value to the data buffer, then append a single U+000A LINE FEED (LF) character to the data buffer.
 *
 * > **↪ [If the field name is "id"][onEventId]**
 *
 * >   >   Set the last event ID buffer to the field value.
 *
 * > **↪ [If the field name is "retry"][onRetryTime]**
 *
 * >   >   If the field value consists of [only ASCII digits][digitsOnly], then interpret the field value as an integer in base ten, and set the event stream's reconnection time to that integer. Otherwise, [ignore the field][onInvalidReconnectionTime].
 *
 * > **↪ [Otherwise][onInvalidField]**
 *
 * >   >   The field is ignored.
 */
@PublishedApi
internal inline fun parseSseLine(
    line: Line,
    onEventEnd: OnEventEnd,
    onEventType: OnEventType,
    onEventData: OnEventData,
    onEventId: OnEventId,
    onRetryTime: OnRetryTime,
    onComment: OnComment,
    onInvalidReconnectionTime: OnInvalidReconnectionTime,
    onInvalidField: OnInvalidField
) {
    val parts = line.takeIf { it.isNotEmpty() }?.split(':', limit = 2) //TODO use regex for [split]
    val field = parts?.getOrNull(0)
    val value = parts?.getOrNull(1)?.removePrefix(" ").orEmpty() //TODO use regex for [removePrefix]
    return when (field) {
        null -> onEventEnd()
        "" -> onComment(value)
        "event" -> onEventType(value)
        "data" -> onEventData(value)
        "id" -> onEventId(value)
        "retry" -> value.toLongOrNull()
            .takeIf { value.matches(digitsOnly) }
            ?.let(onRetryTime)
            ?: onInvalidReconnectionTime(value)
        else -> onInvalidField(field)
    }
}
