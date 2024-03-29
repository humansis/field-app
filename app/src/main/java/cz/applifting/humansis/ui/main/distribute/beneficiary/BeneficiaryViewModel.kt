package cz.applifting.humansis.ui.main.distribute.beneficiary

import android.nfc.Tag
import androidx.lifecycle.MutableLiveData
import cz.applifting.humansis.misc.NfcTagPublisher
import cz.applifting.humansis.model.db.BeneficiaryLocal
import cz.applifting.humansis.repositories.BeneficiariesRepository
import cz.applifting.humansis.ui.App
import cz.applifting.humansis.ui.BaseViewModel
import cz.applifting.humansis.ui.main.distribute.beneficiary.BeneficiaryDialog.Companion.ALREADY_ASSIGNED
import cz.applifting.humansis.ui.main.distribute.beneficiary.BeneficiaryDialog.Companion.INVALID_CODE
import cz.quanti.android.nfc.OfflineFacade
import cz.quanti.android.nfc.dto.v2.Deposit
import cz.quanti.android.nfc.dto.v2.UserPinBalance
import cz.quanti.android.nfc.exception.PINException
import cz.quanti.android.nfc.exception.PINExceptionEnum
import io.reactivex.Single
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import quanti.com.kotlinlog.Log

/**
 * Created by Vaclav Legat <vaclav.legat@applifting.cz>
 * @since 9. 9. 2019
 */

class BeneficiaryViewModel @Inject constructor(
    private val beneficiariesRepository: BeneficiariesRepository,
    app: App
) : BaseViewModel(app) {

    val beneficiaryLD = MutableLiveData<BeneficiaryLocal>()
    val scannedIdLD = MutableLiveData<String>()
    private val scannedCardIdLD = MutableLiveData<String>()
    val goBackEventLD = MutableLiveData<Unit>()
    @Inject
    lateinit var nfcTagPublisher: NfcTagPublisher
    @Inject
    lateinit var nfcFacade: OfflineFacade

    var previousEditState: Boolean? = null
    var isAssignedInOtherAssistance: Boolean = false
    private set

    fun initBeneficiary(id: Int) {
        launch {
            beneficiariesRepository.getBeneficiaryOfflineFlow(id)
                .collect {
                    it?.let {
                        isAssignedInOtherAssistance = beneficiariesRepository.isAssignedInOtherAssistance(it)
                        beneficiaryLD.value = it
                    } ?: run {
                        goBackEventLD.value = Unit
                    }
                }
        }
    }

    fun scanQRBooklet(code: String?) {
        launch {
            beneficiaryLD.value?.let {
                val beneficiary = it.copy(
                    qrBooklets = listOfNotNull(code)
                )
                beneficiariesRepository.updateBeneficiaryOffline(beneficiary)
                beneficiaryLD.value = beneficiary
            }
        }
    }

    fun depositMoneyToCard(
        pin: String,
        remote: Boolean,
        deposit: Deposit,
        tagFoundCallback: () -> Unit
    ): Single<Pair<Tag, UserPinBalance>> {
        return nfcTagPublisher.getTagObservable().firstOrError().flatMap { tag ->
            tagFoundCallback.invoke()
            if (remote) {
                nfcFacade.rewriteBalanceForUser(tag, deposit)
            } else {
                nfcFacade.writeOrRewriteProtectedBalanceForUser(tag, pin, deposit)
            }
                .onErrorResumeNext {
                    if (it is PINException && it.pinExceptionEnum == PINExceptionEnum.ALREADY_DISTRIBUTED) {
                        Log.d(TAG, "Already distributed, reading balance.")
                        nfcFacade.readProtectedBalanceForUser(tag).map { balance ->
                            if (balance.balance == deposit.amount) {
                                balance
                            } else {
                                throw it
                            }
                        }
                    } else {
                        throw it
                    }
                }
                .map {
                    Pair(tag, it)
                }
        }
    }

    fun changePinForCard(
        pin: String,
        ownerId: Int,
        tagFoundCallback: () -> Unit
    ): Single<Pair<Tag, UserPinBalance>> {
        return nfcTagPublisher.getTagObservable().firstOrError().flatMap { tag ->
            tagFoundCallback.invoke()
            nfcFacade.changePinForCard(tag, ownerId, pin).map {
                Pair(tag, it)
            }
        }
    }

    fun saveCard(cardId: String, date: String, originalBalance: Double?, balance: Double) {
        launch {
            beneficiaryLD.value?.let {
                val beneficiary = it.copy(
                    newSmartcard = cardId.toUpperCase(Locale.US),
                    edited = true,
                    distributed = true,
                    distributedAt = date,
                    originalBalance = originalBalance,
                    balance = balance
                )

                beneficiariesRepository.updateBeneficiaryOffline(beneficiary)
                beneficiaryLD.value = beneficiary
                scannedCardIdLD.value = cardId
            }
        }
    }

    internal fun revertBeneficiary() {
        launch {
            beneficiaryLD.value?.let {
                val beneficiary = it
                val updatedBeneficiary = beneficiary.copy(
                    distributed = false,
                    edited = false,
                    qrBooklets = emptyList(),
                    referralType = beneficiary.originalReferralType,
                    referralNote = beneficiary.originalReferralNote
                )

                beneficiariesRepository.updateBeneficiaryOffline(updatedBeneficiary)
                beneficiaryLD.value = updatedBeneficiary
            }
        }
    }

    internal fun checkScannedId(scannedId: String) {
        launch {
            val assigned = beneficiariesRepository.checkBookletAssignedLocally(scannedId)

            val bookletId = when {
                assigned -> ALREADY_ASSIGNED
                isValidBookletCode(scannedId) -> scannedId
                else -> INVALID_CODE
            }
            scannedIdLD.value = bookletId
        }
    }

    private fun isValidBookletCode(code: String): Boolean {
        return (BOOKLET_REGEX.matches(code) || NEW_BOOKLET_REGEX.matches(code))
    }

    companion object {
        private val TAG = BeneficiaryViewModel::class.java.simpleName
        private val BOOKLET_REGEX = "^\\d{1,6}-\\d{1,6}-\\d{1,6}$".toRegex(RegexOption.IGNORE_CASE)
        private val NEW_BOOKLET_REGEX = "^[a-zA-Z0-9]{2,3}_.+_[0-9]{1,2}-[0-9]{1,2}-[0-9]{2,4}_((booklet)|(batch))[0-9]+$".toRegex(RegexOption.IGNORE_CASE)
    }
}