package cz.applifting.humansis.ui

import android.app.AlertDialog
import android.content.*
import android.graphics.Color
import android.net.ConnectivityManager
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import androidx.work.*
import com.google.android.material.navigation.NavigationView
import cz.applifting.humansis.R
import cz.applifting.humansis.extensions.getDate
import cz.applifting.humansis.extensions.isWifiConnected
import cz.applifting.humansis.misc.NfcInitializer
import cz.applifting.humansis.misc.NfcTagPublisher
import cz.applifting.humansis.synchronization.SYNC_WORKER
import cz.applifting.humansis.synchronization.SyncWorker
import cz.applifting.humansis.ui.main.LAST_DOWNLOAD_KEY
import cz.applifting.humansis.ui.main.MainViewModel
import cz.quanti.android.nfc.VendorFacade
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
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
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject
    lateinit var vendorFacade: VendorFacade

    private val networkChangeReceiver = NetworkChangeReceiver()
    private val mainViewModel: MainViewModel by viewModels { viewModelFactory }
    private var readBalanceDisposable: Disposable? = null

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

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val mainNavController = Navigation.findNavController(this, R.id.nav_host_fragment)
        when (item.itemId) {
            R.id.action_read_balance -> {
                showReadBalanceDialog()
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

    private fun showReadBalanceDialog() {
        if (NfcInitializer.initNfc(this)) {
            val scanCardDialog = AlertDialog.Builder(this, R.style.DialogTheme)
                .setMessage(getString(R.string.scan_the_card))
                .setCancelable(false)
                .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                    dialog?.dismiss()
                }
                .create()

            scanCardDialog?.show()

            readBalanceDisposable?.dispose()
            readBalanceDisposable = mainViewModel.readBalance()
            .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe({
                scanCardDialog.dismiss()
                val cardContent = it
                val cardResultDialog = AlertDialog.Builder(this, R.style.DialogTheme)
                .setTitle(getString((R.string.read_balance)))
                .setMessage(
                        getString(
                                R.string.scanning_card_balance,
                                "${cardContent.balance} ${cardContent.currencyCode}"
                        )
                )
                .setCancelable(true)
                .setNegativeButton(getString(R.string.close)) { dialog, _ ->
                    dialog?.dismiss()
                    readBalanceDisposable?.dispose()
                    readBalanceDisposable = null
                }
                .create()
                cardResultDialog.show()
            },
            {
                Toast.makeText(
                    this,
                    getString(R.string.card_error),
                    Toast.LENGTH_LONG
                ).show()
                scanCardDialog.dismiss()
                NfcInitializer.disableForegroundDispatch(this)
            })
        }
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