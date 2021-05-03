package cz.applifting.humansis.misc

import android.content.Context
import cz.applifting.humansis.R
import cz.quanti.android.nfc.exception.PINExceptionEnum

object NfcCardErrorMessage {
    fun getNfcCardErrorMessage(pinExceptionEnum: PINExceptionEnum, context: Context): String {
        return when (pinExceptionEnum) {
            PINExceptionEnum.CARD_LOCKED -> context.getString(R.string.card_locked)
            PINExceptionEnum.INCORRECT_PIN -> context.getString(R.string.incorrect_pin)
            PINExceptionEnum.INVALID_DATA -> context.getString(R.string.invalid_data)
            PINExceptionEnum.UNSUPPORTED_VERSION -> context.getString(R.string.invalid_version)
            PINExceptionEnum.DIFFERENT_CURRENCY -> context.getString(R.string.currency_mismatch)
            PINExceptionEnum.TAG_LOST -> context.getString(R.string.tag_lost_card_error)
            PINExceptionEnum.DIFFERENT_USER -> context.getString(R.string.different_user_card_error)
            PINExceptionEnum.CARD_INITIALIZED -> context.getString(R.string.card_initialized)
            else -> context.getString(R.string.card_error)
        }
    }
}