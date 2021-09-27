package cz.applifting.humansis.model.api

import com.google.gson.annotations.SerializedName

data class AssistanceBeneficiary(
    @SerializedName("id") val id: Int,
    @SerializedName("beneficiaryId") val beneficiaryId: Int,
    @SerializedName("generalReliefItemIds") val reliefIds: List<Int>,
    @SerializedName("smartcardDepositIds") val smartcardDepositIds: List<Int>,
    @SerializedName("bookletIds") val bookletIds: List<Int>
    // TODO dát ve swaggeru pryč transactionIds, nepotřebuju
)