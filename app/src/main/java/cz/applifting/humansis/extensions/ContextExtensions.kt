package cz.applifting.humansis.extensions

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.NavDirections
import androidx.navigation.fragment.findNavController
import cz.applifting.humansis.R

fun Context.isNetworkConnected(): Boolean {
    val connectivityManager = this.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        val cellular = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ?: false
        val wifi = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ?: false
        (cellular || wifi)
    } else {
        val networkInfo = connectivityManager.activeNetworkInfo
        networkInfo != null && networkInfo.isConnected
    }
}

fun Context.isWifiConnected(): Boolean {
    val connectivityManager = this.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ?: false
    } else {
        val networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
        networkInfo != null && networkInfo.isConnected
    }
}

fun Fragment.tryNavController(): NavController? =
    try {
        findNavController()
    } catch (e: IllegalStateException) {
        // when fragment got close etc.
        e.printStackTrace()
        null
    }

fun Fragment.tryNavigateFrom(@IdRes destinationId: Int, block: NavController.() -> Unit) {
    tryNavController()?.apply {
        if (currentDestination?.id == destinationId) {
            block()
        }
    }
}

fun Fragment.tryNavigate(@IdRes destinationId: Int, directions: NavDirections) {
    tryNavigateFrom(destinationId) {
        navigate(directions)
    }
}

fun Context.getCommodityString(commodityValue: Double, commodityUnit: String): String {
    return if ((commodityValue % 1) == 0.0) {
        getString(R.string.commodity_value, commodityValue.toInt(), commodityUnit)
    } else {
        // This needs to be updated if Denars or Madagascar Ariaries are used in the future
        getString(R.string.commodity_value_decimal, commodityValue, commodityUnit)
    }
}
