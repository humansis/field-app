package cz.applifting.humansis.db.migrations

import androidx.room.DeleteColumn
import androidx.room.RenameColumn
import androidx.room.migration.AutoMigrationSpec
import androidx.sqlite.db.SupportSQLiteDatabase
import quanti.com.kotlinlog.Log

@RenameColumn(tableName = "beneficiaries", fromColumnName = "distributionId", toColumnName = "assistanceId")
@DeleteColumn(tableName = "beneficiaries", columnName = "vulnerabilities")
internal class Migration21to22 : AutoMigrationSpec {
    override fun onPostMigrate(db: SupportSQLiteDatabase) {
        Log.d("HumansisDb MIGRATION", "DATABASE MIGRATION FROM 21 TO 22 FINISHED SUCCESSFULLY")
    }
}