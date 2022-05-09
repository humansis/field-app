package cz.applifting.humansis.api.interceptor

import android.content.Context
import cz.applifting.humansis.extensions.isNetworkConnected
import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import java.net.HttpURLConnection

class ConnectionInterceptor(
    private val context: Context
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        return if (context.isNetworkConnected()) {
            try {
                chain.proceed(request)
            } catch (e: Exception) {
                buildErrorResponse(
                    request,
                    HttpURLConnection.HTTP_UNAVAILABLE,
                    "Service unavailable"
                )
            }
        } else {
            buildErrorResponse(
                request,
                HttpURLConnection.HTTP_UNAVAILABLE,
                "No internet connection"
            )
        }
    }

    private fun buildErrorResponse(
        oldRequest: Request,
        errorCode: Int,
        errorMessage: String
    ): Response {
        return Response.Builder()
            .protocol(Protocol.HTTP_2)
            .request(oldRequest)
            .code(errorCode)
            .message(errorMessage)
            .body(ResponseBody.create(MediaType.parse("text/plain"), errorMessage))
            .build()
    }
}