package cz.applifting.humansis.db.migrations

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import cz.applifting.humansis.db.converters.NationalCardIdConverter
import cz.applifting.humansis.model.api.NationalCardId
import cz.applifting.humansis.model.api.NationalCardIdType
import quanti.com.kotlinlog.Log

val Migration25to26 = object : Migration(25, 26) {
    override fun migrate(database: SupportSQLiteDatabase) {
        Log.d("HumansisDb MIGRATION", "RUNNING DATABASE MIGRATION FROM 25 TO 26")

        database.execSQL("ALTER TABLE beneficiaries ADD nationalIds TEXT")
        val cursor = database.query("SELECT * FROM beneficiaries")
        cursor.moveToFirst()
        while (!cursor.isAfterLast) {
            val beneficiaryId = cursor.getInt(cursor.getColumnIndexOrThrow("id"))
            val nationalId = cursor.getString(cursor.getColumnIndexOrThrow("nationalId")) ?: ""
            val nationalIds = listOf(NationalCardId(NationalCardIdType.NATIONAL_ID, nationalId))
            val nationalIdsSerialized = NationalCardIdConverter().toString(nationalIds)
            val contentValues = ContentValues()
            contentValues.put("nationalIds", nationalIdsSerialized)
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

        Log.d("HumansisDb MIGRATION", "DATABASE MIGRATION FROM 25 TO 26 FINISHED SUCCESSFULLY")
    }
}