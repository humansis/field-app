package cz.applifting.humansis.model.api

data class DistributionBeneficiary(
    val id: Int,
    val beneficiary: Beneficiary,
    val distributedAt: String?,
    val currentSmartcardSerialNumber: String?,
    val generalReliefItems: List<Relief>,
    val booklets: List<Booklet>
)