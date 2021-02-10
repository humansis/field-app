package cz.applifting.humansis.model.db

import cz.applifting.humansis.model.CommodityType

data class CommodityLocal(val type: CommodityType, val value: Double, val unit: String)