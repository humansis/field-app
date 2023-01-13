package cz.applifting.humansis.ui.main

import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import cz.applifting.humansis.R
import cz.applifting.humansis.extensions.setDate
import cz.applifting.humansis.managers.LoginManager
import cz.applifting.humansis.managers.ToastManager
import cz.applifting.humansis.misc.ApiEnvironment
import cz.applifting.humansis.misc.NfcTagPublisher
import cz.applifting.humansis.misc.SingleLiveEvent
import cz.applifting.humansis.model.User
import cz.applifting.humansis.ui.App
import cz.applifting.humansis.ui.BaseViewModel
import cz.applifting.humansis.ui.login.SP_ENVIRONMENT_NAME
import cz.applifting.humansis.ui.login.SP_ENVIRONMENT_URL
import cz.quanti.android.nfc.OfflineFacade
import cz.quanti.android.nfc.dto.v2.UserPinBalance
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.util.Date
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Created by Petr Kubes <petr.kubes@applifting.cz> on 21, August, 2019
 */
class MainViewModel @Inject constructor(
    private val loginManager: LoginManager,
    private val toastManager: ToastManager,
    private val sp: SharedPreferences,
    app: App
) : BaseViewModel(app) {

    @Inject
    lateinit var nfcTagPublisher: NfcTagPublisher
    @Inject
    lateinit var pinFacade: OfflineFacade

    val userLD = MutableLiveData<User?>()
    val environmentLD = MutableLiveData<String>()

    val readBalanceResult = SingleLiveEvent<UserPinBalance>()
    val readBalanceError = SingleLiveEvent<Throwable>()
    val initializeCardResult = SingleLiveEvent<UserPinBalance>()
    val initializeCardError = SingleLiveEvent<Throwable>()

    val enqueueSynchronization = SingleLiveEvent<Unit>()

    init {
        launch {
            userLD.value = loginManager.retrieveUser()
        }
        launch {
            environmentLD.value = sp.getString(SP_ENVIRONMENT_NAME, ApiEnvironment.Stage.title)
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

    fun getHostUrl(): ApiEnvironment {
        return sp.getString(SP_ENVIRONMENT_NAME, null)?.let { name ->
            sp.getString(SP_ENVIRONMENT_URL, null)?.let { url ->
                ApiEnvironment.find(name, url)
            }
        } ?: ApiEnvironment.Stage // fallback to stage, because environment was not saved to SP when no environment was selected in debug builds on login screen until v3.4.1
    }

    fun validateTokens(): Boolean {
        userLD.value.let { user ->
            val authToken = user?.token
            val refreshToken = user?.refreshToken
            val refreshTokenExpiration = user?.refreshTokenExpiration?.toLong()
            return if (authToken == null || authToken.isExpired()) {
                if (refreshToken == null || refreshTokenExpiration == null || refreshTokenExpiration < Date().time) {
                    invalidateTokens()
                    false
                } else {
                    true
                }
            } else {
                true
            }
        }
    }

    private fun invalidateTokens() {
        launch(Dispatchers.IO) {
            Log.d(TAG, "You have been logged out because your authentication tokens have expired or are missing.")
            setToastMessage(R.string.token_missing_or_expired)
            loginManager.invalidateTokens()
            sp.setDate(LAST_DOWNLOAD_KEY, null)
            userLD.postValue(null)
        }
    }

    fun getToastMessageLiveData(): LiveData<String?> {
        return toastManager.getToastMessageLiveData()
    }

    fun setToastMessage(text: String) {
        toastManager.setToastMessage(text)
    }

    private fun setToastMessage(stringResId: Int) {
        toastManager.setToastMessage(stringResId)
    }

    fun logout() {
        launch(Dispatchers.IO) {
            loginManager.logout()
            userLD.postValue(null)
        }
    }

    companion object {
        private val TAG = MainViewModel::class.java.simpleName
    }

}