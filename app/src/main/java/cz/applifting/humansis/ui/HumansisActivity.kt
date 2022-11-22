package cz.applifting.humansis.ui

import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.graphics.Color
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import androidx.navigation.findNavController
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.material.navigation.NavigationView
import cz.applifting.humansis.BuildConfig
import cz.applifting.humansis.R
import cz.applifting.humansis.extensions.getDate
import cz.applifting.humansis.extensions.isWifiConnected
import cz.applifting.humansis.misc.NfcCardErrorMessage
import cz.applifting.humansis.misc.NfcInitializer
import cz.applifting.humansis.misc.NfcTagPublisher
import cz.applifting.humansis.misc.SmartcardUtilities.getExpirationDateAsString
import cz.applifting.humansis.misc.SmartcardUtilities.getLimitsAsText
import cz.applifting.humansis.synchronization.SYNC_WORKER
import cz.applifting.humansis.synchronization.SyncWorker
import cz.applifting.humansis.ui.main.LAST_DOWNLOAD_KEY
import cz.applifting.humansis.ui.main.MainViewModel
import cz.quanti.android.nfc.dto.v2.UserPinBalance
import cz.quanti.android.nfc.exception.PINException
import io.reactivex.disposables.Disposable
import java.util.Calendar
import java.util.Date
import javax.inject.Inject
import quanti.com.kotlinlog.Log

/**
 * Created by Petr Kubes <petr.kubes@applifting.cz> on 11, September, 2019
 */
class HumansisActivity : BaseActivity(), NfcAdapter.ReaderCallback, NavigationView.OnNavigationItemSelectedListener {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject
    lateinit var sp: SharedPreferences
    @Inject
    lateinit var nfcTagPublisher: NfcTagPublisher

    private val vm: MainViewModel by viewModels { viewModelFactory }

    private val networkChangeReceiver = NetworkChangeReceiver()

    private var displayedDialog: AlertDialog? = null
    private var displayedToast: Toast? = null

    private var readBalanceDisposable: Disposable? = null
    private var initializeCardDisposable: Disposable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate")

        setContentView(R.layout.activity_humansis)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            window.statusBarColor = Color.BLACK
        }

        (application as App).appComponent.inject(this)

        setUpObservers()
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart")
        checkAppVersion()
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume")

        checkTokenAndEnqueueSynchronization()

        val filter = IntentFilter()
        filter.addAction("android.net.conn.CONNECTIVITY_ACTION")
        filter.addAction("android.net.wifi.STATE_CHANGE")
        registerReceiver(networkChangeReceiver, filter)
    }

    override fun onPause() {
        NfcInitializer.disableForegroundDispatch(this)
        Log.d(TAG, "onPause")
        super.onPause()
    }

    override fun onStop() {
        dispose()
        Log.d(TAG, "onStop")
        super.onStop()
    }

    override fun onDestroy() {
        unregisterReceiver(networkChangeReceiver)
        Log.d(TAG, "onDestroy")
        super.onDestroy()
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val mainNavController = Navigation.findNavController(this, R.id.nav_host_fragment)
        when (item.itemId) {
            R.id.action_read_balance -> {
                Log.d(TAG, "onNavigationItemSelected Read balance")
                showReadBalanceDialog()
            }
            R.id.action_initialize_cards -> {
                Log.d(TAG, "onNavigationItemSelected Initialize cards")
                showInitializeCardsDialog()
            }
            R.id.projectsFragment -> {
                Log.d(TAG, "onNavigationItemSelected Projects")
                mainNavController.navigate(R.id.projectsFragment)
            }
            R.id.settingsFragment -> {
                Log.d(TAG, "onNavigationItemSelected Settings")
                mainNavController.navigate(R.id.settingsFragment)
            }
        }
        findViewById<DrawerLayout>(R.id.drawer_layout).closeDrawer(GravityCompat.START)
        return true
    }

    override fun onBackPressed() {
        Log.d(TAG, "onBackPressed")
        val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)
        val navController = Navigation.findNavController(this, R.id.nav_host_fragment_base)
        if (navController.currentDestination?.id == R.id.mainFragment && drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    private fun checkAppVersion() {
        val lastVersion = sp.getString(LAST_VERSION_KEY, "unknown")
        val currentVersion = BuildConfig.VERSION_NAME
        if (currentVersion != lastVersion) {
            Log.d(TAG, "App updated from $lastVersion to $currentVersion")
            sp.edit().putString(LAST_VERSION_KEY, currentVersion).apply()
        }
    }

    private fun checkTokenAndEnqueueSynchronization() {
        if (findNavController(R.id.nav_host_fragment_base).currentDestination?.id == R.id.mainFragment) {
            if (vm.validateToken(this)) {
                enqueueSynchronization()
            }
        } else {
            enqueueSynchronization()
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
            Log.d(TAG, "Synchronization enqueued")
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
                checkTokenAndEnqueueSynchronization()
            }
        }
    }

    override fun onTagDiscovered(tag: Tag) {
        Log.d(TAG, "onTagDiscovered")
        nfcTagPublisher.getTagSubject().onNext(tag)
    }

    private fun setUpObservers() {

        // The method is here, so nobody can show toasts from anywhere else other than sharedViewModel.toastLD
        fun showToast(text: String) {
            displayedToast?.cancel()
            displayedToast = null
            val toastView = layoutInflater.inflate(R.layout.custom_toast, null)
            val tvMessage = toastView.findViewById<TextView>(R.id.tv_toast)
            tvMessage.text = text
            displayedToast = Toast(this)
            displayedToast?.duration = Toast.LENGTH_SHORT
            displayedToast?.view = toastView
            displayedToast?.show()
        }

        observe(vm.getToastMessageLiveData()) {
            if (it != null) {
                showToast(it)
                vm.setToastMessage(null)
            }
        }

        observe(vm.readBalanceResult) {
            showReadBalanceResult(it)
        }

        observe(vm.readBalanceError) {
            Log.e(this.javaClass.simpleName, it)
            vm.setToastMessage(
                if (it is PINException) {
                    NfcCardErrorMessage.getNfcCardErrorMessage(it.pinExceptionEnum, this)
                } else {
                    getString(R.string.card_error)
                }
            )
            displayedDialog?.dismiss()
            NfcInitializer.disableForegroundDispatch(this)
        }

        observe(vm.initializeCardResult) {
            showCardInitializedDialog(getString(R.string.different_user_card_error))
        }

        observe(vm.initializeCardError) {
            Log.e(this.javaClass.simpleName, it)
            if (it is PINException) {
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
        }
    }

    private fun showReadBalanceResult(cardContent: UserPinBalance) {
        displayedDialog?.dismiss()
        val expirationDate = cardContent.expirationDate
        val cardResultDialog = AlertDialog.Builder(this, R.style.DialogTheme)
            .setTitle(getString((R.string.read_balance)))
            .setMessage(
                if (expirationDate != null && expirationDate < Date()) {
                    getString(R.string.card_balance_expired)
                } else {
                    getString(
                        R.string.scanning_card_balance,
                        if (cardContent.balance == 0.0) {
                            "${0.0} ${cardContent.currencyCode}"
                        } else {
                            "${cardContent.balance} ${cardContent.currencyCode}" +
                            getExpirationDateAsString(expirationDate, this) +
                            getLimitsAsText(cardContent.limits, cardContent.currencyCode, this)
                        }
                    )
                }
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

    private fun dispose() {
        displayedDialog?.dismiss()
        initializeCardDisposable?.dispose()
        initializeCardDisposable = null
        readBalanceDisposable?.dispose()
        readBalanceDisposable = null
    }

    companion object {
        private val TAG = HumansisActivity::class.java.simpleName
        private const val LAST_VERSION_KEY = "last-version-key"
    }
}