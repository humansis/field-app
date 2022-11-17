package cz.applifting.humansis.ui.main.distribute.beneficiaries

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import cz.applifting.humansis.R
import cz.applifting.humansis.extensions.getCommodityValueText
import cz.applifting.humansis.extensions.simpleDrawable
import cz.applifting.humansis.extensions.tintedDrawable
import cz.applifting.humansis.extensions.visible
import cz.applifting.humansis.model.api.NationalCardId
import cz.applifting.humansis.model.db.BeneficiaryLocal
import cz.applifting.humansis.ui.components.listComponent.ListComponentAdapter
import kotlinx.android.synthetic.main.item_beneficiary.view.*
import quanti.com.kotlinlog.Log

/**
 * Created by Vaclav Legat <vaclav.legat@applifting.cz>
 * @since 5. 9. 2019
 */

class BeneficiariesAdapter(
    val onItemClick: (beneficiary: BeneficiaryLocal) -> Unit
) : ListComponentAdapter<BeneficiariesAdapter.BeneficiaryViewHolder>() {

    private val beneficiaries = mutableListOf<BeneficiaryLocal>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BeneficiaryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(
            R.layout.item_beneficiary,
            parent,
            false
        )
        return BeneficiaryViewHolder(view)
    }

    override fun onBindViewHolder(holder: BeneficiaryViewHolder, position: Int) {
        val distributionBeneficiary = beneficiaries[position]
        holder.bind(distributionBeneficiary)
    }

    override fun getItemCount(): Int = beneficiaries.size

    internal fun update(newBeneficiaries: List<BeneficiaryLocal>) {
        val diffResult = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
                newBeneficiaries[newItemPosition].id == beneficiaries[oldItemPosition].id

            override fun getOldListSize(): Int = beneficiaries.size
            override fun getNewListSize(): Int = newBeneficiaries.size
            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
                beneficiaries[oldItemPosition] == newBeneficiaries[newItemPosition]
        })

        beneficiaries.clear()
        beneficiaries.addAll(newBeneficiaries)

        diffResult.dispatchUpdatesTo(this)
    }

    inner class BeneficiaryViewHolder(val view: View) : RecyclerView.ViewHolder(view) {

        private val tvNationalId: TextView = view.tv_id
        private val tvHumansisId: TextView = view.tv_info
        private val tvName: TextView = view.tv_location
        private val ivDistributionState: ImageView = view.iv_distribution_state
        private val tlCommoditiesHolder: TableLayout = view.tl_commodities_holder
        private val ivOffline: ImageView = view.iv_offline
        val context: Context = view.context

        fun bind(beneficiaryLocal: BeneficiaryLocal) {

            tvHumansisId.text = view.context.getString(R.string.humansis_id_formatted, beneficiaryLocal.beneficiaryId)
            tvNationalId.visible(beneficiaryLocal.nationalIds.isNotEmpty())
            tvNationalId.text = constructNationalIdText(beneficiaryLocal.nationalIds)

            tvName.text = view.context.getString(
                R.string.beneficiary_name,
                beneficiaryLocal.givenName,
                beneficiaryLocal.familyName
            )

            val color = if (beneficiaryLocal.distributed) R.color.green else R.color.darkBlue
            ivDistributionState.tintedDrawable(R.drawable.ic_circle, color)
            tlCommoditiesHolder.removeAllViews()

            /*
             There are a few possible states:
                1. The item is not distributed - do not show any icon
                2. The item is distributed:
                    2.1) it's a QR voucher
                        2.1.1) It's offline -  show just an icon, as we do not know the value
                        2.1.2) Online - show icon with booklet value
                    2.2) it's a normal commodity - show commodity and value
             */

            beneficiaryLocal.commodities.forEach { commodity ->
                val row = TableRow(context)

                val commodityImage = ImageView(context)
                commodityImage.simpleDrawable(commodity.type.drawableResId)
                commodityImage.layoutParams = TableRow.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT)

                val txtValue = TextView(context)
                txtValue.text = commodity.value.getCommodityValueText(context, commodity.unit)

                row.addView(commodityImage)
                row.addView(txtValue)

                if (beneficiaryLocal.distributed) {
                    val distributedImage = ImageView(context)
                    distributedImage.simpleDrawable(R.drawable.ic_baseline_check_circle_24)
                    distributedImage.layoutParams = TableRow.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    row.addView(distributedImage)
                }
                tlCommoditiesHolder.addView(row)
            }

            ivOffline.visible(beneficiaryLocal.edited)
            view.setOnClickListener {
                Log.d(TAG, "Beneficiary $beneficiaryLocal clicked")
                if (clickable) onItemClick(beneficiaryLocal)
            }
        }

        private fun constructNationalIdText(nationalIds: List<NationalCardId>): String {
            return nationalIds.joinToString("\n") { nationalCardId ->
                "${context.getString(nationalCardId.type.stringResource)}: ${nationalCardId.number}"
            }
        }
    }

    companion object {
        private val TAG = BeneficiariesAdapter::class.java.simpleName
    }
}