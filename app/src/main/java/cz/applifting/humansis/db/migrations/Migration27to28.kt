package cz.applifting.humansis.db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import quanti.com.kotlinlog.Log

val Migration27to28 = object : Migration(27, 28) {
    override fun migrate(database: SupportSQLiteDatabase) {
        Log.d("HumansisDb MIGRATION", "RUNNING DATABASE MIGRATION FROM 27 TO 28")

        database.execSQL("ALTER TABLE beneficiaries ADD commoditiesTmp TEXT NOT NULL DEFAULT \"\"")
        database.execSQL("UPDATE beneficiaries SET commoditiesTmp = commodities WHERE commodities IS NOT NULL")

        Log.d("HumansisDb MIGRATION", "DATABASE MIGRATION FROM 27 TO 28 FINISHED SUCCESSFULLY")
    }
}