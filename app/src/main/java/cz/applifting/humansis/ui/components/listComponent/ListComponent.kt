package cz.applifting.humansis.ui.components.listComponent

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat.getColor
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cz.applifting.humansis.R
import cz.applifting.humansis.extensions.visible
import kotlinx.android.synthetic.main.component_list.view.*

/**
 * Created by Petr Kubes <petr.kubes@applifting.cz> on 09, September, 2019
 */
class ListComponent(context: Context, attrs: AttributeSet) : ConstraintLayout(context, attrs) {

    private lateinit var adapter: ListComponentAdapter<*>

    fun init(viewAdapter: ListComponentAdapter<*>) {
        View.inflate(context, R.layout.component_list, this)

        adapter = viewAdapter
        val viewManager = LinearLayoutManager(context)

        rv_list.apply {
            setHasFixedSize(true)
            layoutManager = viewManager
            adapter = viewAdapter
            addItemDecoration(VerticalMarginItemDecoration())
        }

        swrl_swipe_to_refresh.isEnabled = false
        swrl_swipe_to_refresh.setColorSchemeResources(
            R.color.colorAccent,
            R.color.colorAccentDark,
            R.color.darkBlue
        )
    }

    fun setState(state: ListComponentState) {
        rv_list.visible(true)
        pb_loading.visible(state.isRetrieving)
        swrl_swipe_to_refresh.isRefreshing = state.isRefreshing

        val itemAnimator = DefaultItemAnimator()
        itemAnimator.addDuration = 300
        rv_list.itemAnimator = itemAnimator

        if (state.text != null && !state.isRetrieving && !state.isRetrieving) {
            tv_info.text = state.text
            tv_info.visible(true)
            rv_list.visible(false)
        } else {
            tv_info.visible(false)
            rv_list.visible(true)
        }

        if (state.isError) {
            rv_list.visible(false)
            tv_info.setTextColor(getColor(context, R.color.red))
            iv_error.visible(true)
        } else {
            iv_error.visible(false)
            tv_info.setTextColor(getColor(context, R.color.black))
        }

        adapter.clickable = !state.isRefreshing
    }

    fun setOnRefreshListener(listener: () -> Unit) {
        swrl_swipe_to_refresh.isEnabled = true
        swrl_swipe_to_refresh.setOnRefreshListener(listener)
    }

    fun scrollToTop() {
        rv_list.layoutManager?.scrollToPosition(0)
    }

    inner class VerticalMarginItemDecoration : RecyclerView.ItemDecoration() {
        override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {

            val margin = context.resources.getDimension(R.dimen.list_items_vertical_margin).toInt()

            with(outRect) {
                if (parent.getChildAdapterPosition(view) == 0) {
                    top = margin
                }
                left = margin
                right = margin
                bottom = margin
            }
        }
    }

}