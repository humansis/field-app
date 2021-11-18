package cz.applifting.humansis.repositories

import android.content.Context
import cz.applifting.humansis.api.HumansisService
import cz.applifting.humansis.db.DbProvider
import cz.applifting.humansis.model.CommodityType
import cz.applifting.humansis.model.api.*
import cz.applifting.humansis.model.db.BeneficiaryLocal
import cz.applifting.humansis.model.db.CommodityLocal
import kotlinx.coroutines.flow.Flow
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Created by Petr Kubes <petr.kubes@applifting.cz> on 09, September, 2019
 */
@Singleton
class BeneficiariesRepository @Inject constructor(val service: HumansisService, val dbProvider: DbProvider, val context: Context) {

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
                    distributed = isReliefDistributed(it.generalReliefItems) || isBookletDistributed(it.booklets) || it.currentSmartcardSerialNumber != null,
                    distributedAt = it.distributedAt,
                    reliefIDs = parseReliefs(it.generalReliefItems),
                    qrBooklets = parseQRBooklets(it.booklets),
                    smartcard = it.currentSmartcardSerialNumber?.toUpperCase(Locale.US),
                    newSmartcard = null,
                    edited = false,
                    commodities = parseCommodities(it.booklets, distribution?.commodities),
                    remote = distribution?.remote ?: false,
                    dateExpiration = distribution?.dateOfExpiration,
                    foodLimit = distribution?.foodLimit,
                    nonfoodLimit = distribution?.nonfoodLimit,
                    cashbackLimit = distribution?.cashbackLimit,
                    nationalId = it.beneficiary.nationalCardId,
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
        service.updateBeneficiaryReferral(beneficiary.beneficiaryId, BeneficiaryForReferralUpdate(
            id = beneficiary.beneficiaryId,
            referralType = beneficiary.referralType,
            referralNote = beneficiary.referralNote
        ))
        // prevent upload again if the sync fails
        dbProvider.get().beneficiariesDao().updateReferralOfMultiple(beneficiary.beneficiaryId, null, null)
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
        dbProvider.get().beneficiariesDao().updateReferralOfMultiple(beneficiary.beneficiaryId, beneficiary.referralType, beneficiary.referralNote)
    }

    suspend fun isAssignedInOtherDistribution(beneficiary: BeneficiaryLocal): Boolean {
        return dbProvider.get().beneficiariesDao().countDuplicateAssignedBeneficiaries(beneficiary.beneficiaryId) > 1
    }

    suspend fun getAllReferralChangesOffline(): List<BeneficiaryLocal> {
        return dbProvider.get().beneficiariesDao().getAllReferralChanges()
    }

    suspend fun countReachedBeneficiariesOffline(assistanceId: Int): Int {
        return dbProvider.get().beneficiariesDao().countReachedBeneficiaries(assistanceId)
    }

    suspend fun distribute(beneficiaryLocal: BeneficiaryLocal) {
        if (beneficiaryLocal.reliefIDs.isNotEmpty()) {
            setDistributedRelief(beneficiaryLocal.reliefIDs)
        }

        if (beneficiaryLocal.qrBooklets?.isNotEmpty() == true) {
            assignBooklet(beneficiaryLocal.qrBooklets.first(), beneficiaryLocal.beneficiaryId, beneficiaryLocal.assistanceId)
        }

        if (beneficiaryLocal.newSmartcard != null) {
            val time = beneficiaryLocal.distributedAt ?: return
            if (beneficiaryLocal.newSmartcard != beneficiaryLocal.smartcard) {
                assignSmartcard(beneficiaryLocal.newSmartcard, beneficiaryLocal.beneficiaryId, time)
                beneficiaryLocal.smartcard?.let {
                    deactivateSmartcard(beneficiaryLocal.smartcard, time)
                }
            }

            val value = beneficiaryLocal.commodities
                ?.findLast { it.type == CommodityType.SMARTCARD }
                ?.value ?: 1.0

            distributeSmartcard(
                beneficiaryLocal.newSmartcard,
                beneficiaryLocal.assistanceId,
                value,
                time,
                beneficiaryLocal.beneficiaryId,
                beneficiaryLocal.originalBalance,
                beneficiaryLocal.balance ?: 1.0
            )
        }

        updateBeneficiaryOffline(beneficiaryLocal.copy(edited = false))
    }

    suspend fun checkBoookletAssignedLocally(bookletId: String): Boolean {
        val booklets = dbProvider.get().beneficiariesDao().getAllBooklets()

        booklets?.forEach {
            if (it.contains(bookletId)) {
                return true
            }
        }

        return false
    }

    private suspend fun setDistributedRelief(ids: List<Int>) {
        service.setDistributedRelief(DistributedReliefRequest(ids))
    }

    private suspend fun assignBooklet(code: String, beneficiaryId: Int, assistanceId: Int) {
        service.assignBooklet(beneficiaryId, assistanceId, AssingBookletRequest(code))
    }

    private suspend fun assignSmartcard(code: String, beneficiaryId: Int, date: String) {
        service.assignSmartcard(AssignSmartcardRequest(code, beneficiaryId, date))
    }

    private suspend fun deactivateSmartcard(code: String, date: String) {
        service.deactivateSmartcard(code, DeactivateSmartcardRequest(createdAt = date))
    }

    private suspend fun distributeSmartcard(code: String, assistanceId: Int, value: Double, date: String, beneficiaryId: Int, balanceBefore: Double?, balanceAfter: Double) {
        service.distributeSmartcard(code, DistributeSmartcardRequest(
            assistanceId = assistanceId,
            value = value,
            createdAt = date,
            beneficiaryId = beneficiaryId,
            balanceBefore = balanceBefore,
            balanceAfter = balanceAfter
        ))
    }

    private fun parseReliefs(reliefs: List<Relief>): List<Int> {
        return reliefs.map { it.id }
    }

    private fun parseQRBooklets(booklets: List<Booklet>): List<String> {
        return booklets.map { it.code }
    }

    private fun parseCommodities(booklets: List<Booklet>, commodities: List<CommodityLocal>?): List<CommodityLocal> {

        if (booklets.isNotEmpty()) {
            return booklets.map { booklet ->
                val bookletValue = booklet.vouchers.sumBy { it.value }.toDouble()
                CommodityLocal(CommodityType.QR_VOUCHER, bookletValue, booklet.currency)
            }
        }

        return commodities ?: mutableListOf()
    }

    private fun isReliefDistributed(reliefs: List<Relief>): Boolean {
        if (reliefs.isEmpty()) {
            return false
        }

        reliefs.forEach {
            if (it.distributedAt == null) {
                return false
            }
        }

        return true
    }

    private fun isBookletDistributed(booklets: List<Booklet>): Boolean {
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
}