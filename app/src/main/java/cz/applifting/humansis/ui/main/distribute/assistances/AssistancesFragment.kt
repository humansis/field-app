package cz.applifting.humansis.ui.main.distribute.assistances

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import cz.applifting.humansis.R
import cz.applifting.humansis.extensions.hideSoftKeyboard
import cz.applifting.humansis.extensions.visible
import cz.applifting.humansis.ui.BaseFragment
import cz.applifting.humansis.ui.HumansisActivity
import kotlinx.android.synthetic.main.component_search.et_search
import kotlinx.android.synthetic.main.fragment_assistances.cmp_search_assistance
import kotlinx.android.synthetic.main.fragment_assistances.lc_assistances

/**
 * Created by Petr Kubes <petr.kubes@applifting.cz> on 14, August, 2019
 */
class AssistancesFragment : BaseFragment() {

    private val args: AssistancesFragmentArgs by navArgs()

    private val viewModel: AssistancesViewModel by viewModels {
        viewModelFactory
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_assistances, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        cmp_search_assistance.clearSearch()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        (activity as HumansisActivity).supportActionBar?.title = args.projectName
        (activity as HumansisActivity).supportActionBar?.subtitle = getString(R.string.assistances)

        val viewAdapter = AssistancesAdapter {
            it.assistance.also { assistance ->
                val action = AssistancesFragmentDirections.actionAssistancesFragmentToBeneficiariesFragment(
                    assistance.id,
                    assistance.name,
                    args.projectName,
                    isQRVoucherDistribution = assistance.isQRVoucherDistribution,
                    isSmartcardDistribution = assistance.isSmartcardDistribution,
                    isRemoteDistribution = assistance.remote
                )
                this.findNavController().navigate(action)
            }
        }

        lc_assistances.init(viewAdapter)

        viewModel.listStateLD.observe(viewLifecycleOwner) {
            lc_assistances.setState(it)
            cmp_search_assistance.visible(!it.isRetrieving)
        }

        viewModel.searchResultsLD.observe(viewLifecycleOwner) {
            viewAdapter.update(it)
        }

        cmp_search_assistance.onTextChanged(viewModel::search)
        cmp_search_assistance.onSort {
            viewModel.changeSort()
            lc_assistances.scrollToTop()
        }

        viewModel.currentSort.observe(viewLifecycleOwner) {
            viewModel.setSortedAssistances(viewModel.searchResultsLD.value)
            cmp_search_assistance.changeSortIcon(it)
        }

        sharedViewModel.syncState.observe(viewLifecycleOwner) {
            viewModel.showRefreshing(it.isLoading)
            if (!it.isLoading) {
                viewModel.getAssistances(args.projectId)
            }
        }

        viewModel.getAssistances(args.projectId)

        findNavController().addOnDestinationChangedListener { _, _, _ -> et_search?.hideSoftKeyboard() }
    }
}