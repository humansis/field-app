package cz.applifting.humansis.model.api

import com.google.gson.annotations.SerializedName
import cz.applifting.humansis.model.ReferralType

/**
 * Created by Vaclav Legat <vaclav.legat@applifting.cz>
 * @since 5. 9. 2019
 */

//todo finish model
data class Beneficiary(
    @SerializedName("id") val id: Int,
    @SerializedName("localFamilyName") val familyName: String?,
    @SerializedName("localGivenName") val givenName: String?,
    @SerializedName("nationalIdCards") val nationalIdCards: List<NationalIdCard>?,
    @SerializedName("referralType") val referralType: ReferralType?,
    @SerializedName("referralComment") val referralComment: String
    // TODO upravit aby swagger nic krom těchhle parametrů nevracel
    // TODO upravit swagger aby se beneficiary nezískával pomocí filteru ale pomocí id v path, zároveň pak vrací jen jednoho bnf místo listu
)