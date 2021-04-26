package cz.applifting.humansis.misc

import android.app.Activity
import cz.applifting.humansis.R
import cz.quanti.android.nfc.exception.PINExceptionEnum

object NfcCardErrorMessage {
    fun getNfcCardErrorMessage(pinExceptionEnum: PINExceptionEnum, activity: Activity): String {
        return when (pinExceptionEnum) {
            PINExceptionEnum.CARD_LOCKED -> activity.getString(R.string.card_locked)
            PINExceptionEnum.INCORRECT_PIN -> activity.getString(R.string.incorrect_pin)
            PINExceptionEnum.INVALID_DATA -> activity.getString(R.string.invalid_data)
            PINExceptionEnum.UNSUPPORTED_VERSION -> activity.getString(R.string.invalid_version)
            PINExceptionEnum.DIFFERENT_CURRENCY -> activity.getString(R.string.currency_mismatch)
            PINExceptionEnum.TAG_LOST -> activity.getString(R.string.tag_lost_card_error)
            PINExceptionEnum.DIFFERENT_USER -> activity.getString(R.string.different_user_card_error)
            PINExceptionEnum.CARD_INITIALIZED -> activity.getString(R.string.card_initialized)
            else -> activity.getString(R.string.card_error)
        }
    }
}