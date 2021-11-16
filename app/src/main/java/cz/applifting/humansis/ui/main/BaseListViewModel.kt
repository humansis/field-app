package cz.applifting.humansis.ui.main

import android.app.Application
import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import cz.applifting.humansis.R
import cz.applifting.humansis.ui.App
import cz.applifting.humansis.ui.BaseViewModel
import cz.applifting.humansis.ui.components.listComponent.ListComponentState

/**
 * Created by Petr Kubes <petr.kubes@applifting.cz> on 10, September, 2019
 */
abstract class BaseListViewModel(
    app: App
) : BaseViewModel(app) {

    val listStateLD: LiveData<ListComponentState>
        get() = _listStateLD

    private val _listStateLD = MutableLiveData<ListComponentState>()

    init {
        _listStateLD.value = ListComponentState()
    }

    fun showRefreshing(show: Boolean, hasData: Boolean = true, isFirstdownload: Boolean = false) {
        _listStateLD.value = if (isFirstdownload) {
            _listStateLD.value?.copy(isRefreshing = show, text = getApplication<Application>().applicationContext.getString(R.string.downloading))
        } else {
            _listStateLD.value?.copy(isRefreshing = show, text = getText(hasData))
        }
    }

    fun showRetrieving(show: Boolean) {
        _listStateLD.value = _listStateLD.value?.copy(isRetrieving = show)
    }

    fun showError(show: Boolean) {
        val old = listStateLD.value

        _listStateLD.value = old?.copy(
            isError = show, text = if (show) {
                (getApplication() as Context).getString(R.string.sync_error)
            } else {
                old.text
            }
        )
    }

    private fun getText(hasData: Boolean): String? = if (hasData) null else (getApplication() as Context).getString(R.string.no_data_message)
}