package cz.applifting.humansis.managers

import android.content.Context
import androidx.lifecycle.MediatorLiveData

interface ToastManager {

    fun getToastMessageLiveData(): MediatorLiveData<String?>

    fun setToastMessage(text: String)

    fun setToastMessage(stringResId: Int)

    fun removeToastMessage()
}

class ToastManagerImpl(val context: Context) : ToastManager {

    private val toastMessageLD = MediatorLiveData<String?>()

    override fun getToastMessageLiveData(): MediatorLiveData<String?> {
        return toastMessageLD
    }

    override fun setToastMessage(text: String) {
        toastMessageLD.postValue(text)
    }

    override fun setToastMessage(stringResId: Int) {
        toastMessageLD.postValue(context.getString(stringResId))
    }

    override fun removeToastMessage() {
        toastMessageLD.postValue(null) // To prevent showing it again on resume
    }
}