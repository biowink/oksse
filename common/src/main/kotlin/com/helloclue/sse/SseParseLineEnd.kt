package com.helloclue.sse

internal typealias Index = Long

/**
 * Finds the next line terminator in some character stream/sequence, which can be either:
 *
 * - [`\r\n`][onCrLf]: _Carriage Return_ character followed by _Line Feed_ character.
 * - [`\n`][onLf]: _Line Feed_ character.
 * - [`\r`][onCr]: _Carriage Return_ character.
 * - [EOF][onEof]: _End Of File_ reached before finding any terminator.
 *
 *
 * ### From [Parsing an event stream](https://www.w3.org/TR/2015/REC-eventsource-20150203/#parsing-an-event-stream):
 *
 * > This event stream format's MIME type is `text/event-stream`.
 *
 * > The event stream format is as described by the `stream` production of the following [ABNF](https://www.w3.org/TR/2015/REC-eventsource-20150203/#refsABNF), the character set for which is Unicode.
 *
 * >     > stream        = [ bom ] *event
 *   >     event         = *( comment / field ) end-of-line
 *   >     comment       = colon *any-char end-of-line
 *   >     field         = 1*name-char [ colon [ space ] *any-char ] end-of-line
 *   >     end-of-line   = ( cr lf / cr / lf )
 *   >
 *   >     ; characters
 *   >     lf            = %x000A ; U+000A LINE FEED (LF)
 *   >     cr            = %x000D ; U+000D CARRIAGE RETURN (CR)
 *   >     space         = %x0020 ; U+0020 SPACE
 *   >     colon         = %x003A ; U+003A COLON (:)
 *   >     bom           = %xFEFF ; U+FEFF BYTE ORDER MARK
 *   >     name-char     = %x0000-0009 / %x000B-000C / %x000E-0039 / %x003B-10FFFF
 *   >                     ; a Unicode character other than U+000A LINE FEED (LF), U+000D CARRIAGE RETURN (CR), or U+003A COLON (:)
 *   >     any-char      = %x0000-0009 / %x000B-000C / %x000E-10FFFF
 *   >                     ; a Unicode character other than U+000A LINE FEED (LF) or U+000D CARRIAGE RETURN (CR)
 *
 * > Event streams in this format must always be encoded as UTF-8. ([RFC3629](https://www.w3.org/TR/2015/REC-eventsource-20150203/#refsRFC3629))
 *
 * > Lines must be separated by either a [U+000D CARRIAGE RETURN U+000A LINE FEED (CRLF) character pair][onCrLf], a [single U+000A LINE FEED (LF) character][onLf], or a [single U+000D CARRIAGE RETURN (CR) character][onCr].
 *
 * @see [parseSseLine]
 */
@PublishedApi
internal inline fun <T> parseSseLineEnd(
    indexOfCr: () -> Index,
    indexOfLf: () -> Index,
    onCrLf: (Index) -> T,
    onLf: (Index) -> T,
    onCr: (Index) -> T,
    onEof: () -> T
): T {
    val lfIndex = indexOfLf()
    if (lfIndex == 0L) return onLf(lfIndex)

    val crIndex = indexOfCr()

    val hasCr = crIndex >= 0
    val hasLf = lfIndex >= 0

    return when {
        hasCr && hasLf -> when {
            crIndex + 1 == lfIndex -> onCrLf(crIndex)
            lfIndex < crIndex -> onLf(lfIndex)
            else -> onCr(crIndex)
        }
        hasLf -> onLf(lfIndex)
        hasCr -> onCr(crIndex)
        else -> onEof()
    }
}
