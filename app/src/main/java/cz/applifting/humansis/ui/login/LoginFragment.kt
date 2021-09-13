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
import cz.applifting.humansis.misc.ApiEnvironments
import cz.applifting.humansis.ui.App
import kotlinx.android.synthetic.main.fragment_login.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity?.application as App).appComponent.inject(this)
        val navController = findNavController()

        settingsButtonInit()

        btn_login.isEnabled = true
        btn_login.setOnClickListener {
            val username = et_username.text.toString()
            btn_login.isEnabled = false
            if(username.equals(BuildConfig.DEMO_ACCOUNT, true)) {
                changeEnvironment(ApiEnvironments.STAGE)
            }
            viewModel.login(username, et_password.text.toString(), this)
        }

        viewModel.viewStateLD.observe(viewLifecycleOwner, { viewState ->
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
        })

        viewModel.loginLD.observe(viewLifecycleOwner, {
            if (it != null && !it.invalidPassword) {
                val action = LoginFragmentDirections.actionLoginFragmentToMainFragment(it.email, it.username)
                navController.navigate(action)
            } else if (it == null) {
                et_username.isEnabled = true
            } else {
                tv_error.text = getString(R.string.auth_expiration_explanation)
                tv_error.visibility = View.VISIBLE
                et_username.setText("")
                et_password.setText("")
            }
        })
    }

    private fun settingsButtonInit() {
        if (BuildConfig.DEBUG) {
            settingsImageView.visibility = View.VISIBLE
            envTextView.visibility = View.VISIBLE

            val host = viewModel.loadHostFromSaved()
            envTextView.text = host.name

            settingsImageView.setOnClickListener {
                val contextThemeWrapper =
                        ContextThemeWrapper(requireContext(), R.style.PopupMenuTheme)
                val popup = PopupMenu(contextThemeWrapper, settingsImageView)
                popup.inflate(R.menu.api_urls_menu)
                popup.menu.add(0, ApiEnvironments.FRONT.id, 0, "FRONT API")
                popup.menu.add(0, ApiEnvironments.DEMO.id, 0, "DEMO API")
                popup.menu.add(0, ApiEnvironments.STAGE.id, 0, "STAGE API")
                popup.menu.add(0, ApiEnvironments.DEV.id, 0, "DEV API")
                popup.menu.add(0, ApiEnvironments.TEST.id, 0, "TEST API")
                popup.setOnMenuItemClickListener { item ->
                    val env = ApiEnvironments.values().find { it.id == item?.itemId }
                    changeEnvironment(env)
                    true
                }
                popup.show()
            }
        } else {
            settingsImageView.visibility = View.INVISIBLE
            envTextView.visibility = View.INVISIBLE
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.viewStateLD.value = LoginViewState()
    }

    private fun changeEnvironment(env: ApiEnvironments?) {
        val newEnv = env ?: ApiEnvironments.STAGE
        envTextView.text = newEnv.name
        viewModel.changeHostUrl(newEnv)
    }

    override fun finishLogin(enableButton: Boolean) {
        btn_login?.isEnabled = enableButton
    }
}