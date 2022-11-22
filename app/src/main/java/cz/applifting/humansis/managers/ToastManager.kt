package cz.applifting.humansis.managers

import androidx.lifecycle.MediatorLiveData

interface ToastManager {

    fun getToastMessageLiveData(): MediatorLiveData<String?>

    fun setToastMessage(text: String?)
}

class ToastManagerImpl : ToastManager {

    private val toastMessageLD = MediatorLiveData<String?>()

    override fun getToastMessageLiveData(): MediatorLiveData<String?> {
        return toastMessageLD
    }

    override fun setToastMessage(text: String?) {
        toastMessageLD.value = text
        toastMessageLD.value = null // To prevent showing it again on resume
    }
}