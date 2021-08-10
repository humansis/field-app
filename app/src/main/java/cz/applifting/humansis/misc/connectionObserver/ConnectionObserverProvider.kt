package cz.applifting.humansis.misc.connectionObserver

import io.reactivex.Observable

interface ConnectionObserverProvider {
    fun getNetworkAvailability(): Observable<Boolean>
}