package cz.applifting.humansis.ui

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import cz.applifting.humansis.extensions.reObserve

abstract class BaseActivity : AppCompatActivity() {

    protected fun <T> observe(liveData: LiveData<T>, observeFun: (T) -> Unit) {
        liveData.reObserve(this) {
            it.let { observeFun.invoke(it) }
        }
    }
}