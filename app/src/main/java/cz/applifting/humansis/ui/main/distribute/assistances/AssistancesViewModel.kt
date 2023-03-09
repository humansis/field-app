package cz.applifting.humansis.ui.main.distribute.assistances

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import cz.applifting.humansis.extensions.toDate
import cz.applifting.humansis.model.ui.AssistanceItemWrapper
import cz.applifting.humansis.repositories.BeneficiariesRepository
import cz.applifting.humansis.repositories.AssistancesRepository
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
class AssistancesViewModel @Inject constructor(
    private val assistancesRepository: AssistancesRepository,
    private val beneficiariesRepository: BeneficiariesRepository,
    app: App
) : BaseListViewModel(app) {

    private val assistancesLD: MutableLiveData<List<AssistanceItemWrapper>> = MutableLiveData()
    internal val searchResultsLD = MediatorLiveData<List<AssistanceItemWrapper>>()
    internal val currentSort = MutableLiveData<Sort>()
    private var searchText: String? = null

    init {
        currentSort.value = Sort.DEFAULT
        searchResultsLD.addSource(assistancesLD) { list ->
            setSortedAssistances(list)
        }
    }

    fun loadAssistances(projectId: Int) {
        launch {
            showRetrieving(true)

            assistancesRepository
                .getAssistancesOffline(projectId)
                .map { newAssistances ->
                    newAssistances.map {
                        val reachedBeneficiaries = beneficiariesRepository.countReachedBeneficiariesOffline(it.id)
                        if (reachedBeneficiaries == it.numberOfBeneficiaries) {
                            it.completed = true
                        }
                        AssistanceItemWrapper(it, reachedBeneficiaries)
                    }
                }
                .collect { list ->
                    assistancesLD.value = list.defaultSort()
                    showRetrieving(false)
                }
        }
    }

    /**
     * Filters assistances by provided query.
     */
    internal fun search(input: String) = assistancesLD.value?.let {
        searchText = input
        val query = input.normalize()

        if (query.isEmpty()) {
            searchResultsLD.value = it.defaultSort()
            return@let
        }

        setSortedAssistances(it.filter { wrapper ->
            val assistanceName = wrapper.assistance.name.normalize()
            val assistanceId = wrapper.assistance.id.toString()

            assistanceName.contains(query) || assistanceId.startsWith(query)
        })
    }

    fun changeSort() {
        currentSort.value = nextSort()
    }

    internal fun setSortedAssistances(list: List<AssistanceItemWrapper>?) {
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
     * Sorts currently displayed assistances by date, completed are put last.
     */
    private fun List<AssistanceItemWrapper>.defaultSort(): List<AssistanceItemWrapper> {
        return this.sortedWith(compareBy<AssistanceItemWrapper> { it.assistance.completed }
                .thenByDescending { it.assistance.dateOfDistribution?.toDate() }
        )
    }

    /**
     * Sorts currently displayed assistances by name A to Z
     */
    private fun List<AssistanceItemWrapper>.sortAZ(): List<AssistanceItemWrapper> {
        return this.sortedBy { it.assistance.name }
    }

    /**
     * Sorts currently displayed assistances by name Z to A
     */
    private fun List<AssistanceItemWrapper>.sortZA(): List<AssistanceItemWrapper> {
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