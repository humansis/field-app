package cz.applifting.humansis.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import cz.applifting.humansis.db.converters.*
import cz.applifting.humansis.db.daos.*
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
    version = 20,
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
}