package cz.applifting.humansis.model

import cz.applifting.humansis.misc.Payload
import java.util.*

class JWToken(
    private val payload: Payload
) {
    fun isExpired(): Boolean {
        val tokenExpirationInMillis = payload.exp * 1000
        val oneHourInMillis = 60 * 60 * 1000 // TODO poresit jestli hodina staci
        return (tokenExpirationInMillis - oneHourInMillis) < Date().time
    }

}