package cz.applifting.humansis.db.migrations

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import cz.applifting.humansis.db.converters.CommodityConverter
import cz.applifting.humansis.db.converters.CommodityTypeConverter
import quanti.com.kotlinlog.Log

val Migration23to24 = object : Migration(23, 24) {
    override fun migrate(database: SupportSQLiteDatabase) {

        Log.d("HumansisDb MIGRATION", "RUNNING DATABASE MIGRATION FROM 23 TO 24")

        database.execSQL("ALTER TABLE distributions ADD commodityTypes TEXT")
        val cursor = database.query("SELECT * FROM distributions")
        cursor.moveToFirst()
        while (!cursor.isAfterLast) {
            val distributionId = cursor.getInt(cursor.getColumnIndexOrThrow("id"))
            val commoditiesSerialized = cursor.getString(cursor.getColumnIndexOrThrow("commodities"))
            val commodities = CommodityConverter().toList(commoditiesSerialized)
            val commodityTypes = commodities.map { it.type }
            val commodityTypesSerialized = CommodityTypeConverter().toString(commodityTypes)
            val contentValues = ContentValues()
            contentValues.put("commodityTypes", commodityTypesSerialized)
            val affectedRows = database.update(
                "distributions",
                SQLiteDatabase.CONFLICT_FAIL,
                contentValues,
                "id=?",
                arrayOf(distributionId)
            )

            Log.d("HumansisDb MIGRATION", if (affectedRows > 0) "Distribution $distributionId migrated!" else "Migration of Distribution $distributionId failed!")

            cursor.moveToNext()
        }
        cursor.close()

        Log.d("HumansisDb MIGRATION", "DATABASE MIGRATION FROM 23 TO 24 FINISHED SUCCESSFULLY")
    }
}