package com.helloclue.sse

interface Logger {
    fun onStart()
    fun onLine(line: Line)
    fun onLineEnding(lineEnding: LineEnding)
    fun onLinePartial(linePartial: LinePartial)
    fun onEndOfFile()
    fun onEventEnd()
    fun onEventType(type: EventType)
    fun onEventData(data: EventData)
    fun onEventId(id: EventId)
    fun onComment(comment: Comment)
    fun onInvalidReconnectionTime(value: String)
    fun onInvalidField(field: String)
    fun onAwaitingConnection()
    fun onRetryDelay(milliseconds: Milliseconds)
    fun onState(state: EventSourceState<*, *>)
}
