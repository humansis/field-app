package cz.applifting.humansis.api.interceptor

import android.content.SharedPreferences
import cz.applifting.humansis.BuildConfig
import cz.applifting.humansis.api.RefreshTokenService
import cz.applifting.humansis.managers.LoginManager
import cz.applifting.humansis.misc.SP_COUNTRY
import cz.applifting.humansis.misc.getPayload
import cz.applifting.humansis.model.JWToken
import cz.applifting.humansis.model.api.RefreshTokenRequest
import kotlinx.coroutines.runBlocking
import okhttp3.Headers
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import quanti.com.kotlinlog.Log

class HeadersInterceptor(
    private val refreshTokenService: RefreshTokenService,
    private val loginManager: LoginManager,
    private val sp: SharedPreferences
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val oldRequest: Request = chain.request()
        val headersBuilder = oldRequest.headers().newBuilder()
        runBlocking {
            headersBuilder.handleAuthorizationHeader(oldRequest)
            sp.getString(SP_COUNTRY, "SYR")?.let { headersBuilder.add("Country", it) }
            headersBuilder.add("Version-Name", BuildConfig.VERSION_NAME)
            headersBuilder.add("Build-Number", BuildConfig.BUILD_NUMBER.toString())
            headersBuilder.add("Build-Type", BuildConfig.BUILD_TYPE)
        }
        val request = oldRequest.newBuilder().headers(headersBuilder.build()).build()
        return chain.proceed(request)
    }

    private suspend fun Headers.Builder.handleAuthorizationHeader(oldRequest: Request) {
        if (!oldRequest.isLoginRequest()) {
            val authToken = loginManager.getAuthToken()
            if (!authToken.isExpiredOrNull()) {
                this.add("Authorization", "Bearer $authToken")
            } else {
                Log.d(
                    TAG,
                    "Auth token is expiring soon, acquiring new token for request ${oldRequest.method()} ${oldRequest.url()}"
                )
                loginManager.getRefreshToken()?.let { refreshToken ->
                    try {
                        val loginResponse =
                            refreshTokenService.refreshToken(RefreshTokenRequest(refreshToken))
                        loginManager.updateUser(loginResponse)
                        this.add("Authorization", "Bearer ${loginResponse.token}")
                    } catch (e: Exception) {
                        Log.e(
                            TAG,
                            e,
                            "Refresh token request ended with an exception. Not using Authorization token expecting a 401 response error code."
                        )
                    }
                } ?: run {
                    Log.e(
                        TAG,
                        "Refresh token not available. Not using Authorization token expecting a 401 response error code."
                    )
                }
            }
        }
    }

    private fun Request.isLoginRequest(): Boolean {
        return this.url().pathSegments().last() == LOGIN_PATH_SEGMENT
    }

    private fun String?.isExpiredOrNull(): Boolean {
        return this?.let { JWToken(getPayload(it)) }?.isExpired() ?: true
    }

    companion object {
        private val TAG = HeadersInterceptor::class.java.simpleName
        private const val LOGIN_PATH_SEGMENT = "login"
    }
}