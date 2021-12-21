package cz.applifting.humansis.model.api

import com.google.gson.annotations.SerializedName

/**
 * Created by Petr Kubes <petr.kubes@applifting.cz> on 19, August, 2019
 */
data class Project(
    val id: Int,
    val name: String?,
    @SerializedName("number_of_households") val numberOfHouseholds: Int?
)