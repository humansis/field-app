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
    tableName = "distributions",
    foreignKeys = [ForeignKey(
        entity = ProjectLocal::class,
        parentColumns = arrayOf("id"),
        childColumns = arrayOf("projectId"),
        onDelete = CASCADE
    )]
)
data class DistributionLocal(
    @PrimaryKey val id: Int,
    val name: String,
    val numberOfBeneficiaries: Int,
    val commodities: List<CommodityLocal>,
    val dateOfDistribution: String?,
    val projectId: Int,
    val target: Target,
    var completed: Boolean,
    val remote: Boolean
) {
    val isQRVoucherDistribution: Boolean
        get() = commodities.any { commodity -> commodity.type == CommodityType.QR_VOUCHER }
    val isSmartcardDistribution: Boolean
        get() = commodities.any { commodity -> commodity.type == CommodityType.SMARTCARD }
}
