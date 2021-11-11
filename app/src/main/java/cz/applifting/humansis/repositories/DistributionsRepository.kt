package cz.applifting.humansis.repositories

import android.content.Context
import cz.applifting.humansis.api.HumansisService
import cz.applifting.humansis.db.DbProvider
import cz.applifting.humansis.model.CommodityType
import cz.applifting.humansis.model.api.Commodity
import cz.applifting.humansis.model.db.CommodityLocal
import cz.applifting.humansis.model.db.DistributionLocal
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Created by Petr Kubes <petr.kubes@applifting.cz> on 09, September, 2019
 */
@Singleton
class DistributionsRepository @Inject constructor(val service: HumansisService, val dbProvider: DbProvider, val context: Context) {

    suspend fun getDistributionsOnline(projectId: Int): List<DistributionLocal> {
        val result = service
            .getDistributions(projectId)
            .filter { // Skip all distributions distributing mobile money, as it is necessary to have a desktop for their distribution
                it.commodities.fold(true, { acc, commodity ->
                    commodity.modalityType?.name != CommodityType.MOBILE_MONEY && acc
                })
            }
            .filter { it.validated && !it.archived && !it.completed }
            .map {
                DistributionLocal(
                    it.id,
                    it.name,
                    it.numberOfBeneficiaries,
                    parseCommodities(it.commodities),
                    it.dateDistribution,
                    it.dateExpiration,
                    projectId,
                    it.type,
                    it.completed,
                    it.remoteDistributionAllowed,
                    it.foodLimit,
                    it.nonfoodLimit,
                    it.cashbackLimit
                )
            }

        dbProvider.get().distributionsDao().replaceByProject(projectId, result)

        return result
    }

    fun getDistributionsOffline(projectId: Int): Flow<List<DistributionLocal>> {
        return dbProvider.get().distributionsDao().getByProject(projectId)
    }

    suspend fun getDistributionsOfflineSuspend(projectId: Int): List<DistributionLocal> {
        return dbProvider.get().distributionsDao().getByProjectSuspend(projectId)
    }

    fun getAllDistributions(): Flow<List<DistributionLocal>> {
        return dbProvider.get().distributionsDao().getAll()
    }

    suspend fun getUncompletedDistributionsSuspend(projectId: Int): List<DistributionLocal> {
        return dbProvider.get().distributionsDao().findUncompletedDistributionsSuspend(projectId)
    }

    suspend fun getById(distributionId: Int) =
        dbProvider.get().distributionsDao().getById(distributionId)

    suspend fun getNameById(distributionId: Int): String? {
        return getById(distributionId)?.name
    }

    private fun parseCommodities(commodities: List<Commodity>): List<CommodityLocal> {
        return commodities.map {
            val commodityName: String = it.modalityType?.name?.name ?: CommodityType.UNKNOWN.name
            CommodityLocal(CommodityType.valueOf(commodityName), it.value, it.unit)
        }
    }
}