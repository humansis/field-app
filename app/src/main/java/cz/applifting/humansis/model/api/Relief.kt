package cz.applifting.humansis.model.api

import com.google.gson.annotations.SerializedName

/**
 * Created by Petr Kubes <petr.kubes@applifting.cz> on 29, September, 2019
 */
data class Relief(
    val id: Int,
    @SerializedName("dateOfDistribution") val dateOfDistribution: String?
)

data class ReliefsApiEntity(
    val totalCount: Int,
    val data: List<Relief>
)
