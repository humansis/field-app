package cz.applifting.humansis.misc

import android.app.Activity
import android.app.AlertDialog
import android.widget.Toast
import cz.applifting.humansis.R
import cz.applifting.humansis.ui.App
import cz.quanti.android.nfc.PINFacade
import cz.quanti.android.nfc.dto.UserBalance
import cz.quanti.android.nfc.exception.PINException
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import quanti.com.kotlinlog.Log
import javax.inject.Inject

class Utilities(
        private val activity: Activity,
        private val nfcTagPublisher: NfcTagPublisher,
        private val pinFacade: PINFacade
) {
    private var readBalanceDisposable: Disposable? = null
    private var initializeCardDisposable: Disposable? = null

    init {
        (activity.application as App).appComponent.inject(this)
    }

    fun showReadBalanceDialog() {
        if (NfcInitializer.initNfc(activity)) {
            val scanCardDialog = AlertDialog.Builder(activity, R.style.DialogTheme)
                    .setMessage(activity.getString(R.string.scan_the_card))
                    .setCancelable(false)
                    .setNegativeButton(activity.getString(R.string.cancel)) { dialog, _ ->
                        dialog?.dismiss()
                        readBalanceDisposable?.dispose()
                        readBalanceDisposable = null
                    }
                    .create()

            scanCardDialog?.show()

            readBalanceDisposable?.dispose()
            readBalanceDisposable = readBalance()
                    .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe({
                        scanCardDialog.dismiss()
                        val cardContent = it
                        val cardResultDialog = AlertDialog.Builder(activity, R.style.DialogTheme)
                                .setTitle(activity.getString((R.string.read_balance)))
                                .setMessage(
                                        activity.getString(
                                                R.string.scanning_card_balance,
                                                "${cardContent.balance} ${cardContent.currencyCode}"
                                        )
                                )
                                .setCancelable(true)
                                .setNegativeButton(activity.getString(R.string.close)) { dialog, _ ->
                                    dialog?.dismiss()
                                    readBalanceDisposable?.dispose()
                                    readBalanceDisposable = null
                                }
                                .create()
                        cardResultDialog.show()
                    },
                            {
                                Log.e(this.javaClass.simpleName, it)
                                Toast.makeText(
                                        activity,
                                        activity.getString(R.string.card_error),
                                        Toast.LENGTH_LONG
                                ).show()
                                scanCardDialog.dismiss()
                                NfcInitializer.disableForegroundDispatch(activity)
                            })
        } else {
            Log.e(this.javaClass.simpleName, activity.getString(R.string.no_nfc_available))
            Toast.makeText(
                    activity,
                    activity.getString(R.string.no_nfc_available),
                    Toast.LENGTH_LONG
            ).show()
        }
    }

    fun showInitializeCardsDialog() {
        if (NfcInitializer.initNfc(activity)) {
            val scanCardDialog = AlertDialog.Builder(activity, R.style.DialogTheme)
                    .setMessage(activity.getString(R.string.scan_the_card))
                    .setCancelable(false)
                    .setNegativeButton(activity.getString(R.string.cancel)) { dialog, _ ->
                        dialog?.dismiss()
                        initializeCardDisposable?.dispose()
                        initializeCardDisposable = null
                    }
                    .create()

            scanCardDialog?.show()
            initializeCard(scanCardDialog)

        } else {
            Log.e(this.javaClass.simpleName, activity.getString(R.string.no_nfc_available))
            Toast.makeText(
                    activity,
                    activity.getString(R.string.no_nfc_available),
                    Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun initializeCard(scanCardDialog: AlertDialog) {
        initializeCardDisposable?.dispose()
        initializeCardDisposable = readBalance()
            .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe({
            showCardInitializedDialog(scanCardDialog, activity.getString(R.string.different_user_card_error))
            },
            {
                Log.e(this.javaClass.simpleName, it)
                if(it is PINException){
                    showCardInitializedDialog(
                        scanCardDialog,
                        NfcCardErrorMessage.getNfcCardErrorMessage(it.pinExceptionEnum, activity)
                    )
                } else {
                    showCardInitializedDialog(scanCardDialog, activity.getString(R.string.card_error))
                }
            })
    }

    private fun showCardInitializedDialog(scanCardDialog: AlertDialog, title: String) {
        scanCardDialog.dismiss()
        val cardResultDialog = AlertDialog.Builder(activity, R.style.DialogTheme)
            .setTitle(title)
            .setMessage(activity.getString(R.string.scan_another_card))
            .setCancelable(true)
            .setNegativeButton(activity.getString(R.string.close)) { dialog, _ ->
                dialog?.dismiss()
                initializeCardDisposable?.dispose()
                initializeCardDisposable = null
                NfcInitializer.disableForegroundDispatch(activity)
            }
            .create()
        cardResultDialog.show()
        NfcInitializer.initNfc(activity)
        initializeCard(cardResultDialog)
    }

    private fun readBalance(): Single<UserBalance> {
        return nfcTagPublisher.getTagObservable().firstOrError().flatMap{ tag ->
            pinFacade.readUserBalance(tag)
        }
    }
}
