package com.helloclue.sse

import okhttp3.Response

object TestUtils {

    fun computeRetryTime(): (EventSourceConnectionRetry.Reason<Response, Throwable>, Int) -> Long {
        return { _, _ -> 300 }
    }
}
