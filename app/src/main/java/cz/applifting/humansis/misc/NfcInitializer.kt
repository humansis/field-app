package cz.applifting.humansis.misc

import android.app.Activity
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.Intent
import android.nfc.NfcAdapter
import android.provider.Settings
import android.widget.Toast
import cz.applifting.humansis.R

object NfcInitializer {

    fun initNfc(activity: Activity): Boolean {
        val nfcAdapter = NfcAdapter.getDefaultAdapter(activity)

        if (nfcAdapter == null) {
            Toast.makeText(
                activity,
                activity.getString(R.string.no_nfc_available),
                Toast.LENGTH_LONG
            ).show()
            return false
        }

        val pendingIntent = PendingIntent.getActivity(
            activity, 0,
            Intent(activity, activity.javaClass)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0
        )

        nfcAdapter.let {
            return if (!it.isEnabled) {
                showWirelessSettings(activity)
                false
            } else {
                it.enableForegroundDispatch(activity, pendingIntent, null, null)
                true
            }
        }
    }

    private fun showWirelessSettings(activity: Activity) {
        AlertDialog.Builder(activity, R.style.DialogTheme)
            .setMessage(activity.getString(R.string.you_need_to_enable_nfc))
            .setCancelable(true)
            .setPositiveButton(activity.getString(R.string.action_settings)) { _,_ ->
                activity.startActivity(Intent(Settings.ACTION_NFC_SETTINGS))
            }
            .setNegativeButton(activity.getString(R.string.cancel), null)
            .create()
            .show()
    }

    fun disableForegroundDispatch(activity: Activity) {
        NfcAdapter.getDefaultAdapter(activity)?.disableForegroundDispatch(activity)
    }
}