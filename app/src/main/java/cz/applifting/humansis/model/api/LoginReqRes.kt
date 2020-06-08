package cz.applifting.humansis.model.api

import com.google.gson.annotations.SerializedName
import cz.applifting.humansis.model.Role
import cz.applifting.humansis.model.Country

/**
 * Created by Petr Kubes <petr.kubes@applifting.cz> on 17, August, 2019
 */
data class LoginReqRes(
//    @SerializedName("change_password") val changePassword: Boolean,
//    val email: String,
//    val id: String?,
//    val language: String?,
//    val password: String,
//    val roles: List<Role>?,
//    val username: String,
//    val vendor: String?,
//    val projects: List<Project>?,

    var id: Long = 0,
    var username: String = "",
    var email: String = "",
    var password: String = "",
    var projects: List<Project>? = null,
    @SerializedName("change_password")
    var changePassword: Boolean = false,

    var countries: List<Country> = listOf(),

    // TODO used to be there, not sure if needed
    var vendor: String? = null,
    var roles: List<Role>? = null,
    var language: String? = null,

    // TODO new, not sure if needed
    var transactions: List<Any>? = null,
    @SerializedName("phone_prefix")
    var phonePrefix: String? = null,
    @SerializedName("phone_number")
    var phoneNumber: String? = null,
    @SerializedName("two_factor_authentication")
    var twoFactorAuthentication: Boolean = false
)