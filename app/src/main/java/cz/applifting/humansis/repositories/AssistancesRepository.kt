package cz.applifting.humansis.repositories

import android.content.Context
import cz.applifting.humansis.api.HumansisService
import cz.applifting.humansis.db.DbProvider
import cz.applifting.humansis.model.CommodityType
import cz.applifting.humansis.model.api.Commodity
import cz.applifting.humansis.model.db.CommodityLocal
import cz.applifting.humansis.model.db.DistributionLocal
import kotlinx.coroutines.flow.Flow
import quanti.com.kotlinlog.Log
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Created by Petr Kubes <petr.kubes@applifting.cz> on 09, September, 2019
 */
@Singleton
class AssistancesRepository @Inject constructor(val service: HumansisService, val dbProvider: DbProvider, val context: Context) {

    suspend fun getCommoditiesOnline(): List<Commodity> {
        return service.getCommodities(
            notModalityTypes = listOf(MODALITY_MOBILE_MONEY)
        )
    }

    suspend fun getDistributionsOnline(projectId: Int, currentCountry: String, commodities: List<Commodity>): List<DistributionLocal> {

        val result = service
            .getAssistances(
                projectId = projectId,
                country = currentCountry,
                assistanceType = TYPE_DISTRIBUTION, // TODO edit after activities are added
                completed = false.int,
                notModalityTypes = listOf(MODALITY_MOBILE_MONEY)
            )
            .map {
                DistributionLocal(
                    id= it.id,
                    name = it.name,
                    dateOfDistribution = it.dateDistribution,
                    projectId = it.projectId,
                    target = it.target,
                    commodities = getCommodities(it.commodityIds, commodities),
                    numberOfBeneficiaries = it.numberOfBeneficiaries,
                    completed = it.completed
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

    private fun getCommodities(commodityIds: List<Int>, commodities: List<Commodity>): List<CommodityLocal> {
        return commodityIds.map { id ->
            commodities.find { it.id == id }?.let {
                val commodityName = it.commodityType?.name ?: onUnknownCommodity(it)
                CommodityLocal(CommodityType.valueOf(commodityName), it.value, it.unit)
            } ?: CommodityLocal(CommodityType.UNKNOWN, 0.0, "")
        }
    }

    private fun onUnknownCommodity(commodity: Commodity): String {
        Log.d(TAG, "Unknown commodity $commodity")
        return CommodityType.UNKNOWN.name
    }

    val Boolean?.int
        get() = if (this != null && this) 1 else 0

    companion object {
        private val TAG = AssistancesRepository::class.java.simpleName
        const val MODALITY_MOBILE_MONEY = "Mobile Money"
        const val TYPE_DISTRIBUTION = "distribution"
        const val TYPE_ACTIVITY = "activity"
    }
}