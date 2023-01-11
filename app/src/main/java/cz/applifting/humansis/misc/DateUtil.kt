package cz.applifting.humansis.misc

import android.content.Context
import android.text.format.DateFormat.getDateFormat
import java.text.SimpleDateFormat
import java.util.*

private val format = SimpleDateFormat("dd-MM-yyyy", Locale.US)
private val formatForApiRequest = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US)

fun stringToDate(value: String?): Date? {
    return value?.let { format.parse(it) }
}

fun dateToString(value: Date, context: Context): String {
    return getDateFormat(context).format(value)
}

fun convertTimeForApiRequestBody(date: Date): String {
    return formatForApiRequest.format(date)
}
