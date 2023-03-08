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

    object Stage2 : ApiEnvironment(
        id = 3,
        title = STAGE2_API_TITLE,
        url = BuildConfig.STAGE2_API_URL
    )

    object Dev1 : ApiEnvironment(
        id = 4,
        title = DEV1_API_TITLE,
        url = BuildConfig.DEV1_API_URL
    )

    object Test : ApiEnvironment(
        id = 5,
        title = TEST_API_TITLE,
        url = BuildConfig.TEST_API_URL
    )

    object Test2 : ApiEnvironment(
        id = 6,
        title = TEST2_API_TITLE,
        url = BuildConfig.TEST2_API_URL
    )

    object Test3 : ApiEnvironment(
        id = 7,
        title = TEST3_API_TITLE,
        url = BuildConfig.TEST3_API_URL
    )

    object Local : ApiEnvironment(
        id = 8,
        title = LOCAL_API_TITLE,
        secure = false,
        url = BuildConfig.LOCAL_API_URL,
        port = 8091
    )

    class Custom(
        url: String
    ) : ApiEnvironment(
        id = 8,
        title = CUSTOM_API_TITLE,
        url = url
    )

    companion object {

        const val PROD_API_TITLE = "PROD"
        const val DEMO_API_TITLE = "DEMO"
        const val STAGE_API_TITLE = "STAGE"
        const val STAGE2_API_TITLE = "STAGE2"
        const val DEV1_API_TITLE = "DEV1"
        const val TEST_API_TITLE = "TEST"
        const val TEST2_API_TITLE = "TEST2"
        const val TEST3_API_TITLE = "TEST3"
        const val LOCAL_API_TITLE = "LOCAL"
        const val CUSTOM_API_TITLE = "CUSTOM"

        fun createEnvironments(context: Context): List<ApiEnvironment> {
            return mutableListOf(
                Prod,
                Demo,
                Stage,
                Stage2,
                Dev1,
                Test,
                Test2,
                Test3,
                Local
            ).apply {
                try {
                    add(Custom(readCustomUrl(context)))
                } catch (e: Exception) {
                    Log.e(e)
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

        fun find(title: String, hostUrl: String): ApiEnvironment? {
            return when (title) {
                PROD_API_TITLE -> Prod
                DEMO_API_TITLE -> Demo
                STAGE_API_TITLE -> Stage
                STAGE2_API_TITLE -> Stage2
                DEV1_API_TITLE -> Dev1
                TEST_API_TITLE -> Test
                TEST2_API_TITLE -> Test2
                TEST3_API_TITLE -> Test3
                LOCAL_API_TITLE -> Local
                CUSTOM_API_TITLE -> Custom(hostUrl)
                else -> null
            }
        }
    }
}
