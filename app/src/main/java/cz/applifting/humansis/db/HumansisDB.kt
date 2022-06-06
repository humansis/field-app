package cz.applifting.humansis.db

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.*
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import cz.applifting.humansis.db.converters.*
import cz.applifting.humansis.db.dao.*
import cz.applifting.humansis.model.db.*

/**
 * Created by Petr Kubes <petr.kubes@applifting.cz> on 21, August, 2019
 */
@Database(
    entities = [
        UserDbEntity::class,
        BeneficiaryLocal::class,
        ProjectLocal::class,
        DistributionLocal::class,
        SyncError::class
    ],
    version = 25,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(
            from = 21,
            to = 22,
            spec = HumansisDB.AutoMigrationTo22::class
        ),
        AutoMigration(
            from = 22,
            to = 23,
            spec = HumansisDB.AutoMigrationTo23::class
        ),
        AutoMigration(
            from = 24,
            to = 25,
            spec = HumansisDB.AutoMigrationTo25::class
        )
    ]
)
@TypeConverters(
    StringListConverter::class,
    TargetConverter::class,
    DateConverter::class,
    IntListConverter::class,
    CommodityConverter::class,
    CommodityTypeConverter::class,
    ReferralTypeConverter::class,
    CountryConverter::class
)
abstract class HumansisDB : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun beneficiariesDao(): BeneficiaryDao
    abstract fun projectsDao(): ProjectsDao
    abstract fun distributionsDao(): DistributionsDao
    abstract fun errorsDao(): ErrorDao

    companion object {
        val MIGRATION_20_21 = object : Migration(20, 21) {
            override fun migrate(database: SupportSQLiteDatabase) {
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
            }
        }

        val MIGRATION_23_24 = object : Migration(23, 24) {
            override fun migrate(database: SupportSQLiteDatabase) {

                // TODO zkusit volat podobnou metodu pomoci tlacitka nekde v appce kde mam pristup k db, at nemusim pri testovani delat milion migraci

                database.execSQL("ALTER TABLE distributions ADD commodityTypes TEXT")
                val cursor = database.query("SELECT * FROM distributions")
                cursor.moveToFirst()
                while (!cursor.isAfterLast) {

                    val distributionId = cursor.getInt(cursor.getColumnIndexOrThrow("id"))
                    val commoditiesSerialized = cursor.getString(cursor.getColumnIndexOrThrow("commodities"))
                    val commodities : List<CommodityLocal> = CommodityConverter().toList(commoditiesSerialized)
                    val commodityTypes = commodities.map { it.type }
                    val commodityTypesSerialized = CommodityTypeConverter().toString(commodityTypes)
                    val contentValues = ContentValues()
                    contentValues.put("commodityTypes", commodityTypesSerialized)
                    database.update(
                        "distributions",
                        SQLiteDatabase.CONFLICT_NONE,
                        contentValues,
                        "id = ?s",
                        arrayOf(distributionId)
                    )

                    cursor.moveToNext()
                }
                cursor.close()

                // TODO pripsat automigraci 24_25, ktera smazne column commodities, zvednout verzi db na 25
            }
        }
    }

    @RenameColumn(tableName = "beneficiaries", fromColumnName = "distributionId", toColumnName = "assistanceId")
    @DeleteColumn(tableName = "beneficiaries", columnName = "vulnerabilities")
    class AutoMigrationTo22 : AutoMigrationSpec

    @DeleteColumn(
        tableName = "User",
        columnName = "salted_password"
    )
    class AutoMigrationTo23 : AutoMigrationSpec

    @DeleteColumn(
        tableName = "distributions",
        columnName = "commodities"
    )
    class AutoMigrationTo25 : AutoMigrationSpec

    // When writing new AutoMigrations, pay attention to app/schemas/currentVersion.json that it has
    // not changed since the last release as it might introduce serious bugs that are hard to trace.
    // i.e. when migrating from version 23 to version 24, look up last release in git, download "23.json"
    // and compare it with your current file, that there are no new unwanted changes.
}