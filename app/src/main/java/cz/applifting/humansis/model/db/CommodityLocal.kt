package cz.applifting.humansis.model.db

import cz.applifting.humansis.model.CommodityType

// reliefPackageId can be made non nullable after v3.7.0 release
data class CommodityLocal(
    val reliefPackageId: Int?,
    val type: CommodityType,
    val value: Double,
    val unit: String,
    val notes: String? = null
)