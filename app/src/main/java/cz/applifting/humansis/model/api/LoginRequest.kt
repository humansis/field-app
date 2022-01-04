package cz.applifting.humansis.model.api

/**
 * Created by Petr Kubes <petr.kubes@applifting.cz> on 17, August, 2019
 */
data class LoginRequest(
    var username: String = "",
    var password: String = ""
)