package cz.applifting.humansis.di

import cz.applifting.humansis.BuildConfig
import dagger.Module
import dagger.Provides
import javax.inject.Named

/**
 * Created by Petr Kubes <petr.kubes@applifting.cz> on 13, November, 2019
 */
@Module
class ParamsModule {

    @Provides
    @Named(BASE_URL)
    fun baseUrl(): String {
        return "https://" + BuildConfig.PROD_API_URL + "/api/jwt/offline-app/"
    }
}