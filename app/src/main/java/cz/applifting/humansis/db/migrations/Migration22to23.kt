package cz.applifting.humansis.db.migrations

import androidx.room.DeleteColumn
import androidx.room.migration.AutoMigrationSpec
import androidx.sqlite.db.SupportSQLiteDatabase
import quanti.com.kotlinlog.Log

@DeleteColumn(
    tableName = "User",
    columnName = "salted_password"
)
class Migration22to23 : AutoMigrationSpec {
    override fun onPostMigrate(db: SupportSQLiteDatabase) {
        Log.d("HumansisDb MIGRATION", "DATABASE MIGRATION FROM 22 TO 23 FINISHED SUCCESSFULLY")
    }
}