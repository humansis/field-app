package cz.applifting.humansis.di

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.GsonBuilder
import cz.applifting.humansis.BuildConfig
import cz.applifting.humansis.api.interceptor.HostUrlInterceptor
import cz.applifting.humansis.api.HumansisService
import cz.applifting.humansis.api.interceptor.HeadersInterceptor
import cz.applifting.humansis.api.interceptor.LoggingInterceptor
import cz.applifting.humansis.db.DbProvider
import cz.applifting.humansis.extensions.isNetworkConnected
import cz.applifting.humansis.managers.LoginManager
import cz.applifting.humansis.misc.NfcTagPublisher
import cz.applifting.humansis.misc.connectionObserver.ConnectionObserver
import cz.applifting.humansis.misc.connectionObserver.ConnectionObserverImpl
import cz.quanti.android.nfc.OfflineFacade
import cz.quanti.android.nfc.PINFacade
import cz.quanti.android.nfc_io_libray.types.NfcUtil
import dagger.Module
import dagger.Provides
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
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
    fun provideHeadersInterceptor(
        loginManager: LoginManager,
        sp: SharedPreferences
    ): HeadersInterceptor {
        return HeadersInterceptor(
            loginManager,
            sp
        )
    }

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): LoggingInterceptor {
        return LoggingInterceptor()
    }

    @Provides
    @Singleton
    fun retrofitProvider(
        @Named(BASE_URL) baseUrl: String,
        context: Context,
        hostUrlInterceptor: HostUrlInterceptor,
        headersInterceptor: HeadersInterceptor,
        loggingInterceptor: LoggingInterceptor
    ): HumansisService {

        val client: OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.MINUTES)
            .callTimeout(5, TimeUnit.MINUTES)
            .readTimeout(5, TimeUnit.MINUTES)
            .addInterceptor(hostUrlInterceptor)
            .addInterceptor(headersInterceptor)
            .addInterceptor { chain ->

                val request = chain.request()

                if (context.isNetworkConnected()) {
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
            .addInterceptor(loggingInterceptor)
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(
                GsonConverterFactory.create(
                    GsonBuilder().serializeNulls().create()
                )
            )
            .client(client)
            .build().create()
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

    @Provides
    @Singleton
    fun nfcTagPublisherProvider(): NfcTagPublisher {
        return NfcTagPublisher()
    }

    @Provides
    @Singleton
    fun provideOfflineFacade(): OfflineFacade {
        return PINFacade(
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

    @Provides
    @Singleton
    fun provideConnectionObserver(context: Context): ConnectionObserver {
        return ConnectionObserverImpl(context)
    }
}