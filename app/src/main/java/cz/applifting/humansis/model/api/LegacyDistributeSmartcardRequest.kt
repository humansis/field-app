package cz.applifting.humansis.model.api

// Can be removed after v3.7.0 release
class LegacyDistributeSmartcardRequest(
    val assistanceId: Int,
    val value: Double,
    val createdAt: String,
    val beneficiaryId: Int,
    val balanceBefore: Double?,
    val balanceAfter: Double
)