package cz.applifting.humansis.ui.main.distribute.projects

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import cz.applifting.humansis.R
import cz.applifting.humansis.model.db.ProjectLocal
import cz.applifting.humansis.ui.components.listComponent.ListComponentAdapter
import kotlinx.android.synthetic.main.item_project.view.*
import quanti.com.kotlinlog.Log

/**
 * Created by Petr Kubes <petr.kubes@applifting.cz> on 14, August, 2019
 */
class ProjectsAdapter(
    private val onItemClick: (project: ProjectLocal) -> Unit
) : ListComponentAdapter<ProjectsAdapter.ProjectViewHolder>() {

    private val projects: MutableList<ProjectLocal> = mutableListOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProjectViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_project, parent, false) as CardView
        return ProjectViewHolder(view)
    }

    override fun getItemCount(): Int = projects.size

    override fun onBindViewHolder(holder: ProjectViewHolder, position: Int) {
        holder.bind(projects[position])
    }

    fun updateProjects(newProjects: List<ProjectLocal>) {
        val diffResult = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean = newProjects[newItemPosition].id == projects[oldItemPosition].id
            override fun getOldListSize(): Int = projects.size
            override fun getNewListSize(): Int = newProjects.size
            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean = false
        })

        this.projects.clear()
        this.projects.addAll(newProjects)
        diffResult.dispatchUpdatesTo(this)
    }

    inner class ProjectViewHolder(val layout: CardView) : RecyclerView.ViewHolder(layout) {
        val context: Context = layout.context
        private val tvName: TextView = layout.tv_name
        private val tvHouseHolds: TextView = layout.tv_households

        fun bind(project: ProjectLocal) {
            tvName.text = project.name
            tvHouseHolds.text = context.getString(R.string.households, project.numberOfHouseholds)
            layout.setOnClickListener {
                Log.d(TAG, "Project $project clicked")
                if (clickable) onItemClick(project)
            }
        }
    }

    companion object {
        private val TAG = ProjectsAdapter::class.java.simpleName
    }
}