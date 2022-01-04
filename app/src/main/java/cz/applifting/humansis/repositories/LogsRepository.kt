package cz.applifting.humansis.repositories

import android.content.Context
import cz.applifting.humansis.api.HumansisService
import cz.applifting.humansis.misc.ApiUtilities.isPositiveResponseHttpCode
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import quanti.com.kotlinlog.utils.getZipOfLogs
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Created by Petr Kubes <petr.kubes@applifting.cz> on 09, September, 2019
 */
@Singleton
class LogsRepository @Inject constructor(val service: HumansisService, val context: Context) {

    suspend fun postLogs(vendorId: Long) {
        val zipOfLogs = getZipOfLogs(context, 48)
        service.postLogs(
            vendorId,
            MultipartBody.Part.createFormData(
                "file",
                zipOfLogs.name,
                RequestBody.create(
                    MediaType.parse("multipart/form-data"),
                    zipOfLogs
                )
            )
        ).let { response ->
            if (!isPositiveResponseHttpCode(response.code())) {
                throw HttpException(response)
            }
        }
    }
}