package cz.applifting.humansis.db

import android.content.Context
import androidx.room.Room
import com.commonsware.cwac.saferoom.BuildConfig
import com.commonsware.cwac.saferoom.SafeHelperFactory
import cz.applifting.humansis.db.migrations.Migration20to21
import cz.applifting.humansis.db.migrations.Migration23to24
import cz.applifting.humansis.db.migrations.Migration25to26
import cz.applifting.humansis.db.migrations.Migration27to28
import cz.applifting.humansis.db.migrations.Migration29to30
import javax.inject.Singleton

/**
 * Created by Petr Kubes <petr.kubes@applifting.cz> on 11, September, 2019
 */
const val DB_NAME = "humansis-db"

@Singleton
class DbProvider(val context: Context) {

    lateinit var db: HumansisDB

    fun init(password: ByteArray, oldPass: ByteArray? = null) {
        if (!isInitialized()) {
            db = Room.databaseBuilder(
                context,
                HumansisDB::class.java, DB_NAME
            )
                .apply {
                    if (!BuildConfig.DEBUG) {
                        openHelperFactory(
                            SafeHelperFactory(
                                if (oldPass != null) {
                                    String(oldPass).toCharArray()
                                } else {
                                    String(password).toCharArray()
                                }
                            )
                        )
                    }
                }
                .addMigrations(
                    Migration20to21,
                    Migration23to24,
                    Migration25to26,
                    Migration27to28,
                    Migration29to30
                )
                .fallbackToDestructiveMigration()
                .build()
        }

        if (oldPass != null && !BuildConfig.DEBUG) {
            SafeHelperFactory.rekey(db.openHelper.readableDatabase, String(password).toCharArray())
        }
    }

    fun get(): HumansisDB {
        return db
    }

    fun isInitialized(): Boolean = ::db.isInitialized
}
