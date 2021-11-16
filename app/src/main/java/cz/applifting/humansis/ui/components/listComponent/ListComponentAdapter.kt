package cz.applifting.humansis.ui.components.listComponent

import androidx.recyclerview.widget.RecyclerView

/**
 * Created by Petr Kubes <petr.kubes@applifting.cz> on 27, November, 2019
 */
abstract class ListComponentAdapter<T : RecyclerView.ViewHolder?> : RecyclerView.Adapter<T>() {
    var clickable = true
}