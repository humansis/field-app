package cz.applifting.humansis.model.api

class DistributeSmartcardRequest (
    val distributionId: Int,
    val value: Double = 1.0,
    val createdAt: String,
    val beneficiaryId: Int
)