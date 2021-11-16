package cz.applifting.humansis.extensions

import android.app.Activity
import android.widget.Toast

fun String.shortToast(activity: Activity) {
    Toast.makeText(activity, this, Toast.LENGTH_SHORT).show()
}

fun String?.equalsIgnoreEmpty(other: String?): Boolean {
    return this.orNullIfEmpty() == other.orNullIfEmpty()
}

fun String?.orNullIfEmpty(): String? {
    return this?.let { if (it.isEmpty()) null else it }
}
