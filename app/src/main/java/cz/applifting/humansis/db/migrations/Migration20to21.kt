package cz.applifting.humansis.db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import quanti.com.kotlinlog.Log

val Migration20to21 = object : Migration(20, 21) {
    override fun migrate(database: SupportSQLiteDatabase) {
        Log.d("HumansisDb MIGRATION", "RUNNING DATABASE MIGRATION FROM 20 TO 21")

        database.execSQL("ALTER TABLE distributions ADD remote INTEGER NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE distributions ADD dateOfExpiration TEXT")
        database.execSQL("ALTER TABLE distributions ADD foodLimit REAL")
        database.execSQL("ALTER TABLE distributions ADD nonfoodLimit REAL")
        database.execSQL("ALTER TABLE distributions ADD cashbackLimit REAL")
        database.execSQL("ALTER TABLE beneficiaries ADD remote INTEGER NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE beneficiaries ADD dateExpiration TEXT")
        database.execSQL("ALTER TABLE beneficiaries ADD foodLimit REAL")
        database.execSQL("ALTER TABLE beneficiaries ADD nonfoodLimit REAL")
        database.execSQL("ALTER TABLE beneficiaries ADD cashbackLimit REAL")
        database.execSQL("ALTER TABLE beneficiaries ADD originalBalance REAL")
        database.execSQL("ALTER TABLE beneficiaries ADD balance REAL")

        Log.d("HumansisDb MIGRATION", "DATABASE MIGRATION FROM 20 TO 21 FINISHED SUCCESSFULLY")
    }
}