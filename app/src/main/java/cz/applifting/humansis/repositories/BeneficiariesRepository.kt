package cz.applifting.humansis.repositories

import android.content.Context
import cz.applifting.humansis.api.HumansisService
import cz.applifting.humansis.db.DbProvider
import cz.applifting.humansis.extensions.convertForApiRequestBody
import cz.applifting.humansis.model.CommodityType
import cz.applifting.humansis.model.api.AssignBookletRequest
import cz.applifting.humansis.model.api.AssignSmartcardRequest
import cz.applifting.humansis.model.api.BeneficiaryForReferralUpdate
import cz.applifting.humansis.model.api.Booklet
import cz.applifting.humansis.model.api.DistributeSmartcardRequest
import cz.applifting.humansis.model.api.DistributedReliefPackages
import cz.applifting.humansis.model.api.AssistanceBeneficiary
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

        val assistance = dbProvider.get().assistancesDao().getById(assistanceId)

        val data = service.getAssistanceBeneficiaries(assistanceId).data

        val duplicateBeneficiaryNames = findAllDuplicateNames(data)

        val result = data.map { assistanceBeneficiary ->
            BeneficiaryLocal(
                id = assistanceBeneficiary.id,
                beneficiaryId = assistanceBeneficiary.beneficiary.id,
                givenName = assistanceBeneficiary.beneficiary.localGivenName,
                familyName = assistanceBeneficiary.beneficiary.localFamilyName,
                assistanceId = assistanceId,
                distributed = areReliefPackagesDistributed(assistanceBeneficiary.reliefPackages) || areBookletsDistributed(
                    assistanceBeneficiary.booklets
                ) || assistanceBeneficiary.distributedAt != null,
                distributedAt = assistanceBeneficiary.distributedAt,
                reliefIDs = parseGeneralReliefPackages(assistanceBeneficiary.reliefPackages),
                qrBooklets = parseQRBooklets(assistanceBeneficiary.booklets),
                smartcard = assistanceBeneficiary.currentSmartcardSerialNumber?.toUpperCase(Locale.US),
                newSmartcard = null,
                edited = false,
                commodities = parseCommodities(assistanceBeneficiary.booklets, assistanceBeneficiary.reliefPackages),
                remote = assistance?.remote ?: false,
                dateExpiration = assistance?.dateOfExpiration,
                foodLimit = assistance?.foodLimit,
                nonfoodLimit = assistance?.nonfoodLimit,
                cashbackLimit = assistance?.cashbackLimit,
                nationalIds = assistanceBeneficiary.beneficiary.nationalCardIds,
                originalReferralType = assistanceBeneficiary.beneficiary.referralType,
                originalReferralNote = assistanceBeneficiary.beneficiary.referralComment,
                referralType = assistanceBeneficiary.beneficiary.referralType,
                referralNote = assistanceBeneficiary.beneficiary.referralComment,
                hasDuplicateName = isDuplicateName(duplicateBeneficiaryNames, assistanceBeneficiary)
            )
        }

        dbProvider.get().beneficiariesDao().deleteByAssistance(assistanceId)
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

    fun getBeneficiariesOffline(assistanceId: Int): Flow<List<BeneficiaryLocal>> {
        return dbProvider.get().beneficiariesDao().getByAssistance(assistanceId)
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

    suspend fun isAssignedInOtherAssistance(beneficiary: BeneficiaryLocal): Boolean {
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
                beneficiaryLocal.distributedAt ?: Date().convertForApiRequestBody()
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
            }

            val lastSmartCardDistribution = beneficiaryLocal.commodities
                .findLast { it.type == CommodityType.SMARTCARD }

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
                CommodityLocal(
                    0,
                    CommodityType.QR_VOUCHER,
                    bookletValue,
                    booklet.currency
                )
            }
        }

        return reliefPackages.map {
            CommodityLocal(
                it.id,
                it.modalityType,
                it.amountToDistribute,
                it.unit,
                it.notes
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

    private fun findAllDuplicateNames(list: List<AssistanceBeneficiary>): List<FullName> {
        val seenNames = mutableSetOf<FullName>()
        return list.asSequence()
            .map { FullName(it.beneficiary.localGivenName ?: "", it.beneficiary.localFamilyName ?: "") }
            .filter { !seenNames.add(it) }
            .distinct()
            .toList()
    }

    private fun isDuplicateName(
        duplicateBeneficiaryNames: List<FullName>,
        assistanceBeneficiary: AssistanceBeneficiary
    ): Boolean {
        return duplicateBeneficiaryNames.find {
            it == FullName(
                assistanceBeneficiary.beneficiary.localGivenName,
                assistanceBeneficiary.beneficiary.localFamilyName
            )
        } != null
    }

    private data class FullName(
        val givenName: String?,
        val familyName: String?
    )
}