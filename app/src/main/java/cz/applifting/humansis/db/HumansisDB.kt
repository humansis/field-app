package cz.applifting.humansis.db

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
    version = 23,
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
            from = 23,
            to = 24,
            spec = HumansisDB.AutoMigrationTo24::class
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
    }

    @RenameColumn(tableName = "beneficiaries", fromColumnName = "distributionId", toColumnName = "assistanceId")
    @DeleteColumn(tableName = "beneficiaries", columnName = "vulnerabilities")
    class AutoMigrationTo22 : AutoMigrationSpec

    @DeleteColumn(
        tableName = "User",
        columnName = "salted_password"
    )
    class AutoMigrationTo23 : AutoMigrationSpec

    // TODO přemapovat list Commodities na list CommodityTypes
    class AutoMigrationTo24 : AutoMigrationSpec

    // When writing new AutoMigrations, pay attention to app/schemas/currentVersion.json that it has
    // not changed since the last release as it might introduce serious bugs that are hard to trace.
    // i.e. when migrating from version 23 to version 24, look up last release in git, download "23.json"
    // and compare it with your current file, that there are no new unwanted changes.
}