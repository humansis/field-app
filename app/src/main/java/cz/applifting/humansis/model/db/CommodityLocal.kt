package cz.applifting.humansis.model.db

import cz.applifting.humansis.model.CommodityType

data class CommodityLocal(
    val reliefPackageId: Int,
    val type: CommodityType,
    val value: Double,
    val unit: String,
    val notes: String? = null
)