package cz.applifting.humansis.synchronization

import android.content.Context
import android.content.SharedPreferences
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import cz.applifting.humansis.R
import cz.applifting.humansis.api.interceptor.HostUrlInterceptor
import cz.applifting.humansis.api.interceptor.LoggingInterceptor
import cz.applifting.humansis.extensions.setDate
import cz.applifting.humansis.extensions.suspendCommit
import cz.applifting.humansis.managers.LoginManager
import cz.applifting.humansis.misc.ApiEnvironment
import cz.applifting.humansis.misc.SP_ENVIRONMENT_NAME
import cz.applifting.humansis.misc.SP_ENVIRONMENT_URL
import cz.applifting.humansis.misc.SP_FIRST_COUNTRY_DOWNLOAD
import cz.applifting.humansis.misc.SP_LAST_DOWNLOAD
import cz.applifting.humansis.misc.SP_LAST_SYNC_FAILED
import cz.applifting.humansis.misc.SP_SYNC_SUMMARY
import cz.applifting.humansis.misc.SP_SYNC_UPLOAD_INCOMPLETE
import cz.applifting.humansis.model.db.BeneficiaryLocal
import cz.applifting.humansis.model.db.ProjectLocal
import cz.applifting.humansis.model.db.SyncError
import cz.applifting.humansis.model.db.SyncErrorActionEnum
import cz.applifting.humansis.repositories.BeneficiariesRepository
import cz.applifting.humansis.repositories.AssistancesRepository
import cz.applifting.humansis.repositories.ErrorsRepository
import cz.applifting.humansis.repositories.LogsRepository
import cz.applifting.humansis.repositories.ProjectsRepository
import cz.applifting.humansis.ui.App
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope
import quanti.com.kotlinlog.Log
import retrofit2.HttpException

/**
 * Created by Petr Kubes <petr.kubes@applifting.cz> on 05, October, 2019
 */
const val SYNC_WORKER = "sync-worker"

const val ERROR_MESSAGE_KEY = "error-message-key"

class SyncWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    @Inject
    lateinit var projectsRepository: ProjectsRepository

    @Inject
    lateinit var assistancesRepository: AssistancesRepository

    @Inject
    lateinit var beneficiariesRepository: BeneficiariesRepository

    @Inject
    lateinit var logsRepository: LogsRepository

    @Inject
    lateinit var sp: SharedPreferences

    @Inject
    lateinit var loginManager: LoginManager

    @Inject
    lateinit var errorsRepository: ErrorsRepository

    @Inject
    lateinit var hostUrlInterceptor: HostUrlInterceptor

    @Inject
    lateinit var loggingInterceptor: LoggingInterceptor

    private val reason = Data.Builder()
    private val syncErrors = arrayListOf<SyncError>()
    private val syncStats = SyncStats()

    init {
        (appContext as App).appComponent.inject(this)
    }

    override suspend fun doWork(): Result {
        return supervisorScope {

            if (isStopped) return@supervisorScope stopWork(
                "Before initialization",
                SyncErrorActionEnum.BEFORE_INITIALIZATION
            )

            // TODO PIN-4471 do not sync if another sync is in progress

            Log.d(TAG, "Started Sync as ${loginManager.retrieveUser()?.username}")

            val host = sp.getString(SP_ENVIRONMENT_NAME, null)?.let { name ->
                sp.getString(SP_ENVIRONMENT_URL, null)?.let { url ->
                    ApiEnvironment.find(name, url)
                }
            } ?: ApiEnvironment.Stage // fallback to stage, because environment was not saved to SP when no environment was selected in debug builds on login screen until v3.4.1

            hostUrlInterceptor.setHost(host)
            loggingInterceptor.setShouldLogHeaders(true)

            sp.edit().putString(SP_SYNC_SUMMARY, "").suspendCommit()

            if (!loginManager.tryInitDB()) {
                reason.putStringArray(
                    ERROR_MESSAGE_KEY,
                    arrayOf("Could not read DB.")
                )
                Log.d(TAG, "Failed to read db")
                return@supervisorScope Result.failure(reason.build())
            }

            errorsRepository.clearAll()

            suspend fun logUploadError(
                e: HttpException,
                it: BeneficiaryLocal,
                action: SyncErrorActionEnum
            ) {
                CoroutineScope(Dispatchers.IO).runCatching {
                    val errBody = "${e.response()?.errorBody()?.string()}"
                    Log.d(TAG, "Failed uploading [$action]: ${it.id}: $errBody")

                    // Mark conflicts in DB
                    val assistanceName = assistancesRepository.getNameById(it.assistanceId)
                    val projectName = projectsRepository.getNameByAssistanceId(it.assistanceId)
                    val beneficiaryName = "${it.givenName} ${it.familyName}"

                    val syncError = SyncError(
                        id = it.id,
                        location = "[$action] $projectName → $assistanceName → $beneficiaryName",
                        params = "Humansis ID: ${it.beneficiaryId}\n${it.nationalIds}",
                        code = e.code(),
                        errorMessage = "${e.code()}: $errBody",
                        beneficiaryId = it.id,
                        syncErrorAction = action
                    )

                    syncErrors.add(syncError)
                }
            }

            val assignedBeneficiaries =
                beneficiariesRepository.getAssignedBeneficiariesOfflineSuspend()
            val referralChangedBeneficiaries =
                beneficiariesRepository.getAllReferralChangesOffline()
            syncStats.uploadCandidatesCount = assignedBeneficiaries.count()
            if (assignedBeneficiaries.isNotEmpty() || referralChangedBeneficiaries.isNotEmpty()) {
                sp.edit().putBoolean(SP_SYNC_UPLOAD_INCOMPLETE, true).suspendCommit()
            }

            if (isStopped) return@supervisorScope stopWork(
                "After initialization",
                SyncErrorActionEnum.AFTER_INITIALIZATION
            )

            // Upload assistances to beneficiaries
            assignedBeneficiaries
                .forEach {
                    try {
                        beneficiariesRepository.distribute(it)
                        syncStats.countUploadSuccess()
                    } catch (e: HttpException) {
                        logUploadError(e, it, SyncErrorActionEnum.ASSISTANCE)
                    }
                    if (isStopped) return@supervisorScope stopWork(
                        "Uploading ${it.beneficiaryId}",
                        SyncErrorActionEnum.ASSISTANCE
                    )
                }
            // Upload changes of referral
            referralChangedBeneficiaries
                .forEach {
                    try {
                        beneficiariesRepository.updateBeneficiaryReferralOnline(it)
                    } catch (e: HttpException) {
                        logUploadError(e, it, SyncErrorActionEnum.REFERRAL_UPDATE)
                    }
                    if (isStopped) return@supervisorScope stopWork(
                        "Uploading ${it.beneficiaryId}",
                        SyncErrorActionEnum.REFERRAL_UPDATE
                    )
                }

            // Download updated data
            if (syncErrors.isEmpty()) {

                val projects = try {
                    projectsRepository.getProjectsOnline()
                } catch (e: Exception) {
                    syncErrors.add(
                        getDownloadError(
                            e,
                            applicationContext.getString(R.string.projects),
                            SyncErrorActionEnum.PROJECTS_DOWNLOAD
                        )
                    )
                    emptyList()
                }

                if (isStopped) return@supervisorScope stopWork(
                    "Downloading projects",
                    SyncErrorActionEnum.PROJECTS_DOWNLOAD
                )

                val assistances = try {
                    projects.map {
                        async { assistancesRepository.getAssistancesOnline(it.id) }
                    }.flatMap {
                        it.await().toList()
                    }
                } catch (e: Exception) {
                    syncErrors.add(
                        getDownloadError(
                            e,
                            applicationContext.getString(R.string.assistance),
                            SyncErrorActionEnum.ASSISTANCES_DOWNLOAD
                        )
                    )
                    emptyList()
                }

                if (isStopped) return@supervisorScope stopWork(
                    "Downloading assistances",
                    SyncErrorActionEnum.ASSISTANCES_DOWNLOAD
                )

                try {
                    assistances.map {
                        async { beneficiariesRepository.getBeneficiariesOnline(it.id) }
                    }.map {
                        it.await()
                    }
                } catch (e: Exception) {
                    syncErrors.add(
                        getDownloadError(
                            e,
                            applicationContext.getString(R.string.beneficiary),
                            SyncErrorActionEnum.BENEFICIARIES_DOWNLOAD
                        )
                    )
                    emptyList<ProjectLocal>()
                }
            }

            // Upload logs
            try {
                loginManager.retrieveUser()?.id?.let { id ->
                    logsRepository.postLogs(id)
                }
            } catch (e: Exception) {
                syncErrors.add(
                    getUploadError(
                        e,
                        applicationContext.getString(R.string.logs),
                        SyncErrorActionEnum.LOGS_UPLOAD_NEW
                    )
                )
            }
            if (isStopped) return@supervisorScope stopWork(
                "Uploading logs",
                SyncErrorActionEnum.LOGS_UPLOAD_NEW
            )

            finishWork()
        }
    }

    private suspend fun stopWork(location: String, action: SyncErrorActionEnum): Result {
        syncErrors.add(
            SyncError(
                location = location,
                params = "",
                code = 0,
                errorMessage = "Sync was stopped by work manager",
                syncErrorAction = action
            )
        )
        return finishWork()
    }

    private suspend fun finishWork(): Result {
        sp.edit().putString(SP_SYNC_SUMMARY, syncStats.toString()).suspendCommit()
        return if (syncErrors.isEmpty()) {
            sp.setDate(SP_LAST_DOWNLOAD, Date())
            sp.setDate(SP_LAST_SYNC_FAILED, null)
            sp.edit().putBoolean(SP_FIRST_COUNTRY_DOWNLOAD, false).suspendCommit()
            sp.edit().putBoolean(SP_SYNC_UPLOAD_INCOMPLETE, false).suspendCommit()
            Log.d(TAG, "Sync finished successfully")
            Result.success()
        } else {
            errorsRepository.insertAll(syncErrors)

            // Erase password to trigger re-authentication
            // 401 response code means token was expired or that no token was sent at all
            if (syncErrors.find { it.code == 401 } != null) {
                loginManager.forceReauthentication()
                sp.setDate(SP_LAST_DOWNLOAD, null)
            }

            Log.d(TAG, "Sync finished with failure")
            sp.setDate(SP_LAST_SYNC_FAILED, Date())

            Result.failure(
                reason.putStringArray(ERROR_MESSAGE_KEY, convertErrors(syncErrors)).build()
            )
        }
    }

    private fun convertErrors(errors: List<SyncError>): Array<String> {
        return errors.map {
            it.errorMessage
        }.toTypedArray()
    }

    private fun getErrorMessageByCode(code: Int): String {
        return applicationContext.getString(
            when (code) {
                400 -> R.string.error_bad_request
                403 -> R.string.error_user_not_allowed
                404 -> R.string.error_resource_not_found
                409 -> R.string.error_data_conflict
                410 -> R.string.error_server_api_changed
                429 -> R.string.error_too_many_requests
                in 500..599 -> R.string.error_server_failure
                else -> R.string.error_other
            }
        )
    }

    private fun getDownloadError(
        e: Exception,
        resourceName: String,
        action: SyncErrorActionEnum
    ): SyncError {
        Log.e(TAG, e, "Failed downloading $resourceName")

        return when (e) {
            is HttpException -> {
                SyncError(
                    location = applicationContext.getString(R.string.download_error)
                        .format(resourceName.toLowerCase(Locale.ROOT)),
                    params = applicationContext.getString(R.string.error_server),
                    errorMessage = getErrorMessageByCode(e.code()),
                    code = e.code(),
                    syncErrorAction = action
                )
            }
            else -> {
                SyncError(
                    location = applicationContext.getString(R.string.download_error)
                        .format(resourceName.toLowerCase(Locale.ROOT)),
                    params = applicationContext.getString(R.string.unknwon_error),
                    errorMessage = e.message ?: "",
                    code = 0,
                    syncErrorAction = action
                )
            }
        }
    }

    private fun getUploadError(
        e: Exception,
        resourceName: String,
        action: SyncErrorActionEnum
    ): SyncError {
        Log.e(TAG, e, "Failed uploading $resourceName")

        return when (e) {
            is HttpException -> {
                SyncError(
                    location = applicationContext.getString(R.string.upload_error)
                        .format(resourceName.toLowerCase(Locale.ROOT)),
                    params = applicationContext.getString(R.string.error_server),
                    errorMessage = getErrorMessageByCode(e.code()),
                    code = e.code(),
                    syncErrorAction = action
                )
            }
            else -> {
                SyncError(
                    location = applicationContext.getString(R.string.upload_error)
                        .format(resourceName.toLowerCase(Locale.ROOT)),
                    params = applicationContext.getString(R.string.unknwon_error),
                    errorMessage = e.message ?: "",
                    code = 0,
                    syncErrorAction = action
                )
            }
        }
    }

    private inner class SyncStats(
        var uploadCandidatesCount: Int? = null,
        private var uploadedSuccessfullyCount: Int = 0
    ) {
        fun countUploadSuccess() {
            uploadedSuccessfullyCount++
        }

        override fun toString(): String {
            return if (uploadCandidatesCount == null || uploadCandidatesCount == 0) {
                applicationContext.getString(R.string.sync_summary_nothing)
            } else {
                applicationContext.getString(
                    R.string.sync_summary,
                    uploadedSuccessfullyCount,
                    uploadCandidatesCount
                )
            }
        }
    }

    companion object {
        private val TAG = SyncWorker::class.java.simpleName
    }
}
