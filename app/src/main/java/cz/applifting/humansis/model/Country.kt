package cz.applifting.humansis.model

import android.content.Context
import cz.applifting.humansis.R

data class Country(
    val iso3: String = "",
    var name: String = ""
) {
    override fun toString(): String {
        return name
    }
}