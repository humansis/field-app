package cz.applifting.humansis.misc

import android.content.Context
import cz.applifting.humansis.BuildConfig
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import quanti.com.kotlinlog.Log

sealed class ApiEnvironment(
    val id: Int,
    val title: String,
    val secure: Boolean = true,
    val url: String,
    val port: Int? = null
) {
    object Prod : ApiEnvironment(
        id = 0,
        title = PROD_API_TITLE,
        url = BuildConfig.PROD_API_URL
    )

    object Demo : ApiEnvironment(
        id = 1,
        title = DEMO_API_TITLE,
        url = BuildConfig.DEMO_API_URL
    )

    object Stage : ApiEnvironment(
        id = 2,
        title = STAGE_API_TITLE,
        url = BuildConfig.STAGE_API_URL
    )

    object Dev1 : ApiEnvironment(
        id = 3,
        title = DEV1_API_TITLE,
        url = BuildConfig.DEV1_API_URL
    )

    object Dev2 : ApiEnvironment(
        id = 4,
        title = DEV2_API_TITLE,
        url = BuildConfig.DEV2_API_URL
    )

    object Dev3 : ApiEnvironment(
        id = 5,
        title = DEV3_API_TITLE,
        url = BuildConfig.DEV3_API_URL
    )

    object Test : ApiEnvironment(
        id = 6,
        title = TEST_API_TITLE,
        url = BuildConfig.TEST_API_URL
    )

    object Local : ApiEnvironment(
        id = 7,
        title = LOCAL_API_TITLE,
        secure = false,
        url = BuildConfig.LOCAL_API_URL,
        port = 8091
    )

    class Custom(
        val context: Context
    ) : ApiEnvironment(
        id = 8,
        title = CUSTOM_API_TITLE,
        url = readCustomUrl(context)
    )

    object T4G : ApiEnvironment(
        id = 9,
        title = "T4G",
        url = "apiarm.humansis.org"
    )

    companion object {

        const val PROD_API_TITLE = "PROD"
        const val DEMO_API_TITLE = "DEMO"
        const val STAGE_API_TITLE = "STAGE"
        const val DEV1_API_TITLE = "DEV1"
        const val DEV2_API_TITLE = "DEV2"
        const val DEV3_API_TITLE = "DEV3"
        const val TEST_API_TITLE = "TEST"
        const val LOCAL_API_TITLE = "LOCAL"
        const val CUSTOM_API_TITLE = "CUSTOM"

        fun createEnvironments(context: Context): List<ApiEnvironment> {
            return mutableListOf(
                Prod,
                Demo,
                Stage,
                Dev1,
                Dev2,
                Dev3,
                Test,
                Local,
                T4G
            ).apply {
                try {
                    add(Custom(context))
                } catch (e: Exception) {
                    Log.d(e)
                }
            }
        }

        private fun readCustomUrl(context: Context): String {
            val fis = File(
                context.getExternalFilesDir(null)?.absolutePath,
                "apiconfig.txt"
            ).inputStream()
            val bufferedReader = BufferedReader(InputStreamReader(fis, "UTF-8"))
            val line = bufferedReader.readLine()

            return if (line.isNullOrBlank()) {
                throw Exception("Custom Api host could not be read.")
            } else {
                line
            }
        }

        fun find(title: String): ApiEnvironment? {
            return when (title) {
                PROD_API_TITLE -> Prod
                DEMO_API_TITLE -> Demo
                STAGE_API_TITLE -> Stage
                DEV1_API_TITLE -> Dev1
                DEV2_API_TITLE -> Dev2
                DEV3_API_TITLE -> Dev3
                TEST_API_TITLE -> Test
                LOCAL_API_TITLE -> Local
                else -> null
            }
        }
    }
}
