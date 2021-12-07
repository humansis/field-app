package cz.applifting.humansis.model

import com.google.gson.annotations.SerializedName

/**
 * Created by Petr Kubes <petr.kubes@applifting.cz> on 28, August, 2019
 */
enum class Target {
    @SerializedName("0") FAMILY,
    @SerializedName("1") INDIVIDUAL
}