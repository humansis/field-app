package cz.applifting.humansis.model.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.CASCADE
import androidx.room.PrimaryKey
import cz.applifting.humansis.model.CommodityType
import cz.applifting.humansis.model.Target

/**
 * Created by Petr Kubes <petr.kubes@applifting.cz> on 09, September, 2019
 */
@Entity(
    tableName = "assistances",
    foreignKeys = [ForeignKey(
        entity = ProjectLocal::class,
        parentColumns = arrayOf("id"),
        childColumns = arrayOf("projectId"),
        onDelete = CASCADE
    )]
)
data class AssistanceLocal(
    @PrimaryKey val id: Int,
    val name: String,
    val numberOfBeneficiaries: Int,
    val commodityTypes: List<CommodityType>,
    val dateOfDistribution: String?,
    val dateOfExpiration: String?,
    val projectId: Int,
    val target: Target,
    var completed: Boolean,
    val remote: Boolean,
    val foodLimit: Double?,
    val nonfoodLimit: Double?,
    val cashbackLimit: Double?
) {
    val isQRVoucherDistribution: Boolean
        get() = commodityTypes.any { it == CommodityType.QR_VOUCHER }
    val isSmartcardDistribution: Boolean
        get() = commodityTypes.any { it == CommodityType.SMARTCARD }
}
