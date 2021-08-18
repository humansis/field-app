package cz.applifting.humansis.misc.connectionObserver

import io.reactivex.Observable

interface ConnectionObserver {
    fun getNetworkAvailability(): Observable<Boolean>
}