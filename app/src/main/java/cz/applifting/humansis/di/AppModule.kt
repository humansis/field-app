package cz.applifting.humansis.di

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.GsonBuilder
import cz.applifting.humansis.BuildConfig
import cz.applifting.humansis.api.HostUrlInterceptor
import cz.applifting.humansis.api.HumansisService
import cz.applifting.humansis.db.DbProvider
import cz.applifting.humansis.extensions.isNetworkConnected
import cz.applifting.humansis.managers.LoginManager
import cz.applifting.humansis.managers.SP_COUNTRY
import cz.applifting.humansis.misc.NfcTagPublisher
import cz.quanti.android.nfc.OfflineFacade
import cz.quanti.android.nfc.PINFacade
import cz.quanti.android.nfc_io_libray.types.NfcUtil
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.runBlocking
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.create
import java.net.HttpURLConnection
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton


/**
 * Created by Petr Kubes <petr.kubes@applifting.cz> on 14, August, 2019
 */

@Module
class AppModule {

    @Provides
    @Singleton
    fun dbProviderProvider(context: Context): DbProvider {
        return DbProvider(context)
    }

    @Provides
    @Singleton
    fun provideHostUrlInterceptor(): HostUrlInterceptor {
        return HostUrlInterceptor()
    }

    @Provides
    @Singleton
    fun retrofitProvider(@Named(BASE_URL) baseUrl: String, loginManager: LoginManager, context: Context, sp: SharedPreferences, hostUrlInterceptor: HostUrlInterceptor): HumansisService {
        val logging = HttpLoggingInterceptor().apply {
            @Suppress("ConstantConditionIf")
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.BASIC
            }
        }

        val client: OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.MINUTES)
            .callTimeout(5, TimeUnit.MINUTES)
            .readTimeout(5, TimeUnit.MINUTES)
            .addInterceptor(hostUrlInterceptor)
            .addInterceptor { chain ->

                val oldRequest = chain.request()

                if (context.isNetworkConnected()) {
                    try {
                        runBlocking {
                            val headersBuilder = oldRequest.headers().newBuilder()

                            headersBuilder.add("country", sp.getString(SP_COUNTRY, "SYR"))

                            loginManager.getAuthHeader()?.let {
                                headersBuilder.add("x-wsse", it)
                            }

                            val request = oldRequest.newBuilder().headers(headersBuilder.build()).build()
                            chain.proceed(request)
                        }
                    } catch (e: Exception) {
                        buildErrorResponse(oldRequest, HttpURLConnection.HTTP_UNAVAILABLE, "Service unavailable")
                    }

                } else {
                    buildErrorResponse(oldRequest, HttpURLConnection.HTTP_UNAVAILABLE, "No internet connection")
                }
            }
            .addInterceptor(logging)
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create(GsonBuilder().serializeNulls().create()))
            .client(client)
            .build().create()
    }

    private fun buildErrorResponse(oldRequest: Request, errorCode: Int, errorMessage: String): Response {
        return Response.Builder()
            .protocol(Protocol.HTTP_2)
            .request(oldRequest)
            .code(errorCode)
            .message(errorMessage)
            .body(ResponseBody.create(MediaType.parse("text/plain"), errorMessage))
            .build()
    }

    @Provides
    @Singleton
    fun nfcTagPublisherProvider(): NfcTagPublisher {
        return NfcTagPublisher()
    }

    @Provides
    @Singleton
    fun provideOfflineFacade(): OfflineFacade {
        return PINFacade(
            BuildConfig.APP_VESION,
            NfcUtil.hexStringToByteArray(BuildConfig.MASTER_KEY),
            NfcUtil.hexStringToByteArray(BuildConfig.APP_ID)
        )
    }

    @Provides
    @Singleton
    fun spProvider(context: Context): SharedPreferences {
        return spGenericProvider(context)
    }

    @Provides
    @Singleton
    @SPQualifier(type = SPQualifier.Type.GENERIC)
    fun spGenericProvider(context: Context): SharedPreferences {
        return context.getSharedPreferences(SPQualifier.Type.GENERIC.spName, Context.MODE_PRIVATE)
    }

    @Provides
    @Singleton
    @SPQualifier(type = SPQualifier.Type.CRYPTO)
    fun spCryptoProvider(context: Context): SharedPreferences {
        return context.getSharedPreferences(SPQualifier.Type.CRYPTO.spName, Context.MODE_PRIVATE)
    }
}