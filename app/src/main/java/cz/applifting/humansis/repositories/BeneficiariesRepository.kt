package cz.applifting.humansis.repositories

import android.content.Context
import cz.applifting.humansis.api.HumansisService
import cz.applifting.humansis.db.DbProvider
import cz.applifting.humansis.misc.DateUtil.convertTimeForApiRequestBody
import cz.applifting.humansis.model.CommodityType
import cz.applifting.humansis.model.api.AssignBookletRequest
import cz.applifting.humansis.model.api.AssignSmartcardRequest
import cz.applifting.humansis.model.api.BeneficiaryForReferralUpdate
import cz.applifting.humansis.model.api.Booklet
import cz.applifting.humansis.model.api.DeactivateSmartcardRequest
import cz.applifting.humansis.model.api.DistributeSmartcardRequest
import cz.applifting.humansis.model.api.DistributedReliefPackages
import cz.applifting.humansis.model.api.LegacyDistributeSmartcardRequest
import cz.applifting.humansis.model.api.NationalCardIdType
import cz.applifting.humansis.model.api.ReliefPackage
import cz.applifting.humansis.model.db.BeneficiaryLocal
import cz.applifting.humansis.model.db.CommodityLocal
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

/**
 * Created by Petr Kubes <petr.kubes@applifting.cz> on 09, September, 2019
 */
@Singleton
class BeneficiariesRepository @Inject constructor(
    val service: HumansisService,
    val dbProvider: DbProvider,
    val context: Context
) {

    suspend fun getBeneficiariesOnline(assistanceId: Int): List<BeneficiaryLocal> {

        val distribution = dbProvider.get().distributionsDao().getById(assistanceId)

        val result = service
            .getDistributionBeneficiaries(assistanceId).data
            .map {
                BeneficiaryLocal(
                    id = it.id,
                    beneficiaryId = it.beneficiary.id,
                    givenName = it.beneficiary.localGivenName,
                    familyName = it.beneficiary.localFamilyName,
                    assistanceId = assistanceId,
                    distributed = areReliefPackagesDistributed(it.reliefPackages) || areBookletsDistributed(
                        it.booklets
                    ) || it.distributedAt != null,
                    distributedAt = it.distributedAt,
                    reliefIDs = parseGeneralReliefPackages(it.reliefPackages),
                    qrBooklets = parseQRBooklets(it.booklets),
                    smartcard = it.currentSmartcardSerialNumber?.toUpperCase(Locale.US),
                    newSmartcard = null,
                    edited = false,
                    commodities = parseCommodities(it.booklets, it.reliefPackages),
                    remote = distribution?.remote ?: false,
                    dateExpiration = distribution?.dateOfExpiration,
                    foodLimit = distribution?.foodLimit,
                    nonfoodLimit = distribution?.nonfoodLimit,
                    cashbackLimit = distribution?.cashbackLimit,
                    nationalIds = it.beneficiary.nationalCardIds.filter { id -> id.type != NationalCardIdType.NONE },
                    originalReferralType = it.beneficiary.referralType,
                    originalReferralNote = it.beneficiary.referralComment,
                    referralType = it.beneficiary.referralType,
                    referralNote = it.beneficiary.referralComment
                )
            }

        dbProvider.get().beneficiariesDao().deleteByDistribution(assistanceId)
        dbProvider.get().beneficiariesDao().insertAll(result)

        return result
    }

    suspend fun updateBeneficiaryReferralOnline(beneficiary: BeneficiaryLocal) {
        service.updateBeneficiaryReferral(
            beneficiary.beneficiaryId, BeneficiaryForReferralUpdate(
                id = beneficiary.beneficiaryId,
                referralType = beneficiary.referralType,
                referralNote = beneficiary.referralNote
            )
        )
        // prevent upload again if the sync fails
        dbProvider.get().beneficiariesDao()
            .updateReferralOfMultiple(beneficiary.beneficiaryId, null, null)
    }

    fun arePendingChanges(): Flow<List<BeneficiaryLocal>> {
        return dbProvider.get().beneficiariesDao().arePendingChanges()
    }

    fun getAllBeneficiariesOffline(): Flow<List<BeneficiaryLocal>> {
        return dbProvider.get().beneficiariesDao().getAllBeneficiaries()
    }

    fun getBeneficiariesOffline(assistanceId: Int): Flow<List<BeneficiaryLocal>> {
        return dbProvider.get().beneficiariesDao().getByDistribution(assistanceId)
    }

    suspend fun getBeneficiariesOfflineSuspend(assistanceId: Int): List<BeneficiaryLocal> {
        return dbProvider.get().beneficiariesDao().getByDistributionSuspend(assistanceId)
    }

    suspend fun getAssignedBeneficiariesOfflineSuspend(): List<BeneficiaryLocal> {
        return dbProvider.get().beneficiariesDao().getAssignedBeneficiariesSuspend()
    }

    suspend fun getBeneficiaryOffline(beneficiaryId: Int): BeneficiaryLocal? {
        return dbProvider.get().beneficiariesDao().findById(beneficiaryId)
    }

    fun getBeneficiaryOfflineFlow(beneficiaryId: Int): Flow<BeneficiaryLocal?> {
        return dbProvider.get().beneficiariesDao().findByIdFlow(beneficiaryId)
    }

    suspend fun updateBeneficiaryOffline(beneficiary: BeneficiaryLocal) {
        return dbProvider.get().beneficiariesDao().update(beneficiary)
    }

    suspend fun updateReferralOfMultiple(beneficiary: BeneficiaryLocal) {
        dbProvider.get().beneficiariesDao().updateReferralOfMultiple(
            beneficiary.beneficiaryId,
            beneficiary.referralType,
            beneficiary.referralNote
        )
    }

    suspend fun isAssignedInOtherDistribution(beneficiary: BeneficiaryLocal): Boolean {
        return dbProvider.get().beneficiariesDao()
            .countDuplicateAssignedBeneficiaries(beneficiary.beneficiaryId) > 1
    }

    suspend fun getAllReferralChangesOffline(): List<BeneficiaryLocal> {
        return dbProvider.get().beneficiariesDao().getAllReferralChanges()
    }

    suspend fun countReachedBeneficiariesOffline(assistanceId: Int): Int {
        return dbProvider.get().beneficiariesDao().countReachedBeneficiaries(assistanceId)
    }

    suspend fun distribute(beneficiaryLocal: BeneficiaryLocal) {
        if (beneficiaryLocal.reliefIDs.isNotEmpty()) {
            setDistributedRelief(
                beneficiaryLocal.reliefIDs,
                beneficiaryLocal.distributedAt ?: convertTimeForApiRequestBody(Date())
            )
        }

        if (beneficiaryLocal.qrBooklets?.isNotEmpty() == true) {
            assignBooklet(
                beneficiaryLocal.qrBooklets.first(),
                beneficiaryLocal.beneficiaryId,
                beneficiaryLocal.assistanceId
            )
        }

        if (beneficiaryLocal.newSmartcard != null) {
            val time = beneficiaryLocal.distributedAt ?: return
            if (beneficiaryLocal.newSmartcard != beneficiaryLocal.smartcard) {
                assignSmartcard(beneficiaryLocal.newSmartcard, beneficiaryLocal.beneficiaryId, time)
                beneficiaryLocal.smartcard?.let {
                    deactivateSmartcard(beneficiaryLocal.smartcard, time)
                }
            }

            val lastSmartCardDistribution = beneficiaryLocal.commodities
                ?.findLast { it.type == CommodityType.SMARTCARD }

            val value = lastSmartCardDistribution?.value ?: 1.0

            lastSmartCardDistribution?.reliefPackageId?.let { reliefPackageId ->
                distributeSmartcard(
                    beneficiaryLocal.newSmartcard,
                    DistributeSmartcardRequest(
                        reliefPackageId,
                        value,
                        time,
                        beneficiaryLocal.beneficiaryId,
                        beneficiaryLocal.originalBalance,
                        beneficiaryLocal.balance ?: 1.0
                    )
                )
            } ?: run {
                legacyDistributeSmartcard(
                    beneficiaryLocal.newSmartcard,
                    LegacyDistributeSmartcardRequest(
                        beneficiaryLocal.assistanceId,
                        value,
                        time,
                        beneficiaryLocal.beneficiaryId,
                        beneficiaryLocal.originalBalance,
                        beneficiaryLocal.balance ?: 1.0
                    )
                )
            }
        }

        updateBeneficiaryOffline(beneficiaryLocal.copy(edited = false))
    }

    suspend fun checkBookletAssignedLocally(bookletId: String): Boolean {
        val booklets = dbProvider.get().beneficiariesDao().getAllBooklets()

        booklets?.forEach {
            if (it.contains(bookletId)) {
                return true
            }
        }

        return false
    }

    private suspend fun setDistributedRelief(ids: List<Int>, distributedAt: String) {
        service.setReliefPackagesDistributed(
            ids.map { DistributedReliefPackages(it, distributedAt) }.toList()
        )
    }

    private suspend fun assignBooklet(code: String, beneficiaryId: Int, assistanceId: Int) {
        service.assignBooklet(beneficiaryId, assistanceId, AssignBookletRequest(code))
    }

    private suspend fun assignSmartcard(code: String, beneficiaryId: Int, date: String) {
        service.assignSmartcard(AssignSmartcardRequest(code, beneficiaryId, date))
    }

    private suspend fun deactivateSmartcard(code: String, date: String) {
        service.deactivateSmartcard(code, DeactivateSmartcardRequest(createdAt = date))
    }

    // Can be removed after v3.7.0 release
    private suspend fun legacyDistributeSmartcard(
        code: String,
        distributeSmartcardRequest: LegacyDistributeSmartcardRequest
    ) {
        service.legacyDistributeSmartcard(code, distributeSmartcardRequest)
    }

    private suspend fun distributeSmartcard(
        code: String,
        distributeSmartcardRequest: DistributeSmartcardRequest
    ) {
        service.distributeSmartcard(code, distributeSmartcardRequest)
    }

    private fun parseGeneralReliefPackages(reliefPackages: List<ReliefPackage>): List<Int> {
        return reliefPackages
            .filterGeneralReliefs()
            .map { it.id }
    }

    private fun parseQRBooklets(booklets: List<Booklet>): List<String> {
        return booklets.map { it.code }
    }

    private fun parseCommodities(
        booklets: List<Booklet>,
        reliefPackages: List<ReliefPackage>
    ): List<CommodityLocal> {

        if (booklets.isNotEmpty()) {
            return booklets.map { booklet ->
                val bookletValue = booklet.voucherValues.sum().toDouble()
                CommodityLocal(0, CommodityType.QR_VOUCHER, bookletValue, booklet.currency)
            }
        }

        return reliefPackages.map {
            CommodityLocal(
                it.id,
                it.modalityType,
                it.amountToDistribute,
                it.unit,
                it.notes // TODO after resolving merge conflict and migrations
            )
        }
    }

    private fun areReliefPackagesDistributed(reliefPackages: List<ReliefPackage>): Boolean {
        if (reliefPackages.isEmpty()) {
            return false
        }

        reliefPackages.forEach {
            if (it.amountDistributed != it.amountToDistribute) {
                return false
            }
        }

        return true
    }

    private fun areBookletsDistributed(booklets: List<Booklet>): Boolean {
        if (booklets.isEmpty()) {
            return false
        }

        booklets.forEach {
            if (it.status != 1) {
                return false
            }
        }

        return true
    }

    private fun List<ReliefPackage>.filterGeneralReliefs(): List<ReliefPackage> {
        val nonGeneralTypes = setOf(CommodityType.SMARTCARD, CommodityType.QR_VOUCHER)
        return this.filterNot { nonGeneralTypes.contains(it.modalityType) }
    }
}