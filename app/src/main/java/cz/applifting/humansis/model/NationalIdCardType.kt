package cz.applifting.humansis.model

import com.google.gson.annotations.SerializedName

enum class NationalIdCardType {
    @SerializedName("National ID") NATIONAL_ID,
    @SerializedName("Passport") PASSPORT,
    @SerializedName("Family Registration") FAMILY,
    @SerializedName("Birth Certificate") BIRTH_CERTIFICATE,
    @SerializedName("Driver's License") DRIVERS_LICENSE,
    @SerializedName("Camp ID") CAMP_ID,
    @SerializedName("Social Service Card") SOCIAL_SERVICE_ID,
    @SerializedName("Other") OTHER,
}