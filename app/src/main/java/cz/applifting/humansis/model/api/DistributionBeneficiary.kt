package cz.applifting.humansis.model.api

import com.google.gson.annotations.SerializedName

data class DistributionBeneficiary(
    @SerializedName("id") val id: Int,
    @SerializedName("beneficiary") val beneficiary: Beneficiary,
    @SerializedName("general_reliefs") val reliefs: List<Relief>,
    @SerializedName("booklets") val booklets: List<Booklet>,
    @SerializedName("smartcard") val smartcard: String?,
    @SerializedName("smartcard_distributed") val smartcardDistributed: Boolean?,
    @SerializedName("smartcard_distributed_at") val smartcardDistributedAt: String?
)