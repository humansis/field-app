package cz.applifting.humansis.misc

import java.text.SimpleDateFormat
import java.util.*

object DateUtil {
    fun stringToDate(value: String?): Date? {
        val format = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        return value?.let { format.parse(it) }
    }
}