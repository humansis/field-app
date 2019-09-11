package cz.applifting.humansis.repositories

import android.content.Context
import cz.applifting.humansis.R
import cz.applifting.humansis.api.HumansisService
import cz.applifting.humansis.db.DbProvider
import cz.applifting.humansis.db.HumansisDB
import cz.applifting.humansis.model.db.ProjectLocal
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Created by Petr Kubes <petr.kubes@applifting.cz> on 09, September, 2019
 */
@Singleton
class ProjectsRepository @Inject constructor(val service: HumansisService, val dbProvider: DbProvider, val context: Context) {

    val db: HumansisDB by lazy { dbProvider.get() }

    suspend fun getProjectsOnline(): List<ProjectLocal>? {
        return try {
            val result = service
                .getProjects()
                .map { ProjectLocal(it.id, it.name ?: context.getString(R.string.unknown), it.numberOfHouseholds ?: -1) }
            
            db.projectsDao().deleteAll()
            db.projectsDao().insertAll(result)

            result
        } catch (e: HttpException) {
            null
        }
    }

    suspend fun getProjectsOffline(): List<ProjectLocal> {
        return db.projectsDao().getAll() ?: listOf()
    }
}