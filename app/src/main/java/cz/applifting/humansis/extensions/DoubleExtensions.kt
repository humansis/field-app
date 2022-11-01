package cz.applifting.humansis.extensions

import android.content.Context
import cz.applifting.humansis.R

fun Double.getCommodityValueText(context: Context, unit: String): String {
    return if ((this % 1) == 0.0) {
        context.getString(R.string.commodity_value, this.toInt(), unit)
    } else {
        // This needs to be updated if Denars or Madagascar Ariaries are used in the future
        context.getString(R.string.commodity_value_decimal, this, unit)
    }
}