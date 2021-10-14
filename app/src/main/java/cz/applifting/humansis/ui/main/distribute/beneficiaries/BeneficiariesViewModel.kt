package cz.applifting.humansis.ui.main.distribute.beneficiaries

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import cz.applifting.humansis.model.db.BeneficiaryLocal
import cz.applifting.humansis.repositories.BeneficiariesRepository
import cz.applifting.humansis.ui.App
import cz.applifting.humansis.ui.components.listComponent.ListComponentState
import cz.applifting.humansis.ui.main.BaseListViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

/**
 * Created by Vaclav Legat <vaclav.legat@applifting.cz>
 * @since 5. 9. 2019
 */
class BeneficiariesViewModel @Inject constructor(
    private val beneficiariesRepository: BeneficiariesRepository,
    app: App
) : BaseListViewModel(app) {

    enum class Sort {
        DEFAULT,
        AZ,
        ZA
    }

    private val beneficiariesLD = MutableLiveData<List<BeneficiaryLocal>>()
    internal val beneficiariesViewStateLD: MutableLiveData<ListComponentState> = MutableLiveData()
    internal val statsLD: MutableLiveData<Pair<Int, Int>> = MutableLiveData()
    internal val searchResultsLD = MediatorLiveData<List<BeneficiaryLocal>>()
    internal val currentSort = MutableLiveData<Sort>()
    private var searchText: String? = null

    init {
        currentSort.value = Sort.DEFAULT
        searchResultsLD.addSource(beneficiariesLD) { list ->
            setSortedBeneficiaries(list)
        }
    }

    fun init(distributionId: Int) {
        launch {
            showRetrieving(true)

            beneficiariesRepository
                .getBeneficiariesOffline(distributionId)
                .collect { newBeneficiaries ->
                    beneficiariesLD.value = newBeneficiaries
                    statsLD.value = Pair(newBeneficiaries.count { it.distributed }, newBeneficiaries.size)

                    searchText?.let {
                        if (it.isNotEmpty()) {
                            search(it)
                        }
                    }

                    showRetrieving(false)
                }
        }
    }

    /**
     * Filters beneficiaries by provided query.
     */
    internal fun search(input: String) = beneficiariesLD.value?.let {
        searchText = input
        val query = input.normalize()

        if (query.isEmpty()) {
            searchResultsLD.value = it.defaultSort()
            return@let
        }

        setSortedBeneficiaries(it.filter { beneficiary ->
            val familyName = beneficiary.familyName?.normalize() ?: ""
            val givenName = beneficiary.givenName?.normalize() ?: ""
            val beneficiaryId = beneficiary.beneficiaryId.toString()
            val nationalId = beneficiary.nationalId

            val fullName = "$givenName $familyName"
            val fullNameReversed = "$familyName $givenName"
            fullName.contains(query) || fullNameReversed.contains(query) || beneficiaryId.startsWith(query) || nationalId?.startsWith(query) == true
        })
    }

    fun changeSort() {
        currentSort.value = nextSort()
    }

    internal fun setSortedBeneficiaries(list: List<BeneficiaryLocal>?) {
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
     * Sorts currently displayed beneficiaries by family name, undistributed puts first.
     */
    private fun List<BeneficiaryLocal>.defaultSort(): List<BeneficiaryLocal> {
        return this.sortedWith(compareBy({ it.distributed }, { it.givenName }, { it.familyName }))
    }

    /**
     * Sorts currently displayed beneficiaries by family name A to Z
     */
    private fun List<BeneficiaryLocal>.sortAZ(): List<BeneficiaryLocal> {
        return this.sortedWith(compareBy({ it.givenName }, { it.familyName }))
    }

    /**
     * Sorts currently displayed beneficiaries by family name Z to A
     */
    private fun List<BeneficiaryLocal>.sortZA(): List<BeneficiaryLocal> {
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
        return this.lowercase(Locale.getDefault()).trim().replace("\\s+".toRegex(), " ")
    }
}