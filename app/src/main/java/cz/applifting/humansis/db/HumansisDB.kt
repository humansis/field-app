package cz.applifting.humansis.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
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
        User::class,
        BeneficiaryLocal::class,
        ProjectLocal::class,
        DistributionLocal::class,
        SyncError::class
    ],
    version = 21,
    exportSchema = false
)
@TypeConverters(
    StringListConverter::class,
    TargetConverter::class,
    DateConverter::class,
    IntListConverter::class,
    CommodityConverter::class,
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
                database.execSQL("ALTER TABLE beneficiaries ADD dateExpiration TEXT")
                database.execSQL("ALTER TABLE beneficiaries ADD foodLimit REAL")
                database.execSQL("ALTER TABLE beneficiaries ADD nonfoodLimit REAL")
                database.execSQL("ALTER TABLE beneficiaries ADD cashbackLimit REAL")
                database.execSQL("ALTER TABLE beneficiaries ADD originalBalance REAL")
                database.execSQL("ALTER TABLE beneficiaries ADD remote INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}