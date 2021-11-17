package cz.applifting.humansis.model.api

import cz.applifting.humansis.model.ReferralType

/**
 * Created by Vaclav Legat <vaclav.legat@applifting.cz>
 * @since 5. 9. 2019
 */

// todo finish model
data class Beneficiary(
    val id: Int,
    val localGivenName: String?,
    val localFamilyName: String?,
    val referralType: ReferralType?,
    val referralComment: String?,
    val nationalCardId: String?
)