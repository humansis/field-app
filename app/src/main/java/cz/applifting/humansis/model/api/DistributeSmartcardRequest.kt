package cz.applifting.humansis.model.api

class DistributeSmartcardRequest(
    val reliefPackageId: Int,
    val value: Double,
    val createdAt: String,
    val beneficiaryId: Int,
    val balanceBefore: Double?,
    val balanceAfter: Double
)