package cz.applifting.humansis.model.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import cz.applifting.humansis.extensions.equalsIgnoreEmpty
import cz.applifting.humansis.model.ReferralType

/**
 * Created by Vaclav Legat <vaclav.legat@applifting.cz>
 * @since 5. 9. 2019
 */

@Entity(
    tableName = "beneficiaries",
    foreignKeys = [ForeignKey(
        entity = DistributionLocal::class,
        parentColumns = ["id"],
        childColumns = ["distributionId"],
        onDelete = ForeignKey.CASCADE
    )]
)
// this is flattened object from API, original: {id, distributionId, {beneficiaryId, givenName, ...}}
// each "beneficiary" (beneficiaryId, givenName, ...) can be in multiple distributions
data class BeneficiaryLocal(
    @PrimaryKey val id: Int, // unique combination of beneficiaryId and distributionId
    val beneficiaryId: Int, // id of actual beneficiary (can be non-unique)
    val givenName: String?,
    val familyName: String?,
    val distributionId: Int,
    val distributed: Boolean,
    val distributedAt: String?,
    val vulnerabilities: List<String>,
    val reliefIDs: List<Int>,
    val qrBooklets: List<String>?,
    val smartcard: String?,
    val newSmartcard: String?,
    val edited: Boolean,
    val commodities: List<CommodityLocal>?,
    val nationalId: String?,
    val originalReferralType: ReferralType?,
    val originalReferralNote: String?,
    val referralType: ReferralType? = null,
    val referralNote: String? = null,
    val originalBalance: Double? = null,
    val balance: Double? = null
) {
    private val isReferralTypeChanged
        get() = originalReferralType != referralType

    private val isReferralNoteChanged
        get() = !originalReferralNote.equalsIgnoreEmpty(originalReferralNote)

    val isReferralChanged
        get() = isReferralTypeChanged || isReferralNoteChanged

    val hasReferral
    get() = referralType != null || !referralNote.isNullOrEmpty()
}