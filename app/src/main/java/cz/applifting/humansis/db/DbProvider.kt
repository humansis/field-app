package cz.applifting.humansis.db

import android.content.Context
import androidx.room.Room
import com.commonsware.cwac.saferoom.BuildConfig
import com.commonsware.cwac.saferoom.SafeHelperFactory
import javax.inject.Singleton

/**
 * Created by Petr Kubes <petr.kubes@applifting.cz> on 11, September, 2019
 */
const val DB_NAME = "humansis-db"

@Singleton
class DbProvider(val context: Context) {

    lateinit var db: HumansisDB

    fun init(password: ByteArray, oldPass: ByteArray? = null) {
        if (!::db.isInitialized) {
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
                    HumansisDB.MIGRATION_20_21,
                    HumansisDB.MIGRATION_23_24
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
