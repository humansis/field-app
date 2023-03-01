package cz.applifting.humansis.model.api

import com.google.gson.annotations.SerializedName
import cz.applifting.humansis.model.Target

/**
 * Created by Petr Kubes <petr.kubes@applifting.cz> on 21, August, 2019
 */
data class Assistance(
    val id: Int,
    val name: String,
    @SerializedName("date_distribution")val dateDistribution: String?, // TODO zmenit na non nullable
    @SerializedName("date_expiration")val dateExpiration: String?,
    val type: Target,
    val commodities: List<Commodity>,
    @SerializedName("beneficiaries_count") val numberOfBeneficiaries: Int,
    val foodLimit: Double?,
    val nonfoodLimit: Double?,
    val cashbackLimit: Double?,
    val remoteDistributionAllowed: Boolean,
    val completed: Boolean,
    val validated: Boolean,
    val archived: Boolean
)