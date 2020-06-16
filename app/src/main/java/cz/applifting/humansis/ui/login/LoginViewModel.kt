package cz.applifting.humansis.ui.login

import android.content.Context
import android.content.SharedPreferences
import android.view.View
import androidx.lifecycle.MutableLiveData
import cz.applifting.humansis.api.HostUrlInterceptor
import cz.applifting.humansis.api.HumansisService
import cz.applifting.humansis.api.parseError
import cz.applifting.humansis.managers.LoginManager
import cz.applifting.humansis.misc.ApiEnvironments
import cz.applifting.humansis.misc.HumansisError
import cz.applifting.humansis.misc.hashAndSaltPassword
import cz.applifting.humansis.model.api.LoginReqRes
import cz.applifting.humansis.model.db.User
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
        private val context: Context,
        private val sp: SharedPreferences
) : BaseViewModel() {

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
        val host = ApiEnvironments.valueOf(sp.getString(SP_ENVIRONMENT, ApiEnvironments.BASE.name))
        hostUrlInterceptor.setHost(host)
        return host
    }

    fun login(username: String, password: String) {
        launch {
            viewStateLD.value = LoginViewState(
                btnLoginVisibility = View.GONE,
                pbLoadingVisible = View.VISIBLE
            )

            try {
                val saltResponse = service.getSalt(username)
                val hashedPassword = hashAndSaltPassword(saltResponse.salt, password)
                val userResponse = service.postLogin(
                    LoginReqRes(
                        changePassword = true,
                        email = username,
                        password = hashedPassword,
                        username = username
                    )
                )

                val user = loginManager.login(userResponse, password.toByteArray())
                loginLD.value = user
            } catch (e: HumansisError) {
                viewStateLD.value = createViewStateErrorOnLogin(e.message)
            } catch (e: HttpException) {
                val message = parseError(e, context)
                viewStateLD.value = createViewStateErrorOnLogin(message)
            }
        }
    }

    private fun createViewStateErrorOnLogin(errorMessage: String?) =
        // keep username disabled when login screen was reached after receiving 403 on sync
        LoginViewState(errorMessage = errorMessage).let { state ->
            loginLD.value?.let { state.copy(etUsernameIsEnabled = true) } ?: state
        }
}