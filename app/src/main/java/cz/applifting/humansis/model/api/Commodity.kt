package cz.applifting.humansis.model.api

import com.google.gson.annotations.SerializedName

/**
 * Created by Petr Kubes <petr.kubes@applifting.cz> on 28, August, 2019
 */
data class Commodity(
    val id: Int,
    val unit: String,
    val value: Double,
    val description: String?,
    @SerializedName("modality_type") val modalityType: ModalityType?
)