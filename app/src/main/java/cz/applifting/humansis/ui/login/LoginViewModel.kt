package cz.applifting.humansis.ui.login

import android.content.SharedPreferences
import android.view.View
import androidx.lifecycle.MutableLiveData
import cz.applifting.humansis.api.interceptor.HostUrlInterceptor
import cz.applifting.humansis.api.HumansisService
import cz.applifting.humansis.api.parseError
import cz.applifting.humansis.managers.LoginManager
import cz.applifting.humansis.misc.ApiEnvironments
import cz.applifting.humansis.misc.HumansisError
import cz.applifting.humansis.model.User
import cz.applifting.humansis.model.api.LoginRequest
import cz.applifting.humansis.ui.App
import cz.applifting.humansis.ui.BaseViewModel
import kotlinx.coroutines.launch
import retrofit2.HttpException
import javax.inject.Inject

const val SP_ENVIRONMENT = "pin_offline_app_api_url"

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
    val loginLD = MutableLiveData<User>()

    init {
        loginLD.value = null
        viewStateLD.value = LoginViewState()
        launch {
            loginLD.value = loginManager.retrieveUser()
        }
    }

    fun changeHostUrl(host: ApiEnvironments?) {
        hostUrlInterceptor.setHost(host)
        sp.edit()?.putString(SP_ENVIRONMENT, host?.name)?.apply()
    }

    fun loadHostFromSaved(): ApiEnvironments {
        val host = try {
            ApiEnvironments.valueOf(
                sp.getString(SP_ENVIRONMENT, ApiEnvironments.STAGE.name) ?: ApiEnvironments.STAGE.name
            )
        } catch (e: Exception) {
            ApiEnvironments.STAGE
        }
        hostUrlInterceptor.setHost(host)
        return host
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
            } catch (e: HumansisError) {
                viewStateLD.value = createViewStateErrorOnLogin(e.message)
                loginFinishCallback.finishLogin(true)
            } catch (e: HttpException) {
                val message = parseError(e, getApplication())
                viewStateLD.value = createViewStateErrorOnLogin(message)
                loginFinishCallback.finishLogin(true)
            }
            loginFinishCallback.finishLogin(true)
        }
    }

    private fun createViewStateErrorOnLogin(errorMessage: String?) =
        // keep username disabled when login screen was reached after receiving 403 on sync
        LoginViewState(errorMessage = errorMessage).let { state ->
            loginLD.value?.let { state.copy(etUsernameIsEnabled = true) } ?: state
        }
}