package cz.applifting.humansis.db.migrations

import androidx.room.DeleteColumn
import androidx.room.migration.AutoMigrationSpec
import androidx.sqlite.db.SupportSQLiteDatabase
import quanti.com.kotlinlog.Log

@DeleteColumn(
    tableName = "beneficiaries",
    columnName = "nationalId"
)
class Migration26to27 : AutoMigrationSpec {
    override fun onPostMigrate(db: SupportSQLiteDatabase) {
        Log.d("HumansisDb MIGRATION", "DATABASE MIGRATION FROM 26 TO 27 FINISHED SUCCESSFULLY")
    }
}