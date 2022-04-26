package cz.applifting.humansis.ui.splash

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import cz.applifting.humansis.R
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
            Log.v(TAG, "DB not initialized ")
            goToLoginScreen()
        } else {
            Log.v(TAG, "DB initialized")
            viewModel.getUser()
        }

        viewModel.userLD.observe(viewLifecycleOwner, Observer {
            if (it == null) {
                Log.v(TAG, "Application navigated to login screen because userLD.value == null.")
                goToLoginScreen()
                return@Observer
            }

            val action = SplashFragmentDirections.actionSplashFragmentToMainFragment(
                it.username,
                it.email
            )
            this.findNavController().navigate(action)
        })
    }

    private fun goToLoginScreen() {
        launch {
            delay(1000)
            val action = SplashFragmentDirections.actionSplashFragmentToLoginFragment()
            findNavController().navigate(action)
        }
    }

    companion object {
        private val TAG = SplashFragment::class.java.simpleName
    }
}