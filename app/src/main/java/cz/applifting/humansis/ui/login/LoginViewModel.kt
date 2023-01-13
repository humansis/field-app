package cz.applifting.humansis.ui.login

import android.content.SharedPreferences
import android.view.View
import androidx.lifecycle.MutableLiveData
import cz.applifting.humansis.api.HumansisService
import cz.applifting.humansis.api.interceptor.HostUrlInterceptor
import cz.applifting.humansis.api.parseError
import cz.applifting.humansis.managers.LoginManager
import cz.applifting.humansis.misc.ApiEnvironment
import cz.applifting.humansis.misc.HumansisError
import cz.applifting.humansis.misc.getDefaultEnvironment
import cz.applifting.humansis.model.User
import cz.applifting.humansis.model.api.LoginRequest
import cz.applifting.humansis.ui.App
import cz.applifting.humansis.ui.BaseViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch
import quanti.com.kotlinlog.Log
import retrofit2.HttpException

const val SP_ENVIRONMENT_NAME = "pin_field_app_env_name"
const val SP_ENVIRONMENT_URL = "pin_field_app_env_url"

/**
 * Created by Petr Kubes <petr.kubes@applifting.cz> on 17, August, 2019
 */
class LoginViewModel @Inject constructor(
    private val hostUrlInterceptor: HostUrlInterceptor,
    private val service: HumansisService,
    private val loginManager: LoginManager,
    private val sp: SharedPreferences,
    app: App
) : BaseViewModel(app) {

    val viewStateLD = MutableLiveData<LoginViewState>()
    val loginLD = MutableLiveData<User?>()

    init {
        loginLD.value = null
        viewStateLD.value = LoginViewState()
        launch {
            loginLD.value = loginManager.retrieveUser()
        }
    }

    fun changeHostUrl(host: ApiEnvironment) {
        hostUrlInterceptor.setHost(host)
        sp.edit().putString(SP_ENVIRONMENT_NAME, host.title).apply()
        sp.edit().putString(SP_ENVIRONMENT_URL, host.url).apply()
    }

    fun loadHostFromSaved(): ApiEnvironment {
        return sp.getString(SP_ENVIRONMENT_NAME, null)?.let { name ->
            sp.getString(SP_ENVIRONMENT_URL, null)?.let { url ->
                ApiEnvironment.find(name, url)
            }
        } ?: getDefaultEnvironment()
    }

    fun login(username: String, password: String, loginFinishCallback: LoginFinishCallback) {
        launch {
            viewStateLD.value = LoginViewState(
                btnLoginVisibility = View.GONE,
                pbLoadingVisible = View.VISIBLE
            )

            try {
                val userResponse = service.postLogin(
                    LoginRequest(
                        password = password,
                        username = username
                    )
                )

                val user = loginManager.login(userResponse, password.toByteArray())
                loginLD.value = user
                Log.d(TAG, "Logged in as ${user.username}")
            } catch (e: HumansisError) {
                viewStateLD.value = createViewStateErrorOnLogin(e.message)
                loginFinishCallback.finishLogin(true)
                Log.d(TAG, "Log in error ${e.message}")
            } catch (e: HttpException) {
                val message = parseError(e, getApplication())
                viewStateLD.value = createViewStateErrorOnLogin(message)
                loginFinishCallback.finishLogin(true)
                Log.d(TAG, "Log in error $message")
            }
            loginFinishCallback.finishLogin(true)
        }
    }

    private fun createViewStateErrorOnLogin(errorMessage: String?) =
        // keep username disabled when login screen was reached after receiving 403 on sync
        LoginViewState(errorMessage = errorMessage).let { state ->
            loginLD.value?.let { state.copy(etUsernameIsEnabled = true) } ?: state
        }

    companion object {
        private val TAG = LoginViewModel::class.java.simpleName
    }
}