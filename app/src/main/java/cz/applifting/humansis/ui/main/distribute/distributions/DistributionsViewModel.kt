package cz.applifting.humansis.ui.main.distribute.distributions

import androidx.lifecycle.MutableLiveData
import cz.applifting.humansis.misc.stringToDate
import cz.applifting.humansis.model.ui.DistributionItemWrapper
import cz.applifting.humansis.repositories.BeneficiariesRepository
import cz.applifting.humansis.repositories.DistributionsRepository
import cz.applifting.humansis.ui.App
import cz.applifting.humansis.ui.main.BaseListViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Created by Petr Kubes <petr.kubes@applifting.cz> on 14, August, 2019
 */
class DistributionsViewModel @Inject constructor(
    private val distributionsRepository: DistributionsRepository,
    private val beneficiariesRepository: BeneficiariesRepository,
    app: App
) : BaseListViewModel(app) {

    val distributionsLD: MutableLiveData<List<DistributionItemWrapper>> = MutableLiveData()

    private var projectId: Int? = null

    fun init(projectId: Int) {
        if (this.projectId != null) {
            return
        }
        this.projectId = projectId

        getDistributions(projectId)
    }

    fun getDistributions(projectId: Int) {
        launch {
            showRetrieving(true)

            distributionsRepository
                .getDistributionsOffline(projectId)
                .map { newDistributions ->
                    newDistributions.map {
                        val reachedBeneficiaries = beneficiariesRepository.countReachedBeneficiariesOffline(it.id)
                        if (reachedBeneficiaries == it.numberOfBeneficiaries) {
                            it.completed = true
                        }
                        DistributionItemWrapper(it, reachedBeneficiaries)
                    }
                }
                .collect { list ->
                    distributionsLD.value = list.defaultSort()
                    showRetrieving(false)
                }
        }
    }

    /**
     * Sorts currently displayed distributions by date, completed are put last.
     */
    private fun List<DistributionItemWrapper>.defaultSort(): List<DistributionItemWrapper> {
        return this.sortedWith(compareBy<DistributionItemWrapper> { it.distribution.completed }
                .thenByDescending { stringToDate(it.distribution.dateOfDistribution) }
        )
    }
}