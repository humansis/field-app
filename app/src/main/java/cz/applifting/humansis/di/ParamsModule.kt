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
        //todo poresit v2
        return "https://" + BuildConfig.RELEASE_API_URL + "/api/wsse/offline-app/"
    }
}