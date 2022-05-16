package cz.applifting.humansis.ui.splash

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import cz.applifting.humansis.R
import cz.applifting.humansis.model.User
import cz.applifting.humansis.ui.App
import kotlinx.coroutines.*
import quanti.com.kotlinlog.Log
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

/**
 * Created by Petr Kubes <petr.kubes@applifting.cz> on 11, September, 2019
 */
class SplashFragment : Fragment(), CoroutineScope {

    private val job = Job()
    override val coroutineContext: CoroutineContext = job + Dispatchers.Main

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private val viewModel: SplashViewModel by viewModels { viewModelFactory }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity?.application as App).appComponent.inject(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_splash, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        if (!viewModel.initDB()) {
            Log.d(TAG, "DB not initialized ")
            goToLoginScreen()
        } else {
            Log.d(TAG, "DB initialized")
            viewModel.getUser()
        }

        viewModel.userLD.observe(viewLifecycleOwner) {
            if (it == null) {
                Log.d(TAG, "Application navigated to login screen because userLD.value == null.")
                goToLoginScreen()
            } else {
                if (it.token == null) {
                    Log.d(TAG, "Application navigated to login screen because token == null.")
                    goToLoginScreen()
                } else {
                    Log.d(TAG, "Application navigated to main screen because user ${it.username} has active session")
                    goToMainScreen(it)
                }
            }
        }
    }

    private fun goToLoginScreen() {
        launch {
            delay(1000)
            val action = SplashFragmentDirections.actionSplashFragmentToLoginFragment()
            findNavController().navigate(action)
        }
    }

    private fun goToMainScreen(user: User) {
        launch {
            delay(1000)
            val action = SplashFragmentDirections.actionSplashFragmentToMainFragment(
                user.username,
                user.email
            )
            findNavController().navigate(action)
        }
    }

    companion object {
        private val TAG = SplashFragment::class.java.simpleName
    }
}