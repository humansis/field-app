package cz.applifting.humansis.model.api

import com.google.gson.annotations.SerializedName
import cz.applifting.humansis.R

data class NationalCardId(
    val type: NationalCardIdType,
    val number: String
) {
    override fun toString(): String = "${getSerializedName(type)}: $number"

    private fun getSerializedName(type: NationalCardIdType): String {
        return type.declaringClass.getField(type.name).getAnnotation(SerializedName::class.java)?.value ?: type.name
    }
}

enum class NationalCardIdType(val stringResource: Int) {
    @SerializedName("National ID") NATIONAL_ID(R.string.national_id),
    @SerializedName("Tax Number") TAX_NUMBER(R.string.tax_number),
    @SerializedName("Passport") PASSPORT(R.string.passport),
    @SerializedName("Family Registration") FAMILY(R.string.family_registration),
    @SerializedName("Birth Certificate") BIRTH_CERTIFICATE(R.string.birth_certificate),
    @SerializedName("Driver's License") DRIVERS_LICENSE(R.string.drivers_license),
    @SerializedName("Camp ID") CAMP_ID(R.string.camp_id),
    @SerializedName("Social Service Card") SOCIAL_SERVICE_ID(R.string.social_service_card),
    @SerializedName("Other") OTHER(R.string.other),
    @SerializedName("None") NONE(0)
}
