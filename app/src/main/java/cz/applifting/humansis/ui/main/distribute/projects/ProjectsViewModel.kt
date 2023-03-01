package cz.applifting.humansis.ui.main.distribute.projects

import androidx.lifecycle.MutableLiveData
import cz.applifting.humansis.model.db.ProjectLocal
import cz.applifting.humansis.repositories.AssistancesRepository
import cz.applifting.humansis.repositories.ProjectsRepository
import cz.applifting.humansis.ui.App
import cz.applifting.humansis.ui.main.BaseListViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Created by Petr Kubes <petr.kubes@applifting.cz> on 14, August, 2019
 */
@FlowPreview
class ProjectsViewModel @Inject constructor(
    private val projectsRepository: ProjectsRepository,
    private val assistancesRepository: AssistancesRepository,
    app: App
) : BaseListViewModel(app) {

    val projectsLD: MutableLiveData<List<ProjectLocal>> = MutableLiveData()

    init {
        launch {
            showRetrieving(true)

            assistancesRepository
                .getAllAssistances()
                .flatMapMerge { assistances ->
                    projectsRepository
                        .getProjectsOffline()
                        .map {
                            Pair(assistances, it)
                        }
                }
                .map { (assistances, projects) ->
                    projects.filter { project ->
                        assistances.any { it.projectId == project.id && !it.completed }
                    }
                }
                .collect {
                    projectsLD.value = it
                    showRetrieving(false)
                }
        }
    }
}