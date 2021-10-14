package cz.applifting.humansis.model.api

data class SmartcardDeposit(
    val id: Int,
    val smartcard: String,
    val value: Long,
    val currency: String,
    val distributed: Boolean,
    val dateOfDistribution: String
)
