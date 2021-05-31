package cz.applifting.humansis.ui

import android.app.AlertDialog
import android.content.*
import android.graphics.Color
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.Navigation
import androidx.work.*
import com.google.android.material.navigation.NavigationView
import cz.applifting.humansis.R
import cz.applifting.humansis.extensions.getDate
import cz.applifting.humansis.extensions.isWifiConnected
import cz.applifting.humansis.misc.NfcCardErrorMessage
import cz.applifting.humansis.misc.NfcInitializer
import cz.applifting.humansis.misc.NfcTagPublisher
import cz.applifting.humansis.synchronization.SYNC_WORKER
import cz.applifting.humansis.synchronization.SyncWorker
import cz.applifting.humansis.ui.main.LAST_DOWNLOAD_KEY
import cz.quanti.android.nfc.PINFacade
import cz.quanti.android.nfc.dto.UserBalance
import cz.quanti.android.nfc.exception.PINException
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import quanti.com.kotlinlog.Log
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
    lateinit var pinFacade: PINFacade

    private val networkChangeReceiver = NetworkChangeReceiver()

    private var displayedDialog: AlertDialog? = null
    private var readBalanceDisposable: Disposable? = null
    private var initializeCardDisposable: Disposable? = null

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
        filter.addAction("android.net.conn.CONNECTIVITY_ACTION")
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
            R.id.action_initialize_cards -> {
                showInitializeCardsDialog()
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

    override fun onStop() {
        dispose()
        super.onStop()
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

    private fun showReadBalanceDialog() {
        if (NfcInitializer.initNfc(this)) {
            val scanCardDialog = AlertDialog.Builder(this, R.style.DialogTheme)
                .setMessage(getString(R.string.scan_the_card))
                .setCancelable(false)
                .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                    dialog?.dismiss()
                    readBalanceDisposable?.dispose()
                    readBalanceDisposable = null
                }
                .create()

            scanCardDialog?.show()
            displayedDialog = scanCardDialog

            readBalanceDisposable?.dispose()
            readBalanceDisposable = readBalance()
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
                    displayedDialog = cardResultDialog
                },
                    {
                        Log.e(this.javaClass.simpleName, it)
                        Toast.makeText(
                            this,
                            getString(R.string.card_error),
                            Toast.LENGTH_LONG
                        ).show()
                        scanCardDialog.dismiss()
                        NfcInitializer.disableForegroundDispatch(this)
                    })
        } else {
            Log.e(this.javaClass.simpleName, getString(R.string.no_nfc_available))
            Toast.makeText(
                this,
                getString(R.string.no_nfc_available),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun showInitializeCardsDialog() {
        if (NfcInitializer.initNfc(this)) {
            val scanCardDialog = AlertDialog.Builder(this, R.style.DialogTheme)
                .setMessage(getString(R.string.scan_the_card))
                .setCancelable(false)
                .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                    dialog?.dismiss()
                    initializeCardDisposable?.dispose()
                    initializeCardDisposable = null
                }
                .create()

            scanCardDialog?.show()
            displayedDialog = scanCardDialog
            initializeCard(scanCardDialog)

        } else {
            getString(R.string.no_nfc_available)
            Toast.makeText(
                this,
                getString(R.string.no_nfc_available),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun initializeCard(scanCardDialog: AlertDialog) {
        initializeCardDisposable?.dispose()
        initializeCardDisposable = readBalance()
            .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe({
                showCardInitializedDialog(scanCardDialog, getString(R.string.different_user_card_error))
            },
                {
                    Log.e(this.javaClass.simpleName, it)
                    if(it is PINException){
                        showCardInitializedDialog(
                            scanCardDialog,
                            NfcCardErrorMessage.getNfcCardErrorMessage(it.pinExceptionEnum, this)
                        )
                    } else {
                        showCardInitializedDialog(scanCardDialog, getString(R.string.card_error))
                    }
                })
    }

    private fun showCardInitializedDialog(scanCardDialog: AlertDialog, title: String) {
        scanCardDialog.dismiss()
        val cardResultDialog = AlertDialog.Builder(this, R.style.DialogTheme)
            .setTitle(title)
            .setMessage(getString(R.string.scan_another_card))
            .setCancelable(true)
            .setNegativeButton(getString(R.string.close)) { dialog, _ ->
                dialog?.dismiss()
                initializeCardDisposable?.dispose()
                initializeCardDisposable = null
                NfcInitializer.disableForegroundDispatch(this)
            }
            .create()
        cardResultDialog.show()
        displayedDialog = cardResultDialog
        if (NfcInitializer.initNfc(this)) {
            initializeCard(cardResultDialog)
        }
    }

    private fun readBalance(): Single<UserBalance> {
        return nfcTagPublisher.getTagObservable().firstOrError().flatMap{ tag ->
            pinFacade.readUserBalance(tag)
        }
    }

    private fun dispose() {
        displayedDialog?.dismiss()
        initializeCardDisposable?.dispose()
        initializeCardDisposable = null
        readBalanceDisposable?.dispose()
        readBalanceDisposable = null
    }
}