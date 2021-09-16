package cz.applifting.humansis.model.api

import com.google.gson.annotations.SerializedName
import cz.applifting.humansis.model.NationalIdCardType

/**
 * Created by Petr Kubes <petr.kubes@applifting.cz> on 06, November, 2019
 */
data class NationalIdCard(
    @SerializedName("number") val number: String,
    @SerializedName("type") val type: NationalIdCardType
)