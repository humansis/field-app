package cz.applifting.humansis.ui.main.distribute.upload

import android.content.SharedPreferences
import androidx.lifecycle.MutableLiveData
import cz.applifting.humansis.misc.stringLiveData
import cz.applifting.humansis.model.db.BeneficiaryLocal
import cz.applifting.humansis.model.db.DistributionLocal
import cz.applifting.humansis.model.db.SyncError
import cz.applifting.humansis.repositories.BeneficiariesRepository
import cz.applifting.humansis.repositories.DistributionsRepository
import cz.applifting.humansis.repositories.ErrorsRepository
import cz.applifting.humansis.repositories.ProjectsRepository
import cz.applifting.humansis.synchronization.SP_SYNC_SUMMARY
import cz.applifting.humansis.ui.App
import cz.applifting.humansis.ui.BaseViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Created by Petr Kubes <petr.kubes@applifting.cz> on 27, November, 2019
 */
enum class Screen {
    MAIN,
    ERROR_INFO
}

class UploadDialogViewModel @Inject constructor(
    private val errorsRepository: ErrorsRepository,
    private val projectsRepository: ProjectsRepository,
    private val distributionsRepository: DistributionsRepository,
    private val beneficiariesRepository: BeneficiariesRepository,
    sp: SharedPreferences,
    app: App
) : BaseViewModel(app) {

    val currentScreenLD: MutableLiveData<Screen> = MutableLiveData()
    val syncErrorListLD: MutableLiveData<List<SyncError>> = MutableLiveData()
    val syncSummary = sp.stringLiveData(SP_SYNC_SUMMARY, "")

    init {
        currentScreenLD.value = Screen.MAIN

        launch {
            errorsRepository.getAll().collect {
                syncErrorListLD.value = it
            }
        }
    }

    fun changeScreen(screen: Screen) {
        currentScreenLD.value = screen
    }

    suspend fun getRelatedEntities(id: Int): Triple<String?, DistributionLocal?, BeneficiaryLocal?> {
        val beneficiary = beneficiariesRepository.getBeneficiaryOffline(id)
        return Triple(
            beneficiary?.let { projectsRepository.getNameByDistributionId(it.distributionId) },
            beneficiary?.let { distributionsRepository.getById(it.distributionId) },
            beneficiary
        )
    }
}