package cz.applifting.humansis.api.interceptor

import android.content.SharedPreferences
import cz.applifting.humansis.BuildConfig
import cz.applifting.humansis.managers.LoginManager
import cz.applifting.humansis.managers.SP_COUNTRY
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response

class HeadersInterceptor(
    private val loginManager: LoginManager,
    private val sp: SharedPreferences
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val oldRequest: Request = chain.request()
        val headersBuilder = oldRequest.headers().newBuilder()
        runBlocking {
            loginManager.getAuthToken()?.let {
                headersBuilder.add("Authorization", "Bearer $it")
            }
            sp.getString(SP_COUNTRY, "SYR")
                ?.let { headersBuilder.add("Country", it) }
            headersBuilder.add("Version-Name", BuildConfig.VERSION_NAME)
            headersBuilder.add("Build-Number", BuildConfig.BUILD_NUMBER.toString())
            headersBuilder.add("Build-Type", BuildConfig.BUILD_TYPE)
        }
        val request = oldRequest.newBuilder().headers(headersBuilder.build()).build()
        return chain.proceed(request)
    }
}