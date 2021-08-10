package cz.applifting.humansis.repositories

import android.content.Context
import cz.applifting.humansis.R
import cz.applifting.humansis.api.HumansisService
import cz.applifting.humansis.db.DbProvider
import cz.applifting.humansis.model.db.ProjectLocal
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Created by Petr Kubes <petr.kubes@applifting.cz> on 09, September, 2019
 */
@Singleton
class ProjectsRepository @Inject constructor(val service: HumansisService, val dbProvider: DbProvider, val context: Context) {


    suspend fun getProjectsOnline(currentCountry: String): List<ProjectLocal> {
        val result = service
            .getProjects(currentCountry)
            .map { ProjectLocal(
                it.id,
                it.name ?: context.getString(R.string.unknown),
                it.numberOfHouseholds ?: -1
            ) }

        dbProvider.get().projectsDao().replaceProjects(result)

        return result
    }

    fun getProjectsOffline(): Flow<List<ProjectLocal>> {
        return dbProvider.get().projectsDao().getAll()
    }

    suspend fun getProjectsOfflineSuspend(): List<ProjectLocal> {
        return dbProvider.get().projectsDao().getAllSuspend()
    }

    suspend fun getNameByDistributionId(distributionId: Int): String? {
        return dbProvider.get().projectsDao().getNameByDistributionId(distributionId)
    }

    suspend fun deleteAll() {
        dbProvider.get().projectsDao().deleteAll()
    }
}