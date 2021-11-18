package cz.applifting.humansis.model.api

data class DistributionBeneficiary(
    val id: Int,
    val beneficiary: Beneficiary,
    val distributedAt: String?, // TODO tyka se jenom smartcarty v teto distribuci, prejmenovat na smartcardDistributedAt
    val currentSmartcardSerialNumber: String?,
    val generalReliefItems: List<Relief>,
    val booklets: List<Booklet>
)