package cz.applifting.humansis.api

import cz.applifting.humansis.misc.ApiEnvironments
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response

class HostUrlInterceptor : Interceptor {

    @Volatile
    private var host: ApiEnvironments? = null

    fun setHost(host: ApiEnvironments?) {
        this.host = host
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        var request: Request = chain.request()
        host?.let { host ->
            val newUrl = request.url().newBuilder()
                .host(host.url)
                .port(host.port ?: 443)
                .build()
            request = request.newBuilder()
                .url(newUrl)
                .build()
        }
        return chain.proceed(request)
    }
}