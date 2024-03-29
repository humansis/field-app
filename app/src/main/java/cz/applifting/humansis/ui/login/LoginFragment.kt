package cz.applifting.humansis.ui.login

import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import cz.applifting.humansis.BuildConfig
import cz.applifting.humansis.R
import cz.applifting.humansis.misc.ApiEnvironment
import cz.applifting.humansis.misc.SendLogDialogFragment
import cz.applifting.humansis.ui.App
import kotlinx.android.synthetic.main.fragment_login.btn_login
import kotlinx.android.synthetic.main.fragment_login.envTextView
import kotlinx.android.synthetic.main.fragment_login.et_password
import kotlinx.android.synthetic.main.fragment_login.et_username
import kotlinx.android.synthetic.main.fragment_login.loginLogo
import kotlinx.android.synthetic.main.fragment_login.pb_loading
import kotlinx.android.synthetic.main.fragment_login.settingsImageView
import kotlinx.android.synthetic.main.fragment_login.tv_error
import kotlinx.android.synthetic.main.fragment_login.verTextView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import quanti.com.kotlinlog.Log
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

/**
 * Created by Petr Kubes <petr.kubes@applifting.cz> on 15, August, 2019
 */
class LoginFragment : Fragment(), CoroutineScope, LoginFinishCallback {

    private val job = Job()
    override val coroutineContext: CoroutineContext = job + Dispatchers.Main

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val viewModel: LoginViewModel by viewModels {
        viewModelFactory
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        (activity?.application as App).appComponent.inject(this)
        val navController = findNavController()

        settingsButtonInit()

        btn_login.isEnabled = true
        btn_login.setOnClickListener {
            Log.d(TAG, "Login button clicked")
            val username = et_username.text.toString()
            btn_login.isEnabled = false
            if (username.equals(BuildConfig.DEMO_ACCOUNT, true)) {
                changeEnvironment(ApiEnvironment.Stage)
            }
            viewModel.login(username, et_password.text.toString(), this)
        }

        verTextView.text = getString(R.string.version, BuildConfig.VERSION_NAME)

        loginLogo.setOnLongClickListener {
            SendLogDialogFragment.newInstance(
                sendEmailAddress = getString(R.string.send_email_adress),
                title = getString(R.string.logs_dialog_title),
                message = getString(R.string.logs_dialog_message),
                emailButtonText = getString(R.string.logs_dialog_email_button),
                dialogTheme = R.style.DialogTheme
            ).show(requireActivity().supportFragmentManager, "TAG")
            // TODO inside this method in kotlinlogger there is a method getZipOfFiles() that automatically deletes all logs older than 4 days
            return@setOnLongClickListener true
        }

        viewModel.viewStateLD.observe(viewLifecycleOwner) { viewState ->
            et_username.isEnabled = viewState.etUsernameIsEnabled
            et_password.isEnabled = viewState.etPasswordIsEnabled
            btn_login.visibility = viewState.btnLoginVisibility
            pb_loading.visibility = viewState.pbLoadingVisible

            if (viewState.errorMessage != null) {
                tv_error.visibility = View.VISIBLE
                tv_error.text = viewState.errorMessage
            } else {
                tv_error.visibility = View.GONE
            }
        }

        viewModel.loginLD.observe(viewLifecycleOwner) {
            if (it?.token != null && !it.shouldReauthenticate) {
                val action =
                    LoginFragmentDirections.actionLoginFragmentToMainFragment(it.email, it.username)
                navController.navigate(action)
            } else if (it == null) {
                et_username.isEnabled = true
            } else {
                tv_error.text = getString(R.string.auth_expiration_explanation)
                tv_error.visibility = View.VISIBLE
                et_username.setText("")
                et_password.setText("")
            }
        }
    }

    private fun settingsButtonInit() {
        changeEnvironment(viewModel.loadHostFromSaved())

        settingsImageView.setOnClickListener {
            Log.d(TAG, "Settings button clicked")
            val contextThemeWrapper =
                ContextThemeWrapper(requireContext(), R.style.PopupMenuTheme)
            val popup = PopupMenu(contextThemeWrapper, settingsImageView)
            popup.inflate(R.menu.api_urls_menu)
            val environments = ApiEnvironment.createEnvironments(requireContext())
            environments.forEach {
                popup.menu.add(0, it.id, 0, it.title)
            }

            popup.setOnMenuItemClickListener { item ->
                environments.find { it.id == item?.itemId }?.let {
                    changeEnvironment(it)
                }
                true
            }
            popup.show()
        }

        if (BuildConfig.DEBUG) {
            settingsImageView.visibility = View.VISIBLE
            envTextView.visibility = View.VISIBLE
        } else {
            settingsImageView.visibility = View.INVISIBLE
            envTextView.visibility = View.INVISIBLE

            verTextView.setOnLongClickListener {
                settingsImageView.visibility = View.VISIBLE
                envTextView.visibility = View.VISIBLE
                return@setOnLongClickListener true
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.viewStateLD.value = LoginViewState()
    }

    private fun changeEnvironment(env: ApiEnvironment) {
        envTextView.text = env.title
        viewModel.changeHostUrl(env)
    }

    override fun finishLogin(enableButton: Boolean) {
        btn_login?.isEnabled = enableButton
    }

    companion object {
        private val TAG = LoginFragment::class.java.simpleName
    }
}