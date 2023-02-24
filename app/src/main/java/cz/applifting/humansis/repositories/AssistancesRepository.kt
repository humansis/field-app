package cz.applifting.humansis.repositories

import android.content.Context
import cz.applifting.humansis.api.HumansisService
import cz.applifting.humansis.db.DbProvider
import cz.applifting.humansis.model.CommodityType
import cz.applifting.humansis.model.api.Commodity
import cz.applifting.humansis.model.db.AssistanceLocal
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Created by Petr Kubes <petr.kubes@applifting.cz> on 09, September, 2019
 */
@Singleton
class AssistancesRepository @Inject constructor(
    val service: HumansisService,
    val dbProvider: DbProvider,
    val context: Context
) {

    suspend fun getAssistancesOnline(projectId: Int): List<AssistanceLocal> {
        val result = service
            .getAssistances(projectId)
            .filter { // Skip all assistances distributing mobile money, as it is necessary to have a desktop for their distribution
                it.commodities.fold(true) { acc, commodity ->
                    commodity.modalityType?.name != CommodityType.MOBILE_MONEY &&
                        commodity.modalityType?.name != CommodityType.ACTIVITY_ITEM &&
                        commodity.modalityType?.name != CommodityType.BUSINESS_GRANT &&
                        acc
                }
            }
            .filter { it.validated && !it.archived && !it.completed }
            .map {
                AssistanceLocal(
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

        dbProvider.get().assistancesDao().replaceByProject(projectId, result)

        return result
    }

    fun getAssistancesOffline(projectId: Int): Flow<List<AssistanceLocal>> {
        return dbProvider.get().assistancesDao().getByProject(projectId)
    }

    fun getAllAssistances(): Flow<List<AssistanceLocal>> {
        return dbProvider.get().assistancesDao().getAll()
    }

    suspend fun getById(assistanceId: Int) =
        dbProvider.get().assistancesDao().getById(assistanceId)

    suspend fun getNameById(assistanceId: Int): String? {
        return getById(assistanceId)?.name
    }

    private fun parseCommodities(commodities: List<Commodity>): List<CommodityType> {
        return commodities.map {
            CommodityType.valueOf(it.modalityType?.name?.name ?: CommodityType.UNKNOWN.name)
        }
    }
}