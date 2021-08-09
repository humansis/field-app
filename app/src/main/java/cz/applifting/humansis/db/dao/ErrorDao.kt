package cz.applifting.humansis.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import cz.applifting.humansis.model.db.SyncError
import kotlinx.coroutines.flow.Flow

/**
 * Created by Petr Kubes <petr.kubes@applifting.cz> on 27, November, 2019
 */
@Dao
interface ErrorDao {
    @Query("SELECT * FROM errors")
    fun getAll(): Flow<List<SyncError>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(syncErrors: List<SyncError>)

    @Query("DELETE FROM errors")
    suspend fun deleteAll()
}