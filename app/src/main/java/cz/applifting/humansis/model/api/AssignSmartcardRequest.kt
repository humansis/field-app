package cz.applifting.humansis.model.api

data class AssignSmartcardRequest(
    val serialNumber: String,
    val beneficiaryId: Int,
    val createdAt: String
)