package cz.applifting.humansis.model.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import cz.applifting.humansis.R
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
    val remote: Boolean,
    val dateExpiration: String?,
    val foodLimit: Double?,
    val nonfoodLimit: Double?,
    val cashbackLimit: Double?,
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

    fun getLimits(): Map<Int, Double> {
        val limits = mutableMapOf<Int, Double>()
        this.foodLimit?.let {
            limits[CategoryType.FOOD.typeId] = it
        }
        this.nonfoodLimit?.let {
            limits[CategoryType.NONFOOD.typeId] = it
        }
        this.cashbackLimit?.let {
            limits[CategoryType.CASHBACK.typeId] = it
        }
        return limits
    }
}

enum class CategoryType(
    val typeId: Int,
    val backendName: String?,
    val stringRes: Int?
) {
    ALL(0, null, null),
    FOOD(1, "Food", R.string.food),
    NONFOOD(2, "Non-Food", R.string.nonfood),
    CASHBACK(3, "Cashback", R.string.cashback),
    OTHER(4, null, null);

    companion object {
        fun getByName(backendName: String): CategoryType {
            return values().find { it.backendName == backendName } ?: OTHER
        }

        fun getById(id: Int): CategoryType {
            return values().find { it.typeId == id } ?: OTHER
        }
    }
}