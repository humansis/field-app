package cz.applifting.humansis.synchronization

import java.util.*

/**
 * Created by Petr Kubes <petr.kubes@applifting.cz> on 16, December, 2019
 */
data class SyncWorkerState(
    val isLoading: Boolean,
    val lastSyncFail: Date?,
    val lastDownload: Date?,
    val isFirstCountryDownload: Boolean
)