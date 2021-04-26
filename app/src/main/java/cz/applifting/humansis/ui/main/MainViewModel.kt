package cz.applifting.humansis.ui.main

import androidx.lifecycle.MutableLiveData
import cz.applifting.humansis.managers.LoginManager
import cz.applifting.humansis.misc.NfcTagPublisher
import cz.applifting.humansis.model.db.User
import cz.applifting.humansis.ui.BaseViewModel
import cz.quanti.android.nfc.OfflineFacade
import cz.quanti.android.nfc.PINFacade
import cz.quanti.android.nfc.dto.UserBalance
import io.reactivex.Single
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Created by Petr Kubes <petr.kubes@applifting.cz> on 21, August, 2019
 */
class MainViewModel @Inject constructor(
    private val loginManager: LoginManager
) : BaseViewModel() {

    @Inject
    lateinit var nfcTagPublisher: NfcTagPublisher
    @Inject
    lateinit var pinFacade: PINFacade
    @Inject
    lateinit var offlineFacade: OfflineFacade

    val userLD = MutableLiveData<User>()

    init {
        launch {
            val user = loginManager.retrieveUser()
            userLD.value = user
        }
    }

    fun logout() {
        launch(Dispatchers.IO) {
            loginManager.logout()
            userLD.postValue(null)
        }
    }

    fun readBalance(): Single<UserBalance> {
        return nfcTagPublisher.getTagObservable().firstOrError().flatMap{ tag ->
            pinFacade.readUserBalance(tag)
        }
    }
}