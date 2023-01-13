package cz.applifting.humansis.managers

import android.content.Context
import androidx.lifecycle.MediatorLiveData

interface ToastManager {

    fun getToastMessageLiveData(): MediatorLiveData<String?>

    fun setToastMessage(text: String)

    fun setToastMessage(stringResId: Int)
}

class ToastManagerImpl(val context: Context) : ToastManager {

    private val toastMessageLD = MediatorLiveData<String?>()

    override fun getToastMessageLiveData(): MediatorLiveData<String?> {
        return toastMessageLD
    }

    override fun setToastMessage(text: String) {
        toastMessageLD.value = text
        toastMessageLD.value = null // To prevent showing it again on resume
    }

    override fun setToastMessage(stringResId: Int) {
        toastMessageLD.value = context.getString(stringResId)
        toastMessageLD.value = null // To prevent showing it again on resume
    }
}