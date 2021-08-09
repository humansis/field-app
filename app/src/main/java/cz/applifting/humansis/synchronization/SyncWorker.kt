package cz.applifting.humansis.synchronization

import android.content.Context
import android.content.SharedPreferences
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import cz.applifting.humansis.BuildConfig
import cz.applifting.humansis.R
import cz.applifting.humansis.api.HostUrlInterceptor
import cz.applifting.humansis.extensions.setDate
import cz.applifting.humansis.extensions.suspendCommit
import cz.applifting.humansis.managers.LoginManager
import cz.applifting.humansis.managers.SP_COUNTRY
import cz.applifting.humansis.managers.SP_FIRST_COUNTRY_DOWNLOAD
import cz.applifting.humansis.misc.ApiEnvironments
import cz.applifting.humansis.model.api.Commodity
import cz.applifting.humansis.model.db.BeneficiaryLocal
import cz.applifting.humansis.model.db.DistributionLocal
import cz.applifting.humansis.model.db.ProjectLocal
import cz.applifting.humansis.model.db.SyncError
import cz.applifting.humansis.repositories.AssistancesRepository
import cz.applifting.humansis.repositories.BeneficiariesRepository
import cz.applifting.humansis.repositories.ErrorsRepository
import cz.applifting.humansis.repositories.ProjectsRepository
import cz.applifting.humansis.ui.App
import cz.applifting.humansis.ui.login.SP_ENVIRONMENT
import cz.applifting.humansis.ui.main.LAST_DOWNLOAD_KEY
import cz.applifting.humansis.ui.main.LAST_SYNC_FAILED_KEY
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope
import quanti.com.kotlinlog.Log
import retrofit2.HttpException
import java.util.*
import javax.inject.Inject

/**
 * Created by Petr Kubes <petr.kubes@applifting.cz> on 05, October, 2019
 */
const val SYNC_WORKER = "sync-worker"

const val ERROR_MESSAGE_KEY = "error-message-key"

const val SP_SYNC_UPLOAD_INCOMPLETE = "sync-upload-incomplete"
const val SP_SYNC_SUMMARY = "sync-summary"

class SyncWorker(appContext: Context, workerParams: WorkerParameters) : CoroutineWorker(appContext, workerParams) {

    @Inject
    lateinit var projectsRepository: ProjectsRepository
    @Inject
    lateinit var assistancesRepository: AssistancesRepository
    @Inject
    lateinit var beneficiariesRepository: BeneficiariesRepository
    @Inject
    lateinit var sp: SharedPreferences
    @Inject
    lateinit var loginManager: LoginManager
    @Inject
    lateinit var errorsRepository: ErrorsRepository
    @Inject
    lateinit var hostUrlInterceptor: HostUrlInterceptor

    private val reason = Data.Builder()
    private val syncErrors = arrayListOf<SyncError>()
    private val syncStats = SyncStats()

    init {
        (appContext as App).appComponent.inject(this)
    }

    override suspend fun doWork(): Result {
        return supervisorScope {

            if (isStopped) return@supervisorScope stopWork("Before initialization")

            Log.d(TAG, "Started Sync")
            if (BuildConfig.DEBUG || loginManager.retrieveUser()?.username?.equals(BuildConfig.DEMO_ACCOUNT, true) == true)
            {
                val host = ApiEnvironments.valueOf(sp.getString(SP_ENVIRONMENT, ApiEnvironments.STAGE.name) ?: ApiEnvironments.STAGE.name)
                hostUrlInterceptor.setHost(host)
            }

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

            suspend fun logUploadError(e: HttpException, it: BeneficiaryLocal, action: UploadAction) {
                val errBody = "${e.response()?.errorBody()?.toString()}"
                Log.d(TAG, "Failed uploading [$action]: ${it.id}: $errBody")

                // Mark conflicts in DB
                val distributionName = assistancesRepository.getNameById(it.distributionId)
                val projectName = projectsRepository.getNameByDistributionId(it.distributionId)
                val beneficiaryName = "${it.givenName} ${it.familyName}"

                val syncError = SyncError(
                    id = it.id,
                    location = "[$action] $projectName → $distributionName → $beneficiaryName",
                    params = "Humansis ID: ${it.beneficiaryId} \nNational ID: ${it.nationalId}",
                    code = e.code(),
                    errorMessage = "${e.code()}: $errBody",
                    beneficiaryId = it.id
                )

                syncErrors.add(syncError)
            }

            val assignedBeneficiaries = beneficiariesRepository.getAssignedBeneficiariesOfflineSuspend()
            val referralChangedBeneficiaries = beneficiariesRepository.getAllReferralChangesOffline()
            syncStats.uploadCandidatesCount = assignedBeneficiaries.count()
            if (assignedBeneficiaries.isNotEmpty() || referralChangedBeneficiaries.isNotEmpty()) {
                sp.edit().putBoolean(SP_SYNC_UPLOAD_INCOMPLETE, true).suspendCommit()
            }

            if (isStopped) return@supervisorScope stopWork("After initialization")

            // Upload distributions of beneficiaries
            assignedBeneficiaries
                .forEach {
                    try {
                        beneficiariesRepository.distribute(it)
                        syncStats.countUploadSuccess()
                    } catch (e: HttpException) {
                        logUploadError(e, it, UploadAction.DISTRIBUTION)
                    }
                    if (isStopped) return@supervisorScope stopWork("Uploading ${it.beneficiaryId}")
                }
            // Upload changes of referral
            referralChangedBeneficiaries
                .forEach {
                    try {
                        beneficiariesRepository.updateBeneficiaryReferralOnline(it)
                    } catch (e: HttpException) {
                        logUploadError(e, it, UploadAction.REFERRAL_UPDATE)
                    }
                    if (isStopped) return@supervisorScope stopWork("Uploading ${it.beneficiaryId}")
                }

            // Download updated data
            if (syncErrors.isEmpty()) {

                val projects = try {
                    projectsRepository.getProjectsOnline(getCurrentCountry(sp))
                } catch (e: Exception) {
                    syncErrors.add(getDownloadError(e, applicationContext.getString(R.string.projects)))
                    emptyList<ProjectLocal>()
                }

                if (isStopped) return@supervisorScope stopWork("Downloading projects")

                val commodities = try {
                    assistancesRepository.getCommoditiesOnline()
                } catch (e: Exception) {
                    syncErrors.add(getDownloadError(e, "Commodities"))
                    emptyList<Commodity>()
                }

                if (isStopped) return@supervisorScope stopWork("Downloading commodities")

                val distributions = try {
                    projects.map {
                        async { assistancesRepository.getDistributionsOnline(it.id, getCurrentCountry(sp), commodities) }
                    }.flatMap {
                        it.await().toList()
                    }
                } catch (e: Exception) {
                    syncErrors.add(getDownloadError(e, applicationContext.getString(R.string.distribution)))
                    emptyList<DistributionLocal>()
                }

                if (isStopped) return@supervisorScope stopWork("Downloading distributions")

                try {
                    distributions.map {
                        async { beneficiariesRepository.getBeneficiariesOnline(it.id) }
                    }.map {
                        it.await()
                    }
                } catch (e: Exception) {
                    syncErrors.add(getDownloadError(e, applicationContext.getString(R.string.beneficiary)))
                    emptyList<BeneficiaryLocal>()
                }
            }

            finishWork()
        }
    }

    private fun getCurrentCountry(sp: SharedPreferences): String {
        return sp.getString(SP_COUNTRY, "") ?: ""
    }

    private suspend fun stopWork(location: String): Result {
        syncErrors.add(
            SyncError(
                location = location,
                params = "",
                code = 0,
                errorMessage = "Sync was stopped by work manager"
            )
        )
        return finishWork()
    }

    private suspend fun finishWork(): Result {
        sp.edit().putString(SP_SYNC_SUMMARY, syncStats.toString()).suspendCommit()
        return if (syncErrors.isEmpty()) {
            sp.setDate(LAST_DOWNLOAD_KEY, Date())
            sp.setDate(LAST_SYNC_FAILED_KEY, null)
            sp.edit().putBoolean(SP_FIRST_COUNTRY_DOWNLOAD, false).suspendCommit()
            sp.edit().putBoolean(SP_SYNC_UPLOAD_INCOMPLETE, false).suspendCommit()
            Log.d(TAG, "Sync finished successfully")
            Result.success()
        } else {
            errorsRepository.insertAll(syncErrors)

            // Erase password to trigger re-authentication
            if (syncErrors.find { it.code == 403 } != null) {
                loginManager.markInvalidPassword()
            }

            Log.d(TAG, "Sync finished with failure")
            sp.setDate(LAST_SYNC_FAILED_KEY, Date())
            Result.failure(reason.putStringArray(ERROR_MESSAGE_KEY, convertErrors(syncErrors)).build())
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

    private fun getDownloadError(e: Exception, resourceName: String): SyncError {
        Log.d(TAG, "Failed downloading ${resourceName}: ${e.message}}")
        Log.e(TAG, e)

        return when (e) {
            is HttpException -> {
                SyncError(
                    location = applicationContext.getString(R.string.download_error).format(resourceName.toLowerCase(Locale.ROOT)),
                    params = applicationContext.getString(R.string.error_server),
                    errorMessage = getErrorMessageByCode(e.code()),
                    code = e.code()
                )
            }
            else -> {
                SyncError(
                    location = applicationContext.getString(R.string.download_error).format(resourceName.toLowerCase(Locale.ROOT)),
                    params = applicationContext.getString(R.string.unknwon_error),
                    errorMessage = e.message ?: "",
                    code = 0
                )
            }
        }
    }

    private enum class UploadAction {
        DISTRIBUTION, REFERRAL_UPDATE
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
                applicationContext.getString(R.string.sync_summary, uploadedSuccessfullyCount, uploadCandidatesCount)
            }
        }
    }

    companion object {
        private val TAG = SyncWorker::class.java.simpleName
        private val MOBILE_MONEY = "Mobile Money"
    }
}
