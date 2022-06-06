package cz.applifting.humansis.ui.main.distribute.distributions

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
import cz.applifting.humansis.model.ui.DistributionItemWrapper
import cz.applifting.humansis.ui.components.listComponent.ListComponentAdapter
import kotlinx.android.synthetic.main.item_distribution.view.*
import kotlinx.android.synthetic.main.item_project.view.tv_location
import quanti.com.kotlinlog.Log

/**
 * Created by Petr Kubes <petr.kubes@applifting.cz> on 21, August, 2019
 */
class DistributionsAdapter(
    private val onItemClick: (distribution: DistributionItemWrapper) -> Unit
) : ListComponentAdapter<DistributionsAdapter.DistributionViewHolder>() {

    private val distributions: MutableList<DistributionItemWrapper> = mutableListOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DistributionViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(
            R.layout.item_distribution,
            parent,
            false
        ) as CardView
        return DistributionViewHolder(view)
    }

    override fun getItemCount(): Int = distributions.size

    override fun onBindViewHolder(holder: DistributionViewHolder, position: Int) {
        val distribution = distributions[position]
        holder.bind(distribution)
    }

    fun updateDistributions(newDistributions: List<DistributionItemWrapper>) {
        val diffResult = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
                newDistributions[newItemPosition].distribution.id == distributions[oldItemPosition].distribution.id

            override fun getOldListSize(): Int = distributions.size
            override fun getNewListSize(): Int = newDistributions.size
            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
                false
        })

        this.distributions.clear()
        this.distributions.addAll(newDistributions)
        diffResult.dispatchUpdatesTo(this)
    }

    inner class DistributionViewHolder(val layout: CardView) : RecyclerView.ViewHolder(layout) {
        private val tvName: TextView = layout.tv_location
        private val tvDate: TextView = layout.tv_date!!
        private val tvBeneficiariesCnt: TextView = layout.tv_beneficiaries_cnt
        private val ivTarget: ImageView = layout.iv_target
        private val ivStatus: ImageView = layout.iv_status
        private val llComoditiesHolder: LinearLayout = layout.tl_commodities_holder
        private val pbDistributionProgress: ProgressBar = layout.pb_distribution_progress
        val context: Context = layout.context

        fun bind(distributionItemWrapper: DistributionItemWrapper) = with(distributionItemWrapper.distribution) {

            // Set text fields
            tvName.text = if (remote) { context.getString(R.string.remote, name) } else { name }
            tvDate.text = context.getString(R.string.date_of_distribution, dateOfDistribution ?: context.getString(R.string.unknown))
            tvBeneficiariesCnt.text = context.getString(R.string.beneficiaries, numberOfBeneficiaries)
            llComoditiesHolder.removeAllViews()

            commodityTypes.forEach {
                try {
                    val commodityImage = ImageView(context)
                    commodityImage.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT)

                    try {
                        val commodityDrawable = it.drawableResId

                        commodityImage.simpleDrawable(commodityDrawable)
                        llComoditiesHolder.addView(commodityImage)
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
                Log.d(TAG, "Distribution ${distributions[layoutPosition].distribution} clicked")
                if (clickable) onItemClick(distributions[layoutPosition])
            }

            val statusColor = when {
                completed -> R.color.green
                else -> R.color.darkBlue
            }

            ivStatus.tintedDrawable(R.drawable.ic_circle, statusColor)

            pbDistributionProgress.visible(!completed)

            if (numberOfBeneficiaries > 0) {
                pbDistributionProgress.progress = distributionItemWrapper.numberOfReachedBeneficiaries * 100 / numberOfBeneficiaries
            }
        }
    }

    companion object {
        private val TAG = DistributionsAdapter::class.java.simpleName
    }
}