package cz.applifting.humansis.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import cz.applifting.humansis.R
import kotlinx.android.synthetic.main.component_reached_beneficiaries.view.*

class ReachedBeneficiariesComponent @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : LinearLayout(context, attrs, defStyleAttr) {

    init {
        LayoutInflater.from(context).inflate(R.layout.component_reached_beneficiaries, this, true)
        orientation = VERTICAL
        background = ContextCompat.getDrawable(context, R.drawable.background_dark_component)
    }

    internal fun setStats(reachedBeneficiaries: Int, totalBeneficiaries: Int) {
        tv_beneficiaries_reached.text = context.getString(R.string.beneficiaries_reached, reachedBeneficiaries, totalBeneficiaries)

        if (totalBeneficiaries != 0) {
            pb_beneficiaries_reached.progress = reachedBeneficiaries * 100 / totalBeneficiaries
        }
    }
}
