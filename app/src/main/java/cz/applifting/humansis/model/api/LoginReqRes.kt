package cz.applifting.humansis.model.api

import com.google.gson.annotations.SerializedName

/**
 * Created by Petr Kubes <petr.kubes@applifting.cz> on 17, August, 2019
 */
data class LoginReqRes(
    var id: Long = 0,
    var username: String = "",
    var password: String = "",
//    var countries: List<Country> = listOf(), //it is there, but has strange data inside and thus its useless
//    var projects: List<Project>? = null, //it is there, but has strange data inside and thus its useless
    var email: String = "",
//    var roles: List<Role>? = null, // it is there, but not used by the app
//    var transactions: List<Any>? = null, // it is there, but not used by the app
//    var vendor: String? = null, // it is there, but not used by the app
//    var language: String? = null, // it is there, but not used by the app
//    @SerializedName("phone_prefix") var phonePrefix: String? = null, // it is there, but not used by the app
//    @SerializedName("phone_number") var phoneNumber: String? = null, // it is there, but not used by the app
    @SerializedName("change_password") var changePassword: Boolean = false,
//    @SerializedName("two_factor_authentication") var twoFactorAuthentication: Boolean = false // it is there, but not used by the app
    @SerializedName("available_countries") var availableCountries: List<String>? = null
)