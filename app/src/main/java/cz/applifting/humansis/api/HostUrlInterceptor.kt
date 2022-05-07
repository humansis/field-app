package cz.applifting.humansis.api

import cz.applifting.humansis.misc.ApiEnvironments
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response

class HostUrlInterceptor : Interceptor {

    @Volatile
    private var host: ApiEnvironments? = null

    @Volatile
    private var shouldLogHeaders: Boolean = false

    fun setHost(host: ApiEnvironments?) {
        this.host = host
    }

    fun setShouldLogHeaders(shouldLogHeaders: Boolean) {
        this.shouldLogHeaders = shouldLogHeaders
    }

    fun getShouldLogHeaders(): Boolean {
        return shouldLogHeaders
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        var request: Request = chain.request()
        host?.let { host ->
            val newUrl = request.url().newBuilder()
                .scheme(if (host.secure) HTTPS_SCHEME else HTTP_SCHEME)
                .host(host.url)
                .port(host.port ?: HTTPS_PORT)
                .build()
            request = request.newBuilder()
                .url(newUrl)
                .build()
        }
        return chain.proceed(request)
    }

    companion object {
        const val HTTPS_PORT = 443
        const val HTTPS_SCHEME = "https"
        const val HTTP_SCHEME = "http"
    }
}