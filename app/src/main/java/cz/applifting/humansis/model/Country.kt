package cz.applifting.humansis.model

import android.content.Context
import cz.applifting.humansis.R

data class Country(
    val iso3: String = "",
    var context: Context? = null
) {
    override fun toString(): String {
        return when(iso3){
            "SYR" -> {
                context?.getString(R.string.SYR) ?: iso3
            }
            "KHM" -> {
                context?.getString(R.string.KHM) ?: iso3
            }
            "UKR" -> {
                context?.getString(R.string.UKR) ?: iso3
            }
            else -> {
                iso3
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if(other == null)
            return false
        if(!(other is Country))
            return false
        return iso3 == other.iso3
    }
}