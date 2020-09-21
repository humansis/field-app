package cz.applifting.humansis.model.api

import com.google.gson.annotations.SerializedName
import cz.applifting.humansis.model.Target

/**
 * Created by Petr Kubes <petr.kubes@applifting.cz> on 21, August, 2019
 */
data class Distribution (
    val id : Int,
    val name : String,
    @SerializedName("updated_on") val updatedOn : String?,
    @SerializedName("date_distribution") val dateDistribution : String?,
    val location : Any?,
    val project : Project,
    @SerializedName("selection_criteria") val selection_criteria : List<SelectionCriteria>?,
    val archived : Boolean,
    val validated : Boolean,
    val type : Target,
    val commodities : List<Commodity>,
    @SerializedName("beneficiaries_count") val beneficiariesCount: Int,
    val completed : Boolean
)