package cz.applifting.humansis.db.dao

import androidx.room.*
import cz.applifting.humansis.model.db.UserDbEntity

/**
 * Created by Petr Kubes <petr.kubes@applifting.cz> on 21, August, 2019
 */
@Dao
interface UserDao {
    @Query("SELECT * FROM user LIMIT 1")
    suspend fun getUser(): UserDbEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(user: UserDbEntity)

    @Update
    fun update(user: UserDbEntity)

    @Query("DELETE FROM user")
    suspend fun deleteAll()
}