package cz.applifting.humansis.db

import androidx.room.*
import cz.applifting.humansis.db.converters.*
import cz.applifting.humansis.db.dao.*
import cz.applifting.humansis.db.migrations.Migration21to22
import cz.applifting.humansis.db.migrations.Migration22to23
import cz.applifting.humansis.db.migrations.Migration24to25
import cz.applifting.humansis.db.migrations.Migration26to27
import cz.applifting.humansis.db.migrations.Migration28to29
import cz.applifting.humansis.model.db.BeneficiaryLocal
import cz.applifting.humansis.model.db.DistributionLocal
import cz.applifting.humansis.model.db.ProjectLocal
import cz.applifting.humansis.model.db.SyncError
import cz.applifting.humansis.model.db.UserDbEntity

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
    version = 30,
    exportSchema = true,

    // When writing new AutoMigrations, pay attention to app/schemas/currentVersion.json that it has
    // not changed since the last release as it might introduce serious bugs that are hard to trace.
    // i.e. when migrating from version 30 to version 31, look up last release in git, download "30.json"
    // and compare it with your current file, that there are no new unwanted changes.
    autoMigrations = [
        AutoMigration(
            from = 21,
            to = 22,
            spec = Migration21to22::class
        ),
        AutoMigration(
            from = 22,
            to = 23,
            spec = Migration22to23::class
        ),
        AutoMigration(
            from = 24,
            to = 25,
            spec = Migration24to25::class
        ),
        AutoMigration(
            from = 26,
            to = 27,
            spec = Migration26to27::class
        ),
        AutoMigration(
            from = 28,
            to = 29,
            spec = Migration28to29::class
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
    CountryConverter::class,
    NationalCardIdConverter::class
)
abstract class HumansisDB : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun beneficiariesDao(): BeneficiaryDao
    abstract fun projectsDao(): ProjectsDao
    abstract fun distributionsDao(): DistributionsDao
    abstract fun errorsDao(): ErrorDao
}