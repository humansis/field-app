package cz.applifting.humansis.repositories

import cz.applifting.humansis.db.DbProvider
import cz.applifting.humansis.model.db.SyncError
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Created by Petr Kubes <petr.kubes@applifting.cz> on 27, November, 2019
 */
@Singleton
class ErrorsRepository @Inject constructor(val dbProvider: DbProvider) {

    fun getAll(): Flow<List<SyncError>> {
        return dbProvider.get().errorsDao().getAll()
    }

    suspend fun insertAll(syncErrors: List<SyncError>) {
        dbProvider.get().errorsDao().insertAll(syncErrors)
    }

    suspend fun clearAll() {
        dbProvider.get().errorsDao().deleteAll()
    }

    suspend fun update(syncError: SyncError) {
        dbProvider.get().errorsDao().update(syncError)
    }
}