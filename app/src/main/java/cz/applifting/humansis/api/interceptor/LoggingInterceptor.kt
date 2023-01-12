package cz.applifting.humansis.api.interceptor

import cz.applifting.humansis.BuildConfig
import cz.applifting.humansis.misc.isPositiveResponseHttpCode
import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okhttp3.ResponseBody
import okio.Buffer
import okio.GzipSource
import quanti.com.kotlinlog.Log
import java.nio.charset.Charset
import java.util.regex.Pattern

class LoggingInterceptor : Interceptor {

    @Volatile
    private var shouldLogHeaders: Boolean = false

    private val forbiddenRegex = Regex("(multipart/form-data|^<html>)")
    private val passwordPattern = Pattern.compile("password\":\"[^\"]*\"", Pattern.CASE_INSENSITIVE)
    private val tokenPattern = Pattern.compile("token\":\"[^\"]*\"", Pattern.CASE_INSENSITIVE)

    fun setShouldLogHeaders(shouldLogHeaders: Boolean) {
        this.shouldLogHeaders = shouldLogHeaders
    }

    private fun getShouldLogHeaders(): Boolean {
        return shouldLogHeaders
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request: Request = chain.request()

        logRequest(request.method(), request.url())

        if (BuildConfig.DEBUG) {
            if (getShouldLogHeaders()) {
                logRequestHeaders(request.headers())
            }

            request.body()?.let {
                logRequestBody(request.method(), it)
            }
        }
        return chain.proceed(request).apply {

            logResponse(this.code(), this.message(), request.url())

            if (BuildConfig.DEBUG && getShouldLogHeaders()) {
                logResponseHeaders(this.headers())
                setShouldLogHeaders(false)
            }

            if (BuildConfig.DEBUG || !isPositiveResponseHttpCode(this.code())) {
                this.body()?.let {
                    logResponseBody(this.headers(), it)
                }
            }
        }
    }

    private fun logRequest(method: String, url: HttpUrl) {
        Log.d(TAG, "--> $method $url")
    }

    private fun logResponse(code: Int, message: String, url: HttpUrl) {
        Log.d(TAG, "<-- $code $message $url")
    }

    private fun logRequestHeaders(headers: Headers) {
        Log.d(TAG, "--> HEADERS\n$headers")
    }

    private fun logResponseHeaders(headers: Headers) {
        Log.d(TAG, "<-- HEADERS\n$headers")
    }

    private fun logRequestBody(requestMethod: String, requestBody: RequestBody) {
        val buffer = Buffer()
        requestBody.writeTo(buffer)
        var charset = Charset.forName("UTF-8")
        val contentType = requestBody.contentType()
        if (contentType != null) {
            charset = contentType.charset(charset)
        }
        val body = buffer.readString(charset)
        if (!body.contains(forbiddenRegex)) {
            Log.d(TAG, "--> BODY ${transformBody(body)}")
            Log.d(
                TAG,
                "--> END " + requestMethod + " (" + requestBody.contentLength() + "-byte body)"
            )
        }
    }

    private fun logResponseBody(headers: Headers, responseBody: ResponseBody) {
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
                charset = contentType.charset(charset)
            }
            val body = buffer.clone().readString(charset)
            if (!body.contains(forbiddenRegex)) {
                Log.d(TAG, "<-- BODY ${transformBody(body)}")
            }
        }
        if (gzippedLength != null) {
            Log.d(
                TAG,
                "<-- END HTTP (" + buffer.size() + "-byte, " + gzippedLength + "-gzipped-byte body)"
            )
        } else {
            Log.d(TAG, "<-- END HTTP (" + buffer.size() + "-byte body)")
        }
    }

    private fun transformBody(body: String): String {
        val bodyWithHiddenPasswords = passwordPattern.matcher(body).replaceAll("password\":\"******\"")
        return tokenPattern.matcher(bodyWithHiddenPasswords).replaceAll("token\":\"******\"")
    }

    companion object {
        private val TAG = LoggingInterceptor::class.java.simpleName
    }
}