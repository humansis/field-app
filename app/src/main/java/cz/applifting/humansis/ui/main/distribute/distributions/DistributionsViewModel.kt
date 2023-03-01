package cz.applifting.humansis.ui.main.distribute.distributions

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import cz.applifting.humansis.extensions.toDate
import cz.applifting.humansis.model.ui.DistributionItemWrapper
import cz.applifting.humansis.repositories.BeneficiariesRepository
import cz.applifting.humansis.repositories.DistributionsRepository
import cz.applifting.humansis.ui.App
import cz.applifting.humansis.ui.components.Sort
import cz.applifting.humansis.ui.main.BaseListViewModel
import java.util.Locale
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
    internal val searchResultsLD = MediatorLiveData<List<DistributionItemWrapper>>()
    internal val currentSort = MutableLiveData<Sort>()
    private var searchText: String? = null

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
     * Filters distributions by provided query.
     */
    internal fun search(input: String) = distributionsLD.value?.let {
        searchText = input
        val query = input.normalize()

        if (query.isEmpty()) {
            searchResultsLD.value = it.defaultSort()
            return@let
        }

        setSortedDistributions(it.filter { wrapper ->
            val distributionName = wrapper.distribution.name.normalize()
            val distributionId = wrapper.distribution.id.toString()

            distributionName.contains(query) || distributionId.startsWith(query)
        })
    }

    fun changeSort() {
        currentSort.value = nextSort()
    }

    internal fun setSortedDistributions(list: List<DistributionItemWrapper>?) {
        searchResultsLD.value = list?.run {
            when (currentSort.value) {
                Sort.DEFAULT -> defaultSort()
                Sort.AZ -> sortAZ()
                Sort.ZA -> sortZA()
                else -> defaultSort()
            }
        } ?: emptyList()
    }

    /**
     * Sorts currently displayed distributions by date, completed are put last.
     */
    private fun List<DistributionItemWrapper>.defaultSort(): List<DistributionItemWrapper> {
        return this.sortedWith(compareBy<DistributionItemWrapper> { it.distribution.completed }
                .thenByDescending { it.distribution.dateOfDistribution?.toDate() }
        )
    }

    // TODO rename distribuce na assistence

    // TODO syncnout preklady

    /**
     * Sorts currently displayed distributions by name A to Z
     */
    private fun List<DistributionItemWrapper>.sortAZ(): List<DistributionItemWrapper> {
        return this.sortedBy { it.distribution.name }
    }

    /**
     * Sorts currently displayed distributions by name Z to A
     */
    private fun List<DistributionItemWrapper>.sortZA(): List<DistributionItemWrapper> {
        return sortAZ().reversed()
    }

    private fun nextSort(): Sort {
        return when (currentSort.value) {
            Sort.DEFAULT -> Sort.AZ
            Sort.AZ -> Sort.ZA
            Sort.ZA -> Sort.DEFAULT
            else -> Sort.DEFAULT
        }
    }

    private fun String.normalize(): String {
        return this.toLowerCase(Locale.getDefault()).trim().replace("\\s+".toRegex(), " ")
    }
}