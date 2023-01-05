package cz.applifting.humansis.model.api

/**
 * Created by Petr Kubes <petr.kubes@applifting.cz> on 17, August, 2019
 */
data class LoginResponse(
    var id: Long = 0,
    var username: String = "",
    var token: String = "",
    var refreshToken: String = "",
    var refreshTokenExpiration: String = "",
    var email: String = "",
    var changePassword: Boolean = false,
    var availableCountries: List<String>? = null
)