package cz.applifting.humansis.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import cz.applifting.humansis.ui.main.SharedViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

/**
 * Created by Petr Kubes <petr.kubes@applifting.cz> on 14, August, 2019
 */
open class BaseFragment: Fragment(), CoroutineScope {

    private val job = Job()
    override val coroutineContext: CoroutineContext = job + Dispatchers.Main

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    // TODO decide whether to share one or multiple viewmodels as more stuff is added
    lateinit var sharedViewModel: SharedViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity?.application as App).appComponent.inject(this)
        sharedViewModel = ViewModelProvider(activity as HumansisActivity, viewModelFactory).get(SharedViewModel::class.java)
    }
}