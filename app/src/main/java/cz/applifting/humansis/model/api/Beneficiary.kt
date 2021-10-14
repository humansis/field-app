package cz.applifting.humansis.model.api

import com.google.gson.annotations.SerializedName
import cz.applifting.humansis.model.ReferralType

/**
 * Created by Vaclav Legat <vaclav.legat@applifting.cz>
 * @since 5. 9. 2019
 */

data class Beneficiary(
    @SerializedName("id") val id: Int,
    @SerializedName("localFamilyName") val familyName: String?,
    @SerializedName("localGivenName") val givenName: String?,
    @SerializedName("nationalIdCards") val nationalIdCards: List<Long>?,
    @SerializedName("referralType") val referralType: ReferralType?,
    @SerializedName("referralComment") val referralComment: String
)