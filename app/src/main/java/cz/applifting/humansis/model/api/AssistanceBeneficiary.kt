package cz.applifting.humansis.model.api

import com.google.gson.annotations.SerializedName

data class AssistanceBeneficiary(
    @SerializedName("id") val id: Int,
    @SerializedName("beneficiaryId") val beneficiaryId: Int,
    @SerializedName("generalReliefItemIds") val reliefIds: List<Int>,
    @SerializedName("lastSmartcardDepositId") val lastSmartcardDepositId: Int?,
    @SerializedName("bookletIds") val bookletIds: List<Int>
)

data class AssistanceBeneficiariesEntity(
    val totalCount: Int,
    val data: List<AssistanceBeneficiary>
)