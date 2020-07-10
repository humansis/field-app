package cz.applifting.humansis.model.api

class DistributeSmartcardRequest (
    val value: Int = 1,
    val distributionId: Int,
    val createdAt: String
)