package cz.applifting.humansis.ui.main.distribute.upload

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import cz.applifting.humansis.R
import cz.applifting.humansis.model.db.SyncError
import kotlinx.android.synthetic.main.item_error.view.*

/**
 * Created by Petr Kubes <petr.kubes@applifting.cz> on 27, November, 2019
 */
class ErrorListAdapter(
    val onItemClick: (SyncError) -> Unit
): RecyclerView.Adapter<ErrorListAdapter.ViewHolder>() {

    private val syncErrors: MutableList<SyncError> = mutableListOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_error, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return syncErrors.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(syncErrors[position])
    }

    fun update(list: List<SyncError>) {
        syncErrors.clear()
        syncErrors.addAll(list)
        notifyDataSetChanged()
    }

    inner class ViewHolder(val view: View): RecyclerView.ViewHolder(view) {
        private val tvLocation: TextView = view.tv_location
        private val tvInfo: TextView = view.tv_info
        private val tvError: TextView = view.tv_error

        fun bind(syncError: SyncError) {
            tvLocation.text = syncError.location
            tvInfo.text = syncError.params
            tvError.text = syncError.errorMessage
            view.setOnClickListener {
                onItemClick(syncError)
            }
        }
    }

}