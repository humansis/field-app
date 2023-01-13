package cz.applifting.humansis.model

import cz.applifting.humansis.misc.Payload
import java.util.*

class JWToken(
    private val payload: Payload
) {
    fun isExpired(): Boolean {
        val tokenExpirationInMillis = payload.exp * 1000
        val twentyMinutesInMillis = 20 * 60 * 1000 // Twenty minutes should be plenty for one request. (Timeouts add to 15 minutes)
        return (tokenExpirationInMillis - twentyMinutesInMillis) < Date().time
    }
}