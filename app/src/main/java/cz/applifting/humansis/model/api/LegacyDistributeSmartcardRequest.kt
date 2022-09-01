package cz.applifting.humansis.model.api

// Can be removed in v 3.8.0
class LegacyDistributeSmartcardRequest(
    val assistanceId: Int,
    val value: Double,
    val createdAt: String,
    val beneficiaryId: Int,
    val balanceBefore: Double?,
    val balanceAfter: Double
)