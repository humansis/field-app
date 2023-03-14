package cz.applifting.humansis.db.migrations

import androidx.room.RenameTable
import androidx.room.migration.AutoMigrationSpec
import androidx.sqlite.db.SupportSQLiteDatabase
import quanti.com.kotlinlog.Log

@RenameTable(
    fromTableName = "distributions",
    toTableName = "assistances"
)
class Migration31to32 : AutoMigrationSpec {
    // This migration also adds refresh token columns to UserDbEntity.
    override fun onPostMigrate(db: SupportSQLiteDatabase) {
        Log.d("HumansisDb MIGRATION", "DATABASE MIGRATION FROM 31 TO 32 FINISHED SUCCESSFULLY")
    }
}
