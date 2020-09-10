package cz.applifting.humansis.model.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Created by Petr Kubes <petr.kubes@applifting.cz> on 21, August, 2019
 */
@Entity
data class User(
    @PrimaryKey val id: Long,
    val username: String,
    val email: String,
    @ColumnInfo(name = "salted_password") val saltedPassword: String,
    val invalidPassword: Boolean = false,
    val countries: List<String> = listOf()
)