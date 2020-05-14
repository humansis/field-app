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

    fun getHostUrl(): String {
        host?.let {
            return it.getUrl()
        }
        return ApiEnvironments.BASE.getUrl()
    }

    override fun intercept(chain: Interceptor.Chain): Response {
            var request: Request = chain.request()
            host?.let { host ->
                    val newUrl = request.url().newBuilder()
                            .host(host.getUrl())
                            .build()
                    request = request.newBuilder()
                            .url(newUrl)
                            .build()
            }
        return chain.proceed(request)
    }
}