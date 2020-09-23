package cz.applifting.humansis.di

import cz.applifting.humansis.misc.ApiEnvironments
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
        return "https://" + ApiEnvironments.RELEASE.url + "/api/wsse/offline-app/v1/"
    }

    @Provides
    @Named(LOGFILE_PATH)
    fun logFilePath(): String {
        return "log.txt"
    }

}