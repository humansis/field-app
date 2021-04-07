package cz.applifting.humansis.misc

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.content.res.Resources
import android.nfc.NfcAdapter
import android.provider.Settings
import android.widget.Toast
import cz.applifting.humansis.R

class NfcInitializer(
        private val activity: Activity
    ) {

    private var pendingIntent: PendingIntent? = null

    fun initNfc(): Boolean {
        val nfcAdapter = NfcAdapter.getDefaultAdapter(activity)
                ?: // NFC is not available on this device
                return false

        pendingIntent = PendingIntent.getActivity(
                activity, 0,
                Intent(activity, activity.javaClass)
                        .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0
        )

        nfcAdapter.let { nfcAdapter ->
            if (!nfcAdapter.isEnabled) {
                showWirelessSettings()
            }
            nfcAdapter.enableForegroundDispatch(activity, pendingIntent, null, null)
        }

        return true
    }

    private fun showWirelessSettings() {
        Toast.makeText(
                activity,
                Resources.getSystem().getString(R.string.you_need_to_enable_nfc),
                Toast.LENGTH_LONG
        ).show()
        val intent = Intent(Settings.ACTION_WIRELESS_SETTINGS)
        activity.startActivity(intent)
    }
}