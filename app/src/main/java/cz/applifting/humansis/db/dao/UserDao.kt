package cz.applifting.humansis.db.dao

import androidx.room.*
import cz.applifting.humansis.model.db.User

/**
 * Created by Petr Kubes <petr.kubes@applifting.cz> on 21, August, 2019
 */
@Dao
interface UserDao {
    @Query("SELECT * FROM user LIMIT 1")
    suspend fun getUser(): User?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(user: User)

    @Update
    fun update(user: User)

    @Query("DELETE FROM user")
    suspend fun deleteAll()
}