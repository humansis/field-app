package cz.applifting.humansis.db.migrations

import androidx.room.DeleteColumn
import androidx.room.RenameColumn
import androidx.room.migration.AutoMigrationSpec
import androidx.sqlite.db.SupportSQLiteDatabase
import quanti.com.kotlinlog.Log

@DeleteColumn(
    tableName = "beneficiaries",
    columnName = "commodities"
)
@RenameColumn(
    tableName = "beneficiaries",
    fromColumnName = "commoditiesTmp",
    toColumnName = "commodities"
)
class Migration28to29 : AutoMigrationSpec {
    override fun onPostMigrate(db: SupportSQLiteDatabase) {
        Log.d("HumansisDb MIGRATION", "DATABASE MIGRATION FROM 28 TO 29 FINISHED SUCCESSFULLY")
    }
}