package cz.applifting.humansis.ui.main

import android.content.SharedPreferences
import androidx.lifecycle.MutableLiveData
import cz.applifting.humansis.managers.LoginManager
import cz.applifting.humansis.misc.ApiEnvironments
import cz.applifting.humansis.misc.NfcTagPublisher
import cz.applifting.humansis.misc.SingleLiveEvent
import cz.applifting.humansis.model.JWToken
import cz.applifting.humansis.model.User
import cz.applifting.humansis.ui.App
import cz.applifting.humansis.ui.BaseViewModel
import cz.applifting.humansis.ui.login.SP_ENVIRONMENT
import cz.quanti.android.nfc.OfflineFacade
import cz.quanti.android.nfc.dto.v2.UserPinBalance
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
    lateinit var pinFacade: OfflineFacade

    val userLD = MutableLiveData<User>()
    val environmentLD = MutableLiveData<String>()
    var authToken: JWToken? = null

    val readBalanceResult = SingleLiveEvent<UserPinBalance>()
    val readBalanceError = SingleLiveEvent<Throwable>()
    val initializeCardResult = SingleLiveEvent<UserPinBalance>()
    val initializeCardError = SingleLiveEvent<Throwable>()

    init {
        launch {
            val user = loginManager.retrieveUser()
            userLD.value = user
            authToken = user?.token
        }
        launch {
            environmentLD.value = sp.getString(SP_ENVIRONMENT, ApiEnvironments.STAGE.name)
        }
    }

    fun readBalance(): Disposable {
        return nfcTagPublisher.getTagObservable().firstOrError().flatMap { tag ->
            pinFacade.readProtectedBalanceForUser(tag)
        }.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe({
            readBalanceResult.postValue(it)
        }, {
            readBalanceError.postValue(it)
        })
    }

    fun initializeCard(): Disposable {
        return nfcTagPublisher.getTagObservable().firstOrError().flatMap { tag ->
            pinFacade.readProtectedBalanceForUser(tag)
        }.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe({
            initializeCardResult.postValue(it)
        }, {
            initializeCardError.postValue(it)
        })
    }

    fun getHostUrl(): ApiEnvironments {
        return ApiEnvironments.valueOf(sp.getString(SP_ENVIRONMENT, ApiEnvironments.STAGE.name) ?: ApiEnvironments.STAGE.name)
    }

    fun logout() {
        launch(Dispatchers.IO) {
            loginManager.logout()
            userLD.postValue(null)
        }
    }
}