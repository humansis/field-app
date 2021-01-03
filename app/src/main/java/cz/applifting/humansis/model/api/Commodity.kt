package cz.applifting.humansis.model.api

import com.google.gson.annotations.SerializedName

/**
 * Created by Petr Kubes <petr.kubes@applifting.cz> on 28, August, 2019
 */
data class Commodity(
    val id: Int,
    val unit: String,
    val value: Double, // This type is later rounded to Integer. If double is really needed, some changes in the app will be required.
    val description: String?,
    @SerializedName("modality_type") val modalityType: ModalityType?
)