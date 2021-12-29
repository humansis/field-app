package cz.applifting.humansis.misc

import okhttp3.Headers
import okhttp3.ResponseBody
import okio.Buffer
import okio.GzipSource
import quanti.com.kotlinlog.Log
import java.nio.charset.Charset

object ApiUtilities {

    fun isPositiveResponseHttpCode(code: Int): Boolean {
        // The positive http code is in format of 2xx
        return (code - 200 >= 0) && (code - 300 < 0)
    }

    fun logResponseBody(headers: Headers, responseBody: ResponseBody) {
        val source = responseBody.source()
        source.request(Long.MAX_VALUE) // Buffer the entire body.
        var buffer = source.buffer()
        var gzippedLength: Long? = null

        if ("gzip".equals(headers.get("Content-Encoding"), ignoreCase = true)) {
            gzippedLength = buffer.size()
            var gzippedResponseBody: GzipSource? = null
            try {
                gzippedResponseBody = GzipSource(buffer.clone())
                buffer = Buffer()
                buffer.writeAll(gzippedResponseBody)
            } finally {
                gzippedResponseBody?.close()
            }
        }

        if (responseBody.contentLength() != 0L) {
            var charset = Charset.forName("UTF-8")
            val contentType = responseBody.contentType()
            if (contentType != null) {
                charset = contentType.charset(Charset.forName("UTF-8"))
            }
            Log.d("OkHttp", "")
            Log.d("OkHttp", buffer.clone().readString(charset))
        }
        if (gzippedLength != null) {
            Log.d(
                "OkHttp",
                "<-- END HTTP (" + buffer.size() + "-byte, " +
                        gzippedLength + "-gzipped-byte body)"
            )
        }
    }

}