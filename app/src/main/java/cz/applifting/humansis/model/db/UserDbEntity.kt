package cz.applifting.humansis.model.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Created by Petr Kubes <petr.kubes@applifting.cz> on 21, August, 2019
 */
@Entity(tableName = "User")
data class UserDbEntity(
    @PrimaryKey val id: Long,
    val username: String,
    val token: String?,
    val email: String,
    val invalidPassword: Boolean = false,
    val countries: List<String> = listOf()
)