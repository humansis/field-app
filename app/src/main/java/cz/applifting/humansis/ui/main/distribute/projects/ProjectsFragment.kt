package cz.applifting.humansis.ui.main.distribute.projects

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import cz.applifting.humansis.R
import cz.applifting.humansis.ui.BaseFragment
import cz.applifting.humansis.ui.HumansisActivity
import kotlinx.android.synthetic.main.fragment_projects.*
import kotlinx.coroutines.FlowPreview


/**
 * Created by Petr Kubes <petr.kubes@applifting.cz> on 14, August, 2019
 */
@FlowPreview
class ProjectsFragment : BaseFragment() {

    private val viewModel: ProjectsViewModel by viewModels { this.viewModelFactory }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_projects, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as HumansisActivity).supportActionBar?.title = getString(R.string.app_name)
        (activity as HumansisActivity).supportActionBar?.subtitle = getString(R.string.projects)

        val adapter = ProjectsAdapter {
            val action = ProjectsFragmentDirections.chooseProject(it.id, it.name)
            this.findNavController().navigate(action)
        }

        lc_projects.init(adapter)

        viewModel.projectsLD.observe(viewLifecycleOwner, {
            adapter.updateProjects(it)
        })

        viewModel.listStateLD.observe(viewLifecycleOwner, Observer(
            lc_projects::setState
        ))

        sharedViewModel.syncState.observe(viewLifecycleOwner, {
            viewModel.showRefreshing(it.isLoading, isFirstdownload = it.isFirstCountryDownload)
            viewModel.showError(it.lastSyncFail != null && it.isFirstCountryDownload)
        })
    }
}