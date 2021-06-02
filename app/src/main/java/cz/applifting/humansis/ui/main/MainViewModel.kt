package cz.applifting.humansis.ui.main

import android.content.SharedPreferences
import androidx.lifecycle.MutableLiveData
import cz.applifting.humansis.managers.LoginManager
import cz.applifting.humansis.misc.ApiEnvironments
import cz.applifting.humansis.misc.NfcTagPublisher
import cz.applifting.humansis.misc.SingleLiveEvent
import cz.applifting.humansis.model.db.User
import cz.applifting.humansis.ui.App
import cz.applifting.humansis.ui.BaseViewModel
import cz.quanti.android.nfc.PINFacade
import cz.quanti.android.nfc.dto.UserBalance
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Created by Petr Kubes <petr.kubes@applifting.cz> on 21, August, 2019
 */
class MainViewModel @Inject constructor(
    private val loginManager: LoginManager,
    private val sp: SharedPreferences,
    app: App
) : BaseViewModel(app) {

    @Inject
    lateinit var nfcTagPublisher: NfcTagPublisher
    @Inject
    lateinit var pinFacade: PINFacade

    val userLD = MutableLiveData<User>()
    val environmentLD = MutableLiveData<String>()

    val readBalanceResult = SingleLiveEvent<UserBalance>()
    val readBalanceError = SingleLiveEvent<Throwable>()
    val initializeCardResult = SingleLiveEvent<UserBalance>()
    val initializeCardError = SingleLiveEvent<Throwable>()

    init {
        launch {
            val user = loginManager.retrieveUser()
            userLD.value = user
        }
        launch {
            environmentLD.value = sp.getString(cz.applifting.humansis.ui.login.SP_ENVIRONMENT, ApiEnvironments.STAGE.name)
        }
    }

    fun readBalance(): Disposable {
        return nfcTagPublisher.getTagObservable().firstOrError().flatMap{ tag ->
            pinFacade.readUserBalance(tag)
        }.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe ({
            readBalanceResult.postValue(it)
        },{
            readBalanceError.postValue(it)
        })
    }

    fun initializeCard(): Disposable {
        return nfcTagPublisher.getTagObservable().firstOrError().flatMap{ tag ->
            pinFacade.readUserBalance(tag)
        }.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe ({
            initializeCardResult.postValue(it)
        },{
            initializeCardError.postValue(it)
        })
    }

    fun logout() {
        launch(Dispatchers.IO) {
            loginManager.logout()
            userLD.postValue(null)
        }
    }
}