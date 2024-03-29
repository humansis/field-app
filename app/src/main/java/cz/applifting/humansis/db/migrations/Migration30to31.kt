package cz.applifting.humansis.db.migrations

import androidx.room.RenameColumn
import androidx.room.migration.AutoMigrationSpec
import androidx.sqlite.db.SupportSQLiteDatabase
import quanti.com.kotlinlog.Log

@RenameColumn(
    tableName = "User",
    fromColumnName = "invalidPassword",
    toColumnName = "shouldReauthenticate"
)
class Migration30to31 : AutoMigrationSpec {
    // This migration also adds refresh token columns to UserDbEntity.
    override fun onPostMigrate(db: SupportSQLiteDatabase) {
        Log.d("HumansisDb MIGRATION", "DATABASE MIGRATION FROM 30 TO 31 FINISHED SUCCESSFULLY")
    }
}
