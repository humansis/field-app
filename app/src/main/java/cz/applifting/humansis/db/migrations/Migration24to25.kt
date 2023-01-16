package cz.applifting.humansis.db.migrations

import androidx.room.DeleteColumn
import androidx.room.migration.AutoMigrationSpec
import androidx.sqlite.db.SupportSQLiteDatabase
import quanti.com.kotlinlog.Log

@DeleteColumn(
    tableName = "distributions",
    columnName = "commodities"
)
class Migration24to25 : AutoMigrationSpec {
    override fun onPostMigrate(db: SupportSQLiteDatabase) {
        Log.d("HumansisDb MIGRATION", "DATABASE MIGRATION FROM 24 TO 25 FINISHED SUCCESSFULLY")
    }
}