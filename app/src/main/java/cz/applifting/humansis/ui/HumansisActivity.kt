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
import androidx.activity.viewModels
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import androidx.work.*
import com.google.android.material.navigation.NavigationView
import cz.applifting.humansis.R
import cz.applifting.humansis.extensions.getDate
import cz.applifting.humansis.extensions.isWifiConnected
import cz.applifting.humansis.misc.NfcCardErrorMessage
import cz.applifting.humansis.misc.NfcInitializer
import cz.applifting.humansis.misc.NfcTagPublisher
import cz.applifting.humansis.model.db.CategoryType
import cz.applifting.humansis.synchronization.SYNC_WORKER
import cz.applifting.humansis.synchronization.SyncWorker
import cz.applifting.humansis.ui.main.LAST_DOWNLOAD_KEY
import cz.applifting.humansis.ui.main.MainViewModel
import cz.quanti.android.nfc.PINFacade
import cz.quanti.android.nfc.dto.v2.UserBalance
import cz.quanti.android.nfc.exception.PINException
import io.reactivex.disposables.Disposable
import quanti.com.kotlinlog.Log
import java.util.*
import javax.inject.Inject


/**
 * Created by Petr Kubes <petr.kubes@applifting.cz> on 11, September, 2019
 */
class HumansisActivity : BaseActivity(), NavigationView.OnNavigationItemSelectedListener  {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject
    lateinit var sp: SharedPreferences
    @Inject
    lateinit var nfcTagPublisher: NfcTagPublisher
    @Inject
    lateinit var pinFacade: PINFacade

    private val vm: MainViewModel by viewModels { viewModelFactory }

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

        setUpObservers()
    }

    override fun onResume() {
        super.onResume()
        enqueueSynchronization()

        val filter = IntentFilter()
        filter.addAction("android.net.conn.CONNECTIVITY_ACTION")
        filter.addAction("android.net.wifi.STATE_CHANGE")
        registerReceiver(networkChangeReceiver, filter)
    }

    override fun onPause() {
        NfcInitializer.disableForegroundDispatch(this)
        super.onPause()
    }

    override fun onStop() {
        dispose()
        super.onStop()
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

    private fun setUpObservers() {
        observe(vm.readBalanceResult) {
            showReadBalanceResult(it)
        }

        observe(vm.readBalanceError) {
            Log.e(this.javaClass.simpleName, it)
            Toast.makeText(
                this,
                getString(R.string.card_error),
                Toast.LENGTH_LONG
            ).show()
            displayedDialog?.dismiss()
            NfcInitializer.disableForegroundDispatch(this)
        }

        observe(vm.initializeCardResult) {
            showCardInitializedDialog(getString(R.string.different_user_card_error))
        }

        observe(vm.initializeCardError) {
            Log.e(this.javaClass.simpleName, it)
            if(it is PINException){
                showCardInitializedDialog(
                    NfcCardErrorMessage.getNfcCardErrorMessage(it.pinExceptionEnum, this)
                )
            } else {
                showCardInitializedDialog(getString(R.string.card_error))
            }
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

            readBalanceDisposable = null
            readBalanceDisposable = vm.readBalance()

        } else {
            noNfcAvailable()
        }
    }

    private fun showReadBalanceResult(cardContent: UserBalance) {
        displayedDialog?.dismiss()
        val cardResultDialog = AlertDialog.Builder(this, R.style.DialogTheme)
            .setTitle(getString((R.string.read_balance)))
            .setMessage(
                getString(
                    R.string.scanning_card_balance,
                    if (cardContent.expirationDate >= Date()) {
                        "${cardContent.balance} ${cardContent.currencyCode}\n${cardContent.expirationDate}\n${getLimitsAsText(cardContent)}"
                    } else {
                        "0.0 ${cardContent.currencyCode}"
                    }
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
    }

    private fun getLimitsAsText(cardContent: UserBalance): String {
        var limits = ""
        cardContent.limits.map {
            limits += "${CategoryType.getById(it.key).backendName}: ${it.value} ${cardContent.currencyCode} remaining" // TODO preklad? nebo dat pryc?
        }
        return limits
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
            initializeCard()

        } else {
            noNfcAvailable()
        }
    }

    private fun initializeCard() {
        initializeCardDisposable?.dispose()
        initializeCardDisposable = vm.initializeCard()
    }

    private fun showCardInitializedDialog(title: String) {
        displayedDialog?.dismiss()
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
            initializeCard()
        }
    }

    private fun noNfcAvailable() {
        getString(R.string.no_nfc_available)
        Toast.makeText(
            this,
            getString(R.string.no_nfc_available),
            Toast.LENGTH_LONG
        ).show()
    }

    private fun dispose() {
        displayedDialog?.dismiss()
        initializeCardDisposable?.dispose()
        initializeCardDisposable = null
        readBalanceDisposable?.dispose()
        readBalanceDisposable = null
    }
}