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
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.Navigation
import androidx.work.*
import com.google.android.material.navigation.NavigationView
import cz.applifting.humansis.R
import cz.applifting.humansis.extensions.getDate
import cz.applifting.humansis.extensions.isWifiConnected
import cz.applifting.humansis.misc.NfcInitializer
import cz.applifting.humansis.misc.NfcTagPublisher
import cz.applifting.humansis.misc.Utilities
import cz.applifting.humansis.synchronization.SYNC_WORKER
import cz.applifting.humansis.synchronization.SyncWorker
import cz.applifting.humansis.ui.main.LAST_DOWNLOAD_KEY
import java.util.*
import javax.inject.Inject


/**
 * Created by Petr Kubes <petr.kubes@applifting.cz> on 11, September, 2019
 */
class HumansisActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener  {

    @Inject
    lateinit var sp: SharedPreferences
    @Inject
    lateinit var nfcTagPublisher: NfcTagPublisher

    lateinit var utilities: Utilities

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
        utilities = Utilities(this)
    }

    override fun onDestroy() {
        unregisterReceiver(networkChangeReceiver)
        super.onDestroy()
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val mainNavController = Navigation.findNavController(this, R.id.nav_host_fragment)
        when (item.itemId) {
            R.id.action_read_balance -> {
                utilities.showReadBalanceDialog()
            }
            R.id.action_initialize_cards -> {
                utilities.showInitializeCardsDialog()
            }
            R.id.projectsFragment -> {
                mainNavController.navigate(R.id.projectsFragment)
            }
            R.id.settingsFragment -> {
                mainNavController.navigate(R.id.settingsFragment)
            }
        }
        findViewById<DrawerLayout>(R.id.drawer_layout).closeDrawer(GravityCompat.START)
        return true
    }

    override fun onBackPressed() {
        val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)
        val navController = Navigation.findNavController(this, R.id.nav_host_fragment_base)
        if (navController.currentDestination?.id == R.id.mainFragment && drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onPause() {
        NfcInitializer.disableForegroundDispatch(this)
        super.onPause()
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
        if (NfcAdapter.ACTION_TAG_DISCOVERED == intent.action || NfcAdapter.ACTION_NDEF_DISCOVERED == intent.action) {
            val tag: Tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)?:return
            nfcTagPublisher.getTagSubject().onNext(tag)
        }
    }
}