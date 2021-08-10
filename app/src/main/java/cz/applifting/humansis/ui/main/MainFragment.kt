package cz.applifting.humansis.ui.main

import android.app.AlertDialog
import android.content.*
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.*
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.navigation.NavigationView
import cz.applifting.humansis.BuildConfig
import cz.applifting.humansis.R
import cz.applifting.humansis.R.id.action_open_status_dialog
import cz.applifting.humansis.extensions.hideSoftKeyboard
import cz.applifting.humansis.extensions.simpleDrawable
import cz.applifting.humansis.extensions.visible
import cz.applifting.humansis.misc.HumansisError
import cz.applifting.humansis.ui.BaseFragment
import cz.applifting.humansis.ui.HumansisActivity
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.fragment_main.*
import kotlinx.android.synthetic.main.menu_status_button.view.*


/**
 * Created by Petr Kubes <petr.kubes@applifting.cz> on 14, August, 2019
 */
class MainFragment : BaseFragment(){

    private val viewModel: MainViewModel by viewModels { viewModelFactory }
    private lateinit var baseNavController: NavController
    private lateinit var mainNavController: NavController
    private lateinit var drawer: DrawerLayout
    private lateinit var onDestinationChangedListener: NavController.OnDestinationChangedListener

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        sharedViewModel.observeConnection()

        view?.hideSoftKeyboard()

        val appBarConfiguration = AppBarConfiguration(
            setOf(R.id.projectsFragment, R.id.settingsFragment),
            drawer_layout
        )

        drawer = requireActivity().findViewById(R.id.drawer_layout)

        val fragmentContainer = view?.findViewById<View>(R.id.nav_host_fragment) ?: throw HumansisError(
            "Cannot find nav host in main"
        )

        baseNavController = findNavController()
        mainNavController = Navigation.findNavController(fragmentContainer)

        (activity as HumansisActivity).setSupportActionBar(tb_toolbar)

        tb_toolbar.setupWithNavController(mainNavController, appBarConfiguration)
        nav_view.setupWithNavController(mainNavController)

        val metrics: DisplayMetrics = resources.displayMetrics
        val ivAppIcon = nav_view.getHeaderView(0).findViewById<ImageView>(R.id.iv_app_icon)
        ivAppIcon.layoutParams.height = if ((metrics.heightPixels/metrics.density) > 640) {
            resources.getDimensionPixelSize(R.dimen.nav_header_image_height_tall)
        } else {
            resources.getDimensionPixelSize(R.dimen.nav_header_image_height_regular)
        }

        val tvAppVersion = nav_view.getHeaderView(0).findViewById<TextView>(R.id.tv_app_version)
        var appVersion = getString(R.string.app_name) + " " + getString(R.string.version, BuildConfig.VERSION_NAME)
        if (BuildConfig.DEBUG) {
            appVersion += (" (" + BuildConfig.BUILD_NUMBER + ")")
        }
        tvAppVersion.text = appVersion

        val tvEnvironment = nav_view.getHeaderView(0).findViewById<TextView>(R.id.tv_environment)
        if(BuildConfig.DEBUG) {
            tvEnvironment.text = getString(
                R.string.environment,
                viewModel.environmentLD.value
            )
        } else {
            tvEnvironment.visibility = View.GONE
        }

        // Define Observers
        viewModel.userLD.observe(viewLifecycleOwner, Observer {
            if (it == null) {
                findNavController().navigate(R.id.logout)
                return@Observer
            }

            val tvUsername = nav_view.getHeaderView(0).findViewById<TextView>(R.id.tv_username)
            tvUsername.text = it.username
        })

        sharedViewModel.toastLD.observe(viewLifecycleOwner, Observer {
            if (it != null) {
                showToast(it)
                sharedViewModel.showToast(null)
            }
        })

        sharedViewModel.shouldReauthenticateLD.observe(viewLifecycleOwner, Observer {
            if (it) {
                sharedViewModel.resetShouldReauthenticate()
                baseNavController.navigate(R.id.loginFragment)
            }
        })

        btn_logout.setOnClickListener {

            val pendingChanges = sharedViewModel.syncNeededLD.value ?: false

            if (!pendingChanges) {
                 AlertDialog.Builder(requireContext(), R.style.DialogTheme)
                    .setTitle(R.string.logout_alert_title)
                    .setMessage(getString(R.string.logout_alert_text))
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        viewModel.logout()
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .setIcon(R.drawable.ic_warning)
                    .show()
            } else {
                AlertDialog.Builder(requireContext(), R.style.DialogTheme)
                    .setTitle(R.string.logout_alert_pending_changes_title)
                    .setMessage(getString(R.string.logout_alert_pending_changes_text))
                    .setNegativeButton(R.string.close, null)
                    .setIcon(R.drawable.ic_warning)
                    .show()
            }
        }

        sharedViewModel.tryFirstDownload()

        val navigationView = requireActivity().findViewById<NavigationView>(R.id.nav_view)
        navigationView.setNavigationItemSelectedListener(requireActivity() as HumansisActivity)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onDestroy() {
        sharedViewModel.stopObservingConnection()
        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_status, menu)
        // A fix for action with custom layout
        // https://stackoverflow.com/a/35265797
        val item = menu.findItem(action_open_status_dialog)
        item.actionView.setOnClickListener { onOptionsItemSelected(item) }

        val pbSyncProgress = item.actionView.findViewById<ProgressBar>(R.id.pb_sync_progress)
        val ivStatus = item.actionView.findViewById<ImageView>(R.id.iv_status)

        sharedViewModel.getNetworkStatus().observe(viewLifecycleOwner, Observer { available ->
            val drawable = if (available) R.drawable.ic_online else R.drawable.ic_offline
            ivStatus.simpleDrawable(drawable)
        })

        sharedViewModel.syncNeededLD.observe(viewLifecycleOwner, Observer {
            item.actionView.iv_pending_changes.visibility = if (it) View.VISIBLE else View.INVISIBLE
        })

        // show sync in toolbar only on settings screen, because there is no other progress indicator when country is updated
        sharedViewModel.syncState.observe(viewLifecycleOwner, Observer {
            pbSyncProgress.visible(it.isLoading && mainNavController.currentDestination?.id == R.id.settingsFragment)
        })
        onDestinationChangedListener = NavController.OnDestinationChangedListener { _, destination, _ ->
            pbSyncProgress.visible(destination.id == R.id.settingsFragment && sharedViewModel.syncState.value?.isLoading == true)
        }
        mainNavController.addOnDestinationChangedListener(onDestinationChangedListener)

        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onDestroyOptionsMenu() {
        mainNavController.removeOnDestinationChangedListener(onDestinationChangedListener)
        super.onDestroyOptionsMenu()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            action_open_status_dialog -> {
                mainNavController.navigate(R.id.uploadDialog)
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    private fun showToast(text: String) {
        val toastView = layoutInflater.inflate(R.layout.custom_toast, null)
        val tvMessage = toastView.findViewById<TextView>(R.id.tv_toast)
        tvMessage.text = text
        val toast = Toast(context)
        toast.setGravity(Gravity.BOTTOM, 0, 50)
        toast.duration = Toast.LENGTH_SHORT
        toast.view = toastView
        toast.show()
    }
}
