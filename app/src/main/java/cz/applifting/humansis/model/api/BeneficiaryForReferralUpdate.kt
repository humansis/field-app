package cz.applifting.humansis.model.api

import com.google.gson.annotations.SerializedName
import cz.applifting.humansis.model.ReferralType

data class BeneficiaryForReferralUpdate(
    @SerializedName("id") val id: Int,
    @SerializedName("referral_comment") val referralNote: String?,
    @SerializedName("referral_type") val referralType: ReferralType?
)