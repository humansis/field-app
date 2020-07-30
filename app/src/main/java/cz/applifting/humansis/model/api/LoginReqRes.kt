package cz.applifting.humansis.model.api

import com.google.gson.annotations.SerializedName
import cz.applifting.humansis.model.Role
import cz.applifting.humansis.model.Country

/**
 * Created by Petr Kubes <petr.kubes@applifting.cz> on 17, August, 2019
 */
data class LoginReqRes(
    var id: Long = 0,
    var username: String = "",
    var password: String = "",
    var countries: List<Country> = listOf(),

    @SerializedName("change_password")
    var email: String = "",
    var changePassword: Boolean = false,
    var vendor: String? = null,
    var roles: List<Role>? = null,
    var language: String? = null,
    var projects: List<Project>? = null,

    var transactions: List<Any>? = null,
    @SerializedName("phone_prefix")
    var phonePrefix: String? = null,
    @SerializedName("phone_number")
    var phoneNumber: String? = null,
    @SerializedName("two_factor_authentication")
    var twoFactorAuthentication: Boolean = false
)