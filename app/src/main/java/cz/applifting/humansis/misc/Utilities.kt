package cz.applifting.humansis.misc

import android.app.Activity
import android.app.AlertDialog
import android.widget.Toast
import cz.applifting.humansis.R
import cz.applifting.humansis.ui.main.MainViewModel
import cz.quanti.android.nfc.exception.PINException
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers

class Utilities(
        private val activity: Activity,
        private val mainViewModel: MainViewModel
) {

    private var readBalanceDisposable: Disposable? = null
    private var initializeCardDisposable: Disposable? = null

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
            readBalanceDisposable = mainViewModel.readBalance()
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
                                Toast.makeText(
                                        activity,
                                        activity.getString(R.string.card_error),
                                        Toast.LENGTH_LONG
                                ).show()
                                scanCardDialog.dismiss()
                                NfcInitializer.disableForegroundDispatch(activity)
                            })
        } else {
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
            Toast.makeText(
                    activity,
                    activity.getString(R.string.no_nfc_available),
                    Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun initializeCard(scanCardDialog: AlertDialog) {
        initializeCardDisposable?.dispose()
        initializeCardDisposable = mainViewModel.readBalance()
            .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe({
            showCardInitializedDialog(scanCardDialog, activity.getString(R.string.different_user_card_error))
            },
            {
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
}