package cz.applifting.humansis.model.api

data class AssistanceBeneficiary(
    val id: Int,
    val beneficiary: Beneficiary,
    val distributedAt: String?, // TODO tyka se jenom smartcarty v teto distribuci, prejmenovat na smartcardDistributedAt
    val currentSmartcardSerialNumber: String?, // TODO presunout zpet do objektu beneficiary, jednalo se o nedorozumeni
    val reliefPackages: List<ReliefPackage>,
    val booklets: List<Booklet>
)