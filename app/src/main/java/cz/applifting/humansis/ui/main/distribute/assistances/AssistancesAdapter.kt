package cz.applifting.humansis.ui.main.distribute.assistances

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import cz.applifting.humansis.R
import cz.applifting.humansis.extensions.simpleDrawable
import cz.applifting.humansis.extensions.tintedDrawable
import cz.applifting.humansis.extensions.visible
import cz.applifting.humansis.model.Target
import cz.applifting.humansis.model.ui.AssistanceItemWrapper
import cz.applifting.humansis.ui.components.listComponent.ListComponentAdapter
import kotlinx.android.synthetic.main.item_assistance.view.iv_status
import kotlinx.android.synthetic.main.item_assistance.view.iv_target
import kotlinx.android.synthetic.main.item_assistance.view.pb_distribution_progress
import kotlinx.android.synthetic.main.item_assistance.view.tl_commodities_holder
import kotlinx.android.synthetic.main.item_assistance.view.tv_beneficiaries_cnt
import kotlinx.android.synthetic.main.item_assistance.view.tv_date
import kotlinx.android.synthetic.main.item_assistance.view.tv_name
import quanti.com.kotlinlog.Log

/**
 * Created by Petr Kubes <petr.kubes@applifting.cz> on 21, August, 2019
 */
class AssistancesAdapter(
    private val onItemClick: (assistance: AssistanceItemWrapper) -> Unit
) : ListComponentAdapter<AssistancesAdapter.AssistanceViewHolder>() {

    private val assistances: MutableList<AssistanceItemWrapper> = mutableListOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AssistanceViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(
            R.layout.item_assistance,
            parent,
            false
        ) as CardView
        return AssistanceViewHolder(view)
    }

    override fun getItemCount(): Int = assistances.size

    override fun onBindViewHolder(holder: AssistanceViewHolder, position: Int) {
        val assistance = assistances[position]
        holder.bind(assistance)
    }

    fun update(newAssistances: List<AssistanceItemWrapper>) {
        val diffResult = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
                newAssistances[newItemPosition].assistance.id == assistances[oldItemPosition].assistance.id

            override fun getOldListSize(): Int = assistances.size
            override fun getNewListSize(): Int = newAssistances.size
            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
                false
        })

        this.assistances.clear()
        this.assistances.addAll(newAssistances)
        diffResult.dispatchUpdatesTo(this)
    }

    inner class AssistanceViewHolder(val layout: CardView) : RecyclerView.ViewHolder(layout) {
        private val tvName: TextView = layout.tv_name
        private val tvDate: TextView = layout.tv_date!!
        private val tvBeneficiariesCnt: TextView = layout.tv_beneficiaries_cnt
        private val ivTarget: ImageView = layout.iv_target
        private val ivStatus: ImageView = layout.iv_status
        private val llCommoditiesHolder: LinearLayout = layout.tl_commodities_holder
        private val pbDistributionProgress: ProgressBar = layout.pb_distribution_progress
        val context: Context = layout.context

        fun bind(assistanceItemWrapper: AssistanceItemWrapper) = with(assistanceItemWrapper.assistance) {

            // Set text fields
            tvName.text = if (remote) { context.getString(R.string.remote, name) } else { name }
            tvDate.text = context.getString(R.string.date_of_distribution, dateOfDistribution)
            tvBeneficiariesCnt.text = context.getString(R.string.beneficiaries, numberOfBeneficiaries)
            llCommoditiesHolder.removeAllViews()

            commodityTypes.forEach {
                try {
                    val commodityImage = ImageView(context)
                    commodityImage.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT)

                    try {
                        val commodityDrawable = it.drawableResId

                        commodityImage.simpleDrawable(commodityDrawable)
                        llCommoditiesHolder.addView(commodityImage)
                    } catch (e: IllegalArgumentException) {
                        // Todo add unknown commodity image
                    }
                } catch (e: IllegalArgumentException) {
                    // do not show, unknown type
                }
            }

            // Set target
            val targetImage =
                if (target == Target.INDIVIDUAL) {
                    ContextCompat.getDrawable(context, R.drawable.ic_person_black_24dp)
                } else {
                    ContextCompat.getDrawable(context, R.drawable.ic_home_black_24dp)
                }
            ivTarget.setImageDrawable(targetImage)

            layout.setOnClickListener {
                Log.d(TAG, "Assistance ${assistances[layoutPosition].assistance} clicked")
                if (clickable) onItemClick(assistances[layoutPosition])
            }

            val statusColor = when {
                completed -> R.color.green
                else -> R.color.darkBlue
            }

            ivStatus.tintedDrawable(R.drawable.ic_circle, statusColor)

            pbDistributionProgress.visible(!completed)

            if (numberOfBeneficiaries > 0) {
                pbDistributionProgress.progress = assistanceItemWrapper.numberOfReachedBeneficiaries * 100 / numberOfBeneficiaries
            }
        }
    }

    companion object {
        private val TAG = AssistancesAdapter::class.java.simpleName
    }
}