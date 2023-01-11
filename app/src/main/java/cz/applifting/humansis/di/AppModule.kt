package cz.applifting.humansis.di

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.GsonBuilder
import cz.applifting.humansis.BuildConfig
import cz.applifting.humansis.api.HumansisService
import cz.applifting.humansis.api.RefreshTokenService
import cz.applifting.humansis.api.interceptor.ConnectionInterceptor
import cz.applifting.humansis.api.interceptor.HeadersInterceptor
import cz.applifting.humansis.api.interceptor.HostUrlInterceptor
import cz.applifting.humansis.api.interceptor.LoggingInterceptor
import cz.applifting.humansis.db.DbProvider
import cz.applifting.humansis.managers.LoginManager
import cz.applifting.humansis.misc.NfcTagPublisher
import cz.applifting.humansis.misc.connectionObserver.ConnectionObserver
import cz.applifting.humansis.misc.connectionObserver.ConnectionObserverImpl
import cz.quanti.android.nfc.OfflineFacade
import cz.quanti.android.nfc.PINFacade
import cz.quanti.android.nfc_io_libray.types.NfcUtil
import dagger.Module
import dagger.Provides
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.create
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
    fun provideConnectionInterceptor(
        context: Context
    ): ConnectionInterceptor {
        return ConnectionInterceptor(
            context
        )
    }

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): LoggingInterceptor {
        return LoggingInterceptor()
    }

    @Provides
    @Singleton
    fun refreshTokenProvider(
        @Named(BASE_URL) baseUrl: String,
        hostUrlInterceptor: HostUrlInterceptor,
        connectionInterceptor: ConnectionInterceptor,
        loggingInterceptor: LoggingInterceptor
    ): RefreshTokenService {
        val client: OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.MINUTES)
            .callTimeout(5, TimeUnit.MINUTES)
            .readTimeout(5, TimeUnit.MINUTES)
            .addInterceptor(hostUrlInterceptor)
            .addInterceptor(connectionInterceptor)
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

    @Provides
    @Singleton
    fun provideHeadersInterceptor(
        refreshTokenService: RefreshTokenService,
        loginManager: LoginManager,
        sp: SharedPreferences
    ): HeadersInterceptor {
        return HeadersInterceptor(
            refreshTokenService,
            loginManager,
            sp
        )
    }

    @Provides
    @Singleton
    fun retrofitProvider(
        @Named(BASE_URL) baseUrl: String,
        hostUrlInterceptor: HostUrlInterceptor,
        headersInterceptor: HeadersInterceptor,
        connectionInterceptor: ConnectionInterceptor,
        loggingInterceptor: LoggingInterceptor
    ): HumansisService {

        val client: OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.MINUTES)
            .callTimeout(5, TimeUnit.MINUTES)
            .readTimeout(5, TimeUnit.MINUTES)
            .addInterceptor(hostUrlInterceptor)
            .addInterceptor(headersInterceptor)
            .addInterceptor(connectionInterceptor)
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