package cz.applifting.humansis.misc

import android.content.Context
import android.text.format.DateFormat.getDateFormat
import java.text.SimpleDateFormat
import java.util.*

object DateUtil {
    private val format = SimpleDateFormat("dd-MM-yyyy", Locale.US)

    fun stringToDate(value: String?): Date? {
        return value?.let { format.parse(it) }
    }

    fun dateToString(value: Date, context: Context): String {
        return getDateFormat(context).format(value)
    }
}