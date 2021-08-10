package cz.applifting.humansis.model.api

import com.google.gson.annotations.SerializedName
import cz.applifting.humansis.model.CommodityType

/**
 * Created by Petr Kubes <petr.kubes@applifting.cz> on 28, August, 2019
 */
data class Commodity(
    val id: Int,
    @SerializedName("modalityType") val commodityType: CommodityType?,
    val unit: String,
    val value: Double,
    val description: String?
)