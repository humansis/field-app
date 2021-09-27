package cz.applifting.humansis.model.api

import com.google.gson.annotations.SerializedName

/**
 * Created by Petr Kubes <petr.kubes@applifting.cz> on 29, September, 2019
 */
data class Relief(
    val id: Int,
    @SerializedName("dateOfDistribution") val dateOfDistribution: String?
    // TODO dat pryc note a distributed (za predpokladu ze pokud neni distrubuted tak date bude null)
)
