package cz.applifting.humansis.model.api

import cz.applifting.humansis.model.Target

/**
 * Created by Petr Kubes <petr.kubes@applifting.cz> on 21, August, 2019
 */
data class Distribution (
    val id : Int,
    val name : String,
    val dateDistribution : String?,
    val dateExpiration : String?,
    val archived : Boolean,
    val validated : Boolean,
    val type : Target,
    val commodities : List<Commodity>,
    val numberOfBeneficiaries: Int,
    val completed : Boolean,
    val remoteDistributionAllowed: Boolean,
    val foodLimit: Double?,
    val nonfoodLimit: Double?,
    val cashbackLimit: Double?
    )