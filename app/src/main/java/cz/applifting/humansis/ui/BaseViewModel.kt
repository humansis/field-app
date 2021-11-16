package cz.applifting.humansis.ui

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.CoroutineContext

/**
 * Created by Petr Kubes <petr.kubes@applifting.cz> on 17, August, 2019
 */
abstract class BaseViewModel(app: App) : AndroidViewModel(app), CoroutineScope {
    override val coroutineContext: CoroutineContext = viewModelScope.coroutineContext
}