package cz.applifting.humansis.ui.main.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.MutableLiveData
import cz.applifting.humansis.R
import cz.applifting.humansis.extensions.suspendCommit
import cz.applifting.humansis.managers.LoginManager
import cz.applifting.humansis.managers.SP_COUNTRY
import cz.applifting.humansis.managers.SP_FIRST_COUNTRY_DOWNLOAD
import cz.applifting.humansis.model.Country
import cz.applifting.humansis.repositories.ProjectsRepository
import cz.applifting.humansis.ui.App
import cz.applifting.humansis.ui.BaseViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Created by Vaclav Legat <vaclav.legat@applifting.cz>
 * @since 9. 10. 2019
 */

class SettingsViewModel @Inject constructor(
    private val sp: SharedPreferences,
    private val projectsRepository: ProjectsRepository,
    private val loginManager: LoginManager,
    app: App
) : BaseViewModel(app) {
    val savedLD: MutableLiveData<Boolean> = MutableLiveData()

    suspend fun getCountries(context: Context?): List<Country> {
        return loginManager.getCountries().map { Country(it, translateCountry(it, context)) }
    }

    fun getCountrySettings(): String {
        return sp.getString(SP_COUNTRY, "") ?: ""
    }

    fun test() {
        sp.edit().putBoolean("test", !sp.getBoolean("test", false)).apply()
    }

    fun updateCountrySettings(country: String) {
        val oldCountry = sp.getString(SP_COUNTRY, null)
        if (oldCountry == country) {
            return
        }

        launch {
            with(sp.edit()) {
                putString(SP_COUNTRY, country)
                putBoolean(SP_FIRST_COUNTRY_DOWNLOAD, true)
                suspendCommit()
            }

            // Delete all projects to not show old data when connection breaks during switch
            projectsRepository.deleteAll()
            savedLD.value = true
        }
    }

    private fun translateCountry(iso3: String, context: Context?): String {
        return when (iso3) {
            "SYR" -> {
                context?.getString(R.string.SYR) ?: iso3
            }
            "KHM" -> {
                context?.getString(R.string.KHM) ?: iso3
            }
            "UKR" -> {
                context?.getString(R.string.UKR) ?: iso3
            }
            "ETH" -> {
                context?.getString(R.string.ETH) ?: iso3
            }
            "MNG" -> {
                context?.getString(R.string.MNG) ?: iso3
            }
            "ARM" -> {
                context?.getString(R.string.ARM) ?: iso3
            }
            "ZMB" -> {
                context?.getString(R.string.ZMB) ?: iso3
            }
            else -> {
                iso3
            }
        }
    }
}