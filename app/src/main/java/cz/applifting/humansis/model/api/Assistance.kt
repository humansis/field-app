package cz.applifting.humansis.model.api

import cz.applifting.humansis.model.Target

/**
 * Created by Petr Kubes <petr.kubes@applifting.cz> on 21, August, 2019
 */
data class Assistance (
    val id : Int,
    val name : String,
    val dateDistribution : String,
    val projectId : Int,
    val target : Target,
    val type: String,
    val locationId : Int,
    val commodityIds : List<Int>,
    val numberOfBeneficiaries: Int,
    val description: String,
    val validated : Boolean,
    val completed : Boolean

)