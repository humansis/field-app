package cz.applifting.humansis.misc.connectionObserver


import android.content.Context
import android.content.Context.CONNECTIVITY_SERVICE
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET
import android.net.NetworkRequest
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import quanti.com.kotlinlog.Log
import java.io.IOException
import java.net.InetSocketAddress
import javax.net.SocketFactory

/**
 * Save all available networks with an internet connection to a set (@validNetworks).
 * As long as the size of the set > 0, isNetworkAvailable Observable emits true.
 * MinSdk = 21.
 *
 * Inspired by:
 * https://github.com/AlexSheva-mason/Rick-Morty-Database/blob/master/app/src/main/java/com/shevaalex/android/rickmortydatabase/utils/networking/ConnectionLiveData.kt
 */
object ConnectionObserver: ConnectionObserverProvider{

    private val TAG = ConnectionObserver::class.java.simpleName

    private lateinit var networkCallback: ConnectivityManager.NetworkCallback
    private lateinit var cm: ConnectivityManager
    private val validNetworks: MutableSet<Network> = HashSet()
    private val isNetworkAvailable: BehaviorSubject<Boolean> = BehaviorSubject.create()

    fun init (context: Context): ConnectionObserver {
        cm = context.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        registerCallback()
        return this
    }

    override fun getNetworkAvailability(): Observable<Boolean> {
        return isNetworkAvailable
    }

    private fun registerCallback() {
        if (this::networkCallback.isInitialized) {
            cm.unregisterNetworkCallback(networkCallback)
        }
        networkCallback = createNetworkCallback()
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NET_CAPABILITY_INTERNET)
            .build()
        cm.registerNetworkCallback(networkRequest, networkCallback)
        checkValidNetworks()
    }

    private fun checkValidNetworks() {
        isNetworkAvailable.onNext(validNetworks.size > 0)
    }

    private fun createNetworkCallback() = object : ConnectivityManager.NetworkCallback() {

        /*
          Called when a network is detected. If that network has internet, save it in the Set.
          Source: https://developer.android.com/reference/android/net/ConnectivityManager.NetworkCallback#onAvailable(android.net.Network)
         */
        override fun onAvailable(network: Network) {
            Log.d(TAG, "onAvailable: $network")
            val networkCapabilities = cm.getNetworkCapabilities(network)
            val hasInternetCapability = networkCapabilities?.hasCapability(NET_CAPABILITY_INTERNET)
            Log.d(TAG, "onAvailable: $network, $hasInternetCapability")
            if (hasInternetCapability == true) {
                // check if this network actually has internet
                CoroutineScope(Dispatchers.IO).launch {
                    if (doesNetworkHaveInternet(network.socketFactory)) {
                        withContext(Dispatchers.Main) {
                            Log.d(TAG, "onAvailable: adding network. $network")
                            validNetworks.add(network)
                            checkValidNetworks()
                        }
                    }
                }
            }
        }

        /*
          If the callback was registered with registerNetworkCallback() it will be called for each network which no longer satisfies the criteria of the callback.
          Source: https://developer.android.com/reference/android/net/ConnectivityManager.NetworkCallback#onLost(android.net.Network)
         */
        override fun onLost(network: Network) {
            Log.d(TAG, "onLost: $network")
            validNetworks.remove(network)
            checkValidNetworks()
        }

    }

    private fun doesNetworkHaveInternet(socketFactory: SocketFactory): Boolean {
        return try {
            Log.d(TAG, "PINGING google.")
            val socket = socketFactory.createSocket() ?: throw IOException("Socket is null.")
            socket.connect(InetSocketAddress("8.8.8.8", 53), 1500)
            socket.close()
            Log.d(TAG, "PING success.")
            true
        } catch (e: IOException) {
            Log.e(TAG, "No internet connection. $e")
            false
        }
    }
}