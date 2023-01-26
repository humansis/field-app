package cz.applifting.humansis.model

import cz.applifting.humansis.extensions.secondsToMilliseconds
import cz.applifting.humansis.extensions.minutesToSeconds
import cz.applifting.humansis.misc.Payload
import java.util.*

class JWToken(
    private val payload: Payload
) {
    fun isExpired(): Boolean {
        val tokenExpirationInMillis = payload.exp.secondsToMilliseconds()
        // Twenty minutes should be plenty for one request. (Timeouts add to 15 minutes)
        val twentyMinutesInMillis = TWENTY_MINUTES.minutesToSeconds().secondsToMilliseconds()
        return (tokenExpirationInMillis - twentyMinutesInMillis) < Date().time
    }

    companion object {
        private const val TWENTY_MINUTES = 20L
    }
}