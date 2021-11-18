package cz.applifting.humansis.db.dao

import androidx.room.*
import cz.applifting.humansis.model.db.ProjectLocal
import kotlinx.coroutines.flow.Flow

/**
 * Created by Petr Kubes <petr.kubes@applifting.cz> on 09, September, 2019
 */
@Dao
interface ProjectsDao {
    @Query("SELECT * FROM projects")
    fun getAll(): Flow<List<ProjectLocal>>

    @Query("SELECT * FROM projects")
    suspend fun getAllSuspend(): List<ProjectLocal>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(projects: List<ProjectLocal>)

    @Query("DELETE FROM projects")
    suspend fun deleteAll()

    @Query("SELECT projects.name FROM projects INNER JOIN distributions ON projects.id = distributions.projectId WHERE distributions.id = :assistanceId LIMIT 1")
    suspend fun getNameByAssistanceId(assistanceId: Int): String?

    @Transaction
    suspend fun replaceProjects(projects: List<ProjectLocal>) {
        deleteAll()
        insertAll(projects)
    }
}