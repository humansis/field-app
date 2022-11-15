package cz.applifting.humansis.model.db

import cz.applifting.humansis.model.CommodityType

// TODO reliefPackageId should be made non nullable for v3.9.0 release.
//  Removing "?" is all that needs to be done, there is no need for migrations or anything else.
//  We just need to hope that nobody migrates from v3.6 to v3.9
data class CommodityLocal(
    val reliefPackageId: Int?,
    val type: CommodityType,
    val value: Double,
    val unit: String,
    val notes: String? = null
)