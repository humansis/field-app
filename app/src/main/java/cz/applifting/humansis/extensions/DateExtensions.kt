package cz.applifting.humansis.extensions

import android.content.Context
import android.text.format.DateFormat
import java.text.SimpleDateFormat
import java.util.*

private val formatDate = SimpleDateFormat("dd-MM-yyyy", Locale.US)
private val formatDateTime = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.US)
private val formatApiRequest = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US)

fun String.toDate(): Date? {
    return formatDate.parse(this)
}

fun Date.toLocalisedString(context: Context): String {
    return DateFormat.getDateFormat(context).format(this)
}

fun Date.toFormattedString(): String {
    return formatDateTime.format(this)
}

fun Date.convertForApiRequestBody(): String {
    return formatApiRequest.format(this)
}