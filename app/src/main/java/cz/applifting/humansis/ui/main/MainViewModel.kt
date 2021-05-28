package cz.applifting.humansis.ui.main

import android.content.SharedPreferences
import androidx.lifecycle.MutableLiveData
import cz.applifting.humansis.managers.LoginManager
import cz.applifting.humansis.misc.ApiEnvironments
import cz.applifting.humansis.model.db.User
import cz.applifting.humansis.ui.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Created by Petr Kubes <petr.kubes@applifting.cz> on 21, August, 2019
 */
class MainViewModel @Inject constructor(
    private val loginManager: LoginManager,
    private val sp: SharedPreferences
) : BaseViewModel() {

    val userLD = MutableLiveData<User>()
    val environmentLD = MutableLiveData<String>()

    init {
        launch {
            val user = loginManager.retrieveUser()
            userLD.value = user
        }
        launch() {
            environmentLD.value = sp.getString(cz.applifting.humansis.ui.login.SP_ENVIRONMENT, ApiEnvironments.STAGE.name)
        }
    }

    fun logout() {
        launch(Dispatchers.IO) {
            loginManager.logout()
            userLD.postValue(null)
        }
    }
}