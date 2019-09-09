package cz.applifting.humansis.db.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import cz.applifting.humansis.model.db.ProjectLocal

/**
 * Created by Petr Kubes <petr.kubes@applifting.cz> on 09, September, 2019
 */
@Dao
interface ProjectsDao {
    @Query("SELECT * FROM projects")
    suspend fun getAll(): List<ProjectLocal>?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(projects: List<ProjectLocal>)

    @Query("DELETE FROM projects")
    suspend fun deleteAll()
}