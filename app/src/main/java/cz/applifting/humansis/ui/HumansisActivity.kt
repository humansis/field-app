package cz.applifting.humansis.ui

import android.content.*
import android.graphics.Color
import android.net.ConnectivityManager
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.work.*
import cz.applifting.humansis.R
import cz.applifting.humansis.extensions.getDate
import cz.applifting.humansis.extensions.isWifiConnected
import cz.applifting.humansis.misc.NfcTagPublisher
import cz.applifting.humansis.synchronization.SYNC_WORKER
import cz.applifting.humansis.synchronization.SyncWorker
import cz.applifting.humansis.ui.main.LAST_DOWNLOAD_KEY
import java.util.*
import javax.inject.Inject


/**
 * Created by Petr Kubes <petr.kubes@applifting.cz> on 11, September, 2019
 */
class HumansisActivity : AppCompatActivity() {

    @Inject
    lateinit var sp: SharedPreferences
    @Inject
    lateinit var nfcTagPublisher: NfcTagPublisher

    private val networkChangeReceiver = NetworkChangeReceiver()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_humansis)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            window.statusBarColor = Color.BLACK
        }

        (application as App).appComponent.inject(this)
    }

    override fun onResume() {
        super.onResume()
        enqueueSynchronization()

        val filter = IntentFilter()
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION)
        filter.addAction("android.net.wifi.STATE_CHANGE")
        registerReceiver(networkChangeReceiver, filter)
    }

    override fun onDestroy() {
        unregisterReceiver(networkChangeReceiver)
        super.onDestroy()
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        super.onOptionsItemSelected(item)
        return false
    }

    private fun enqueueSynchronization() {
        val workManager = WorkManager.getInstance(this)

        // Try to upload changes as soon as user is online
        if (lastUploadWasLongTimeAgo()) {
            val whenOnWifiConstraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.UNMETERED)
                .build()
            val syncWhenWifiRequest = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(whenOnWifiConstraints)
                .build()

            workManager.enqueueUniqueWork(SYNC_WORKER, ExistingWorkPolicy.KEEP, syncWhenWifiRequest)
        }
    }

    private fun lastUploadWasLongTimeAgo(): Boolean {
        val lastDownloadDate = sp.getDate(LAST_DOWNLOAD_KEY)
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.HOUR_OF_DAY, -1)
        val dateHourAgo = calendar.time


        return (lastDownloadDate != null && lastDownloadDate.before(dateHourAgo))
    }

    private inner class NetworkChangeReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (isWifiConnected()) {
                enqueueSynchronization()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (NfcAdapter.ACTION_TAG_DISCOVERED == intent.action) {
            val tag: Tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
            nfcTagPublisher.getTagSubject().onNext(tag)
        }
    }
}