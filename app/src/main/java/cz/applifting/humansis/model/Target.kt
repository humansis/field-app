package cz.applifting.humansis.model

import com.google.gson.annotations.SerializedName

/**
 * Created by Petr Kubes <petr.kubes@applifting.cz> on 28, August, 2019
 */
enum class Target {
    @SerializedName("household") HOUSEHOLD,
    @SerializedName("individual") INDIVIDUAL,
    @SerializedName("community") COMMUNITY, // TODO sehnat ikonky
    @SerializedName("institution") INSTITUTION // TODO sehnat ikonky
}