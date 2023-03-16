package cz.applifting.humansis.db.migrations

import androidx.room.migration.AutoMigrationSpec
import androidx.sqlite.db.SupportSQLiteDatabase
import quanti.com.kotlinlog.Log

class Migration32to33 : AutoMigrationSpec {
    // This migration only changes dateOfDistribution in AssistanceLocal from nullable to non-nullable
    override fun onPostMigrate(db: SupportSQLiteDatabase) {
        Log.d("HumansisDb MIGRATION", "DATABASE MIGRATION FROM 32 TO 33 FINISHED SUCCESSFULLY")
    }
}
