package cz.applifting.humansis.misc

import java.text.SimpleDateFormat
import java.util.*

object DateUtil {
    val format = SimpleDateFormat("dd-MM-yyyy", Locale.US)

    fun stringToDate(value: String?): Date? {
        return value?.let { format.parse(it) }
    }
}