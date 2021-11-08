package cz.applifting.humansis.misc

import android.content.Context
import cz.applifting.humansis.R
import cz.applifting.humansis.model.db.CategoryType
import java.util.*

object SmartcardUtilities {

    fun getExpirationDateAsString(expirationDate: Date?, context: Context): String {
        return if (expirationDate != null) {
            context.getString(
                R.string.expiration_date_formatted,
                DateUtil.dateToString(expirationDate, context)
            )
        } else {
            String()
        }
    }

    fun getLimitsAsText(mapOfLimits: Map<Int, Double>, currencyCode: String, context: Context): String {
        var limits = String()
        mapOfLimits.map {
            limits += context.getString(R.string.product_type_limit_formatted, CategoryType.getById(it.key).backendName, "${it.value} $currencyCode")
        }
        return limits
    }
}