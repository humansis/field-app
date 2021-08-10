package cz.applifting.humansis.model.api

/**
 * Created by Petr Kubes <petr.kubes@applifting.cz> on 19, August, 2019
 */
data class Project(
    val id: Int,
    val name: String?,
    val startDate: String?,
    val endDate: String?,
    val sectors: List<Any>?,
    val donorIds: List<Any>?,
    val numberOfHouseholds: Int?,
    val beneficiariesReached: Int?
)