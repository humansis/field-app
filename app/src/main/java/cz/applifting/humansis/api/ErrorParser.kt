package cz.applifting.humansis.api

import android.content.Context
import cz.applifting.humansis.R
import retrofit2.HttpException

fun parseError(e: HttpException, context: Context): String {

    val errorBody = e.response()?.errorBody()?.string() ?: ""

    return when {
        errorBody.contains("This username doesn't exist") -> context.getString(R.string.error_invalid_username)
        errorBody.contains("Wrong password") -> context.getString(R.string.error_invalid_password)
        errorBody.contains("No internet connection") -> context.getString(R.string.error_no_internet_connection)
        errorBody.contains("Service unavailable") -> context.getString(R.string.error_service_unavailable)
        else -> {
            val localizedError = context.getString(R.string.error_unknown)
            val message = e.response()?.errorBody()?.string()?.takeIf { it.isNotEmpty() } ?: e.message()
            "$localizedError: ${e.code()}\n${message.take(100)}"
        }
    }
}