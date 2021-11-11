package cz.applifting.humansis.model.api

import com.google.gson.annotations.SerializedName

/**
 * Created by Petr Kubes <petr.kubes@applifting.cz> on 19, August, 2019
 */
data class Project(
    val id: Int,
    val name: String?,
    @SerializedName("start_date") val startDate: String?,
    @SerializedName("end_date") val endDate: String?,
    @SerializedName("number_of_households") val numberOfHouseholds: Int?,
    val target: Long?,
    val notes: String?,
    val iso3: String?,
    val donors: List<Any>?,
    val sectors: List<Any>?,
    val archived: Boolean = false,
    val distributions: List<Any>?
)