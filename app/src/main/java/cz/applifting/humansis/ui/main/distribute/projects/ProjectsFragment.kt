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
import kotlinx.android.synthetic.main.fragment_projects.*

/**
 * Created by Petr Kubes <petr.kubes@applifting.cz> on 14, August, 2019
 */
class ProjectsFragment : BaseFragment() {

    private val viewModel: ProjectsViewModel by viewModels { this.viewModelFactory }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_projects, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
//        (activity as MainFragment).supportActionBar?.title = getString(R.string.app_name)
//        (activity as MainFragment).supportActionBar?.subtitle = getString(R.string.projects)

        val adapter = ProjectsAdapter {
            val action = ProjectsFragmentDirections.chooseProject(it.id, it.name)
            this.findNavController().navigate(action)
        }

        lc_projects.init(adapter)
        lc_projects.setOnRefreshListener {  viewModel.loadProjects(true) }

        viewModel.projectsLD.observe(viewLifecycleOwner, Observer {
            adapter.updateProjects(it)
        })

        viewModel.listStateLD.observe(viewLifecycleOwner, Observer(lc_projects::setState))

        sharedViewModel.downloadingLD.observe(viewLifecycleOwner, Observer {
            if (it) {
                viewModel.showRefreshing()
            } else {
                viewModel.loadProjects()
            }
        })

        if (sharedViewModel.downloadingLD.value == false) {
            viewModel.loadProjects()
        }
    }
}