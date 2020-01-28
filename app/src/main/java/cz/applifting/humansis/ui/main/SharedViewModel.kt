package cz.applifting.humansis.ui.main

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkManager
import cz.applifting.humansis.extensions.getDate
import cz.applifting.humansis.managers.LoginManager
import cz.applifting.humansis.managers.SP_FIRST_COUNTRY_DOWNLOAD
import cz.applifting.humansis.misc.Logger
import cz.applifting.humansis.misc.booleanLiveData
import cz.applifting.humansis.repositories.BeneficiariesRepository
import cz.applifting.humansis.repositories.DistributionsRepository
import cz.applifting.humansis.repositories.ProjectsRepository
import cz.applifting.humansis.synchronization.*
import cz.applifting.humansis.ui.BaseViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Created by Petr Kubes <petr.kubes@applifting.cz> on 10, September, 2019
 */
const val LAST_DOWNLOAD_KEY = "lastDownloadKey"
const val LAST_SYNC_FAILED_KEY = "lastSyncFailedKey"

class SharedViewModel @Inject constructor(
    private val projectsRepository: ProjectsRepository,
    private val distributionsRepository: DistributionsRepository,
    private val beneficiariesRepository: BeneficiariesRepository,
    private val loginManager: LoginManager,
    private val logger: Logger,
    private val sp: SharedPreferences,
    private val context: Context
) : BaseViewModel() {

    val toastLD = MediatorLiveData<String>()
    private val pendingChangesLD = MutableLiveData<Boolean>()
    private val uploadIncompleteLD = sp.booleanLiveData(SP_SYNC_UPLOAD_INCOMPLETE, false)
    val syncNeededLD = MediatorLiveData<Boolean>()
    val networkStatus = MutableLiveData<Boolean>()
    val shouldReauthenticateLD = MediatorLiveData<Boolean>()

    val syncState: MediatorLiveData<SyncWorkerState> = MediatorLiveData()

    private val workInfos: LiveData<List<WorkInfo>>

    private val workManager = WorkManager.getInstance(context)

    init {
        workInfos = workManager.getWorkInfosForUniqueWorkLiveData(SYNC_WORKER)

        syncState.addSource(workInfos) {
            syncState.value = SyncWorkerState(
                isLoading(it),
                sp.getDate(LAST_SYNC_FAILED_KEY),
                sp.getDate(LAST_DOWNLOAD_KEY),
                sp.getBoolean(SP_FIRST_COUNTRY_DOWNLOAD, false)
            )
        }

        shouldReauthenticateLD.addSource(workInfos) {
            launch {
                shouldReauthenticateLD.value = loginManager.retrieveUser()?.invalidPassword == true

                if (loginManager.retrieveUser()?.invalidPassword == true) {
                    sp.edit().putBoolean("test", false).commit()
                }
            }
        }

        // TODO toast is shown every time sync fails and user opens the app. I can not think of a simpler solution than saving the toast to persistent memory
        toastLD.addSource(workInfos) {
            if (it.isNullOrEmpty()) {
                toastLD.value = null
                return@addSource
            }

            if (it.first().state == WorkInfo.State.FAILED) {
                val errors = it.first().outputData.getStringArray(ERROR_MESSAGE_KEY)
                // show only first error in toast
                val error = errors?.first()

                toastLD.value = error
            }
        }


        launch {
            beneficiariesRepository
                .arePendingChanges()
                .collect {
                    pendingChangesLD.value = it.isNotEmpty()
                }
        }
        syncNeededLD.apply {
            addSource(uploadIncompleteLD) {
                syncNeededLD.value = it || pendingChangesLD.value ?: false
            }
            addSource(pendingChangesLD) {
                syncNeededLD.value = it || uploadIncompleteLD.value ?: false
            }
        }
    }

    fun forceSynchronize() {
        launch {
            if (workInfos.value?.first()?.state == WorkInfo.State.ENQUEUED) {
                // cancel previous work which may be stuck in the queue
                workManager.cancelUniqueWork(SYNC_WORKER)
            }
            workManager.enqueueUniqueWork(SYNC_WORKER, ExistingWorkPolicy.KEEP, OneTimeWorkRequest.from(SyncWorker::class.java))
        }
    }

    fun tryFirstDownload() {
        launch {
            if (sp.getDate(LAST_DOWNLOAD_KEY) == null || projectsRepository.getProjectsOfflineSuspend().isEmpty()) {
                forceSynchronize()
            }
        }
    }

    fun showToast(text: String?) {
        toastLD.value = text
    }

    fun resetShouldReauthenticate() {
        shouldReauthenticateLD.value = false
    }

    private fun isLoading(workInfos: List<WorkInfo>): Boolean {
        if (workInfos.isNullOrEmpty()) {
            return false
        }
        launch { logger.logToFile(context, "Worker state: ${workInfos.first().state}") }
        return workInfos.first().state == WorkInfo.State.RUNNING
    }
}