package cz.applifting.humansis.ui.main.distribute.distributions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import cz.applifting.humansis.R
import cz.applifting.humansis.extensions.visible
import cz.applifting.humansis.ui.BaseFragment
import cz.applifting.humansis.ui.HumansisActivity
import kotlinx.android.synthetic.main.fragment_distributions.cmp_search_assistance
import kotlinx.android.synthetic.main.fragment_distributions.lc_distributions

/**
 * Created by Petr Kubes <petr.kubes@applifting.cz> on 14, August, 2019
 */
class DistributionsFragment : BaseFragment() {

    private val args: DistributionsFragmentArgs by navArgs()

    private val viewModel: DistributionsViewModel by viewModels {
        viewModelFactory
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_distributions, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        cmp_search_assistance.clearSearch()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        (activity as HumansisActivity).supportActionBar?.title = args.projectName
        (activity as HumansisActivity).supportActionBar?.subtitle = getString(R.string.distributions)

        val viewAdapter = DistributionsAdapter {
            it.distribution.also { distribution ->
                val action = DistributionsFragmentDirections.actionDistributionsFragmentToBeneficiariesFragment(
                    distribution.id,
                    distribution.name,
                    args.projectName,
                    isQRVoucherDistribution = distribution.isQRVoucherDistribution,
                    isSmartcardDistribution = distribution.isSmartcardDistribution,
                    isRemoteDistribution = distribution.remote
                )
                this.findNavController().navigate(action)
            }
        }

        lc_distributions.init(viewAdapter)

        viewModel.distributionsLD.observe(viewLifecycleOwner) {
            viewAdapter.updateDistributions(it)
        }

        viewModel.listStateLD.observe(viewLifecycleOwner) {
            lc_distributions.setState(it)
            cmp_search_assistance.visible(!it.isRetrieving)
        }

        viewModel.searchResultsLD.observe(viewLifecycleOwner) { assistances ->
            viewAdapter.updateDistributions(assistances)
        }

        cmp_search_assistance.onTextChanged(viewModel::search)
        cmp_search_assistance.onSort {
            viewModel.changeSort()
            lc_distributions.scrollToTop()
        }

        viewModel.currentSort.observe(viewLifecycleOwner) {
            viewModel.setSortedDistributions(viewModel.searchResultsLD.value)
            cmp_search_assistance.changeSortIcon(it)
        }

        sharedViewModel.syncState.observe(viewLifecycleOwner) {
            viewModel.showRefreshing(it.isLoading)
            if (!it.isLoading) {
                viewModel.getDistributions(args.projectId)
            }
        }

        viewModel.init(args.projectId)
    }
}