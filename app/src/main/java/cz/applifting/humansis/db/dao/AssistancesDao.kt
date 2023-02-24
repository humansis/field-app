package cz.applifting.humansis.db.dao

import androidx.room.*
import cz.applifting.humansis.model.db.AssistanceLocal
import kotlinx.coroutines.flow.Flow

/**
 * Created by Petr Kubes <petr.kubes@applifting.cz> on 09, September, 2019
 */
@Dao
interface AssistancesDao {
    @Query("SELECT * FROM assistances WHERE projectId = :projectId")
    fun getByProject(projectId: Int): Flow<List<AssistanceLocal>>

    @Query("SELECT * FROM assistances WHERE projectId = :projectId")
    suspend fun getByProjectSuspend(projectId: Int): List<AssistanceLocal>

    @Query("SELECT * FROM assistances WHERE id = :assistanceId")
    suspend fun getById(assistanceId: Int): AssistanceLocal?

    @Query("DELETE FROM assistances WHERE projectId = :projectId")
    suspend fun deleteByProject(projectId: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(distributions: List<AssistanceLocal>)

    @Query("DELETE FROM assistances")
    suspend fun deleteAll()

    @Query("SELECT * FROM assistances WHERE projectId = :projectId AND completed = 0")
    suspend fun findUncompletedDistributionsSuspend(projectId: Int): List<AssistanceLocal>

    @Query("SELECT * FROM assistances")
    fun getAll(): Flow<List<AssistanceLocal>>

    @Transaction
    suspend fun replaceByProject(projectId: Int, distributions: List<AssistanceLocal>) {
        deleteByProject(projectId)
        insertAll(distributions)
    }
}