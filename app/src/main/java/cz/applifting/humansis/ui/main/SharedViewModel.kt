package cz.applifting.humansis.ui.main

import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkManager
import cz.applifting.humansis.extensions.getDate
import cz.applifting.humansis.extensions.suspendCommit
import cz.applifting.humansis.managers.LoginManager
import cz.applifting.humansis.managers.SP_FIRST_COUNTRY_DOWNLOAD
import cz.applifting.humansis.managers.ToastManager
import cz.applifting.humansis.misc.SingleLiveEvent
import cz.applifting.humansis.misc.booleanLiveData
import cz.applifting.humansis.misc.connectionObserver.ConnectionObserver
import cz.applifting.humansis.model.db.SyncErrorActionEnum
import cz.applifting.humansis.repositories.BeneficiariesRepository
import cz.applifting.humansis.repositories.ErrorsRepository
import cz.applifting.humansis.repositories.ProjectsRepository
import cz.applifting.humansis.synchronization.ERROR_MESSAGE_KEY
import cz.applifting.humansis.synchronization.SP_SYNC_UPLOAD_INCOMPLETE
import cz.applifting.humansis.synchronization.SYNC_WORKER
import cz.applifting.humansis.synchronization.SyncWorker
import cz.applifting.humansis.synchronization.SyncWorkerState
import cz.applifting.humansis.ui.App
import cz.applifting.humansis.ui.BaseViewModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import quanti.com.kotlinlog.Log
import javax.inject.Inject

/**
 * Created by Petr Kubes <petr.kubes@applifting.cz> on 10, September, 2019
 */
const val LAST_DOWNLOAD_KEY = "lastDownloadKey"
const val LAST_SYNC_FAILED_KEY = "lastSyncFailedKey"
const val LAST_SYNC_FAILED_ID_KEY = "lastSyncFailedIdKey"

class SharedViewModel @Inject constructor(
    private val projectsRepository: ProjectsRepository,
    private val beneficiariesRepository: BeneficiariesRepository,
    private val errorsRepository: ErrorsRepository,
    private val loginManager: LoginManager,
    private val toastManager: ToastManager,
    private val connectionObserver: ConnectionObserver,
    private val sp: SharedPreferences,
    app: App
) : BaseViewModel(app) {

    private val pendingChangesLD = MutableLiveData<Boolean>()
    private val uploadIncompleteLD = sp.booleanLiveData(SP_SYNC_UPLOAD_INCOMPLETE, false)
    val syncNeededLD = MediatorLiveData<Boolean>()
    val networkStatus = MutableLiveData<Boolean>()
    val shouldReauthenticateLD = MediatorLiveData<Boolean>()
    val shouldDismissBeneficiaryDialog = SingleLiveEvent<Unit>()
    val beneficiaryDialogDissmissedOnSuccess = SingleLiveEvent<Unit>()

    val syncState: MediatorLiveData<SyncWorkerState> = MediatorLiveData()

    val logsUploadFailed: SingleLiveEvent<Unit> = SingleLiveEvent()

    private val workInfos: LiveData<List<WorkInfo>>

    private val workManager = WorkManager.getInstance(getApplication())

    private var connectionDisposable: Disposable? = null

    init {
        workInfos = workManager.getWorkInfosForUniqueWorkLiveData(SYNC_WORKER)

        syncState.addSource(workInfos) {
            launch {
                syncState.value = SyncWorkerState(
                    isLoading(it),
                    sp.getDate(LAST_SYNC_FAILED_KEY),
                    sp.getDate(LAST_DOWNLOAD_KEY),
                    sp.getBoolean(SP_FIRST_COUNTRY_DOWNLOAD, false),
                    logsUploadFailedOnly()
                )

                if (it.firstOrNull()?.state == WorkInfo.State.FAILED) {
                    errorsRepository.getAll().collect { errors ->
                        errors.find { error -> error.syncErrorAction == SyncErrorActionEnum.LOGS_UPLOAD_NEW }
                            ?.let {
                                logsUploadFailed.call()
                                errorsRepository.update(it.copy(syncErrorAction = SyncErrorActionEnum.LOGS_UPLOAD))
                            }
                    }
                }
            }
        }

        shouldReauthenticateLD.addSource(workInfos) {
            launch {
                shouldReauthenticateLD.value = loginManager.retrieveUser()?.shouldReauthenticate == true

                if (loginManager.retrieveUser()?.shouldReauthenticate == true) {
                    sp.edit().putBoolean("test", false).apply()
                }
            }
        }

        toastManager.getToastMessageLiveData().addSource(workInfos) {
            if (it.isNullOrEmpty()) {
                return@addSource
            }

            val lastInfo = it.first()
            val lastInfoId = lastInfo.id.toString()
            val lastShownInfoId = sp.getString(LAST_SYNC_FAILED_ID_KEY, null)
            if (lastInfo.state == WorkInfo.State.FAILED && lastInfoId != lastShownInfoId) {
                val errors = lastInfo.outputData.getStringArray(ERROR_MESSAGE_KEY)
                // show only first error in toast
                errors?.firstOrNull()?.let { error ->
                    setToastMessage(error)
                }

                launch {
                    // avoid showing the same error toast twice (after restarting the app)
                    sp.edit().putString(LAST_SYNC_FAILED_ID_KEY, lastInfoId).suspendCommit()
                }
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
            workManager.enqueueUniqueWork(
                SYNC_WORKER,
                ExistingWorkPolicy.KEEP,
                OneTimeWorkRequest.from(SyncWorker::class.java)
            )
        }
    }

    fun tryFirstDownload() {
        launch {
            if (sp.getDate(LAST_DOWNLOAD_KEY) == null || projectsRepository.getProjectsOfflineSuspend()
                    .isEmpty()
            ) {
                forceSynchronize()
            }
        }
    }

    fun setToastMessage(text: String) {
        toastManager.setToastMessage(text)
    }

    fun resetShouldReauthenticate() {
        shouldReauthenticateLD.value = false
    }

    private fun isLoading(workInfos: List<WorkInfo>): Boolean {
        if (workInfos.isEmpty()) {
            return false
        }
        launch { Log.d(TAG, "Worker state: ${workInfos.first().state}") }
        return workInfos.first().state == WorkInfo.State.RUNNING
    }

    private suspend fun logsUploadFailedOnly(): Boolean {
        val errors = errorsRepository.getAll().first()
        val logUploadErrorActions =
            setOf(SyncErrorActionEnum.LOGS_UPLOAD_NEW, SyncErrorActionEnum.LOGS_UPLOAD)
        return errors.size == 1 && logUploadErrorActions.contains(errors.single().syncErrorAction)
    }

    fun getNetworkStatus(): LiveData<Boolean> {
        return networkStatus
    }

    fun observeConnection() {
        connectionDisposable?.dispose()
        connectionDisposable = connectionObserver.getNetworkAvailability()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                {
                    networkStatus.value = it
                },
                {
                }
            )
    }

    fun stopObservingConnection() {
        connectionDisposable?.dispose()
    }

    companion object {
        private val TAG = SharedViewModel::class.java.simpleName
    }
}