package cz.applifting.humansis.db.migrations

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import quanti.com.kotlinlog.Log

val Migration29to30 = object : Migration(29, 30) {
    override fun migrate(database: SupportSQLiteDatabase) {
        Log.d("HumansisDb MIGRATION", "RUNNING DATABASE MIGRATION FROM 29 TO 30")

        database.execSQL("ALTER TABLE beneficiaries ADD hasDuplicateName INTEGER NOT NULL DEFAULT 0")

        val cursor = database.query("SELECT * FROM beneficiaries")

        cursor.moveToFirst()
        val beneficiaryNames = mutableListOf<Triple<String, String, Int>>()
        while (!cursor.isAfterLast) {
            val givenName = cursor.getString(cursor.getColumnIndexOrThrow("givenName"))
            val familyName = cursor.getString(cursor.getColumnIndexOrThrow("familyName"))
            val assistanceId = cursor.getInt(cursor.getColumnIndexOrThrow("assistanceId"))
            beneficiaryNames.add(Triple(givenName, familyName, assistanceId))
            cursor.moveToNext()
        }

        val seenBeneficiaryNames = mutableSetOf<Triple<String, String, Int>>()
        val duplicateBeneficiaryNames = beneficiaryNames.filter {
            !seenBeneficiaryNames.add(
                Triple(
                    it.first,
                    it.second,
                    it.third
                )
            )
        }.distinct().toList()

        cursor.moveToFirst()
        while (!cursor.isAfterLast) {
            val beneficiaryId = cursor.getInt(cursor.getColumnIndexOrThrow("id"))
            val givenName = cursor.getString(cursor.getColumnIndexOrThrow("givenName"))
            val familyName = cursor.getString(cursor.getColumnIndexOrThrow("familyName"))
            val assistanceId = cursor.getInt(cursor.getColumnIndexOrThrow("assistanceId"))
            val hasDuplicateName = duplicateBeneficiaryNames.contains(Triple(givenName, familyName, assistanceId))
            val contentValues = ContentValues()
            contentValues.put("hasDuplicateName", hasDuplicateName)
            val affectedRows = database.update(
                "beneficiaries",
                SQLiteDatabase.CONFLICT_FAIL,
                contentValues,
                "id=?",
                arrayOf(beneficiaryId)
            )

            if (affectedRows == 0) Log.d("HumansisDb MIGRATION", "Migration of Beneficiary $beneficiaryId failed!")

            cursor.moveToNext()
        }
        cursor.close()

        Log.d("HumansisDb MIGRATION", "DATABASE MIGRATION FROM 29 TO 30 FINISHED SUCCESSFULLY")
    }
}